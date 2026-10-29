#!/usr/bin/env python3
"""Prepare reproducible CurseForge uploads; network writes require --upload.

Python 3.11+, standard library only. Credentials come exclusively from the environment.
"""
from __future__ import annotations

import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import tomllib
import urllib.error
import urllib.request
import urllib.parse
import uuid
import zipfile

API = "https://minecraft.curseforge.com"
PLATFORMS = {
    "neoforge": {"directory": "", "minecraft": "1.21.1", "loader": "NeoForge", "java": 21,
                 "tasks": [":jar", ":sourcesJar"]},
    "forge": {"directory": "forge", "minecraft": "1.20.1", "loader": "Forge", "java": 17,
              "tasks": [":jarJar", ":reobfJarJar", ":sourcesJar"]},
}


class PublishError(Exception):
    pass


def load_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8-sig"))


def save_json(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + ".tmp-" + uuid.uuid4().hex)
    try:
        temp.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        temp.replace(path)
    finally:
        temp.unlink(missing_ok=True)


def digest(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def version_of(repo):
    version = (repo / "VERSION").read_text(encoding="utf-8-sig").strip()
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9.+_-]*", version):
        raise PublishError(f"Invalid VERSION in {repo}")
    return version


def positive_id(value):
    if isinstance(value, bool) or not str(value).isdigit() or int(value) <= 0:
        raise PublishError("Configure a positive CurseForge project ID")
    return int(value)


def relation_for(modid, required, mapping):
    relation = mapping.get(modid)
    if relation is None:
        if required:
            raise PublishError(f"Missing CurseForge dependency mapping: {modid}")
        return None
    if isinstance(relation, str):
        relation = {"slug": relation}
    slug = relation.get("slug", "")
    if not re.fullmatch(r"[a-z0-9_][a-z0-9_-]*", slug):
        raise PublishError(f"Invalid dependency slug for {modid}")
    result = {"slug": slug, "type": "requiredDependency" if required else "optionalDependency"}
    if relation.get("project_id") is not None:
        result["projectID"] = positive_id(relation["project_id"])
    return result


def check_mixin_refmaps(path):
    path = Path(path)
    try:
        with zipfile.ZipFile(path) as jar:
            names = jar.namelist()
            for name in names:
                if not name.endswith(".json") or "mixins" not in Path(name).name.lower():
                    continue
                config = json.loads(jar.read(name))
                if not isinstance(config, dict):
                    raise PublishError(f"Invalid Mixin config {name}: {path.name}")
                refmap_name = config.get("refmap")
                if not refmap_name:
                    continue
                if not isinstance(refmap_name, str):
                    raise PublishError(f"Invalid Mixin refmap name in {name}: {path.name}")
                if refmap_name not in names:
                    raise PublishError(f"Missing Mixin refmap {refmap_name}: {path.name}")
                refmap = json.loads(jar.read(refmap_name))
                if not isinstance(refmap, dict) or not isinstance(refmap.get("mappings"), dict):
                    raise PublishError(f"Invalid Mixin refmap {refmap_name}: {path.name}")
    except (OSError, UnicodeError, zipfile.BadZipFile, json.JSONDecodeError) as error:
        raise PublishError(f"Invalid Mixin resources in release JAR: {path.name}") from error


def inspect_jar(path, modid, version, platform, mapping):
    target = PLATFORMS[platform]
    entry = "META-INF/neoforge.mods.toml" if platform == "neoforge" else "META-INF/mods.toml"
    try:
        with zipfile.ZipFile(path) as jar:
            metadata = tomllib.loads(jar.read(entry).decode("utf-8-sig"))
            names = jar.namelist()
    except (OSError, KeyError, zipfile.BadZipFile, tomllib.TOMLDecodeError) as error:
        raise PublishError(f"Invalid {platform} release JAR: {path.name}") from error
    check_mixin_refmaps(path)
    if "LICENSE" not in names:
        raise PublishError(f"Missing LICENSE: {path.name}")
    if not any(name.endswith(".class") for name in names):
        raise PublishError(f"Not a binary release: {path.name}")
    mods = metadata.get("mods", [])
    if len(mods) != 1 or mods[0].get("modId") != modid or mods[0].get("version") != version:
        raise PublishError(f"JAR modid/version differs from release selection: {path.name}")
    deps = metadata.get("dependencies", {}).get(modid, [])
    ids = {dep["modId"] for dep in deps}
    expected_loader = platform
    other_loader = "forge" if platform == "neoforge" else "neoforge"
    if expected_loader not in ids or other_loader in ids:
        raise PublishError(f"Loader declaration differs from {platform}: {path.name}")
    mc = next((dep for dep in deps if dep["modId"] == "minecraft"), {})
    if mc.get("versionRange") != f"[{target['minecraft']}]":
        raise PublishError(f"Minecraft version differs from {target['minecraft']}: {path.name}")
    relations = []
    for dep in deps:
        if dep["modId"] in {"minecraft", "forge", "neoforge", "java"}:
            continue
        required = dep.get("type") == "required" if platform == "neoforge" else dep.get("mandatory", False)
        relation = relation_for(dep["modId"], required, mapping)
        if relation is not None:
            relations.append(relation)
    return relations


def selected_projects(config, names):
    projects = config["projects"]
    names = names or config.get("default_projects", [])
    if not names or len(names) != len(set(names)):
        raise PublishError("Select at least one project, without duplicates")
    for name in names:
        if name not in projects:
            raise PublishError(f"Unknown project: {name}")
        positive_id(projects[name].get("project_id"))
    # Dependencies first, retaining relative order of other projects.
    return sorted(names, key=lambda name: projects[name]["modid"] != "ars_conflux")


def build_projects(config, root, names, platforms, java_homes, offline):
    jobs = []
    for name in names:
        project = config["projects"][name]
        repo = (root / project["directory"]).resolve()
        for platform in platforms:
            if platform not in project["platforms"]:
                continue
            target = PLATFORMS[platform]
            home = java_homes.get(str(target["java"]))
            if not home or not (Path(home) / "bin/java.exe").is_file():
                raise PublishError(f"Set Java {target['java']} home in local config or launcher parameters")
            directory = repo / target["directory"]
            wrapper = directory / "gradlew.bat"
            if not wrapper.is_file():
                raise PublishError(f"Missing build wrapper: {wrapper}")
            tasks = list(project.get("build_tasks", {}).get(platform, target["tasks"])) + project.get("extra_tasks", {}).get(platform, [])
            args = [str(wrapper), *tasks, "--console=plain"]
            if offline:
                args.append("--offline")
            # Keep native cmd forwarding free of input strings interpreted as shell syntax.
            if any(any(char in arg for char in '&|<>^%\r\n"') for arg in args):
                raise PublishError("Build path/arguments contain unsupported cmd characters")
            jobs.append((name, platform, directory, args, home))
    for name, platform, directory, args, home in jobs:
        env = os.environ.copy()
        env["JAVA_HOME"] = str(home)
        env["PATH"] = str(Path(home) / "bin") + os.pathsep + env["PATH"]
        env.pop(config.get("token_environment", "CURSEFORGE_API_TOKEN"), None)
        print(f"Building {name} / {platform}", flush=True)
        result = subprocess.run(args, cwd=directory, env=env, check=False)
        if result.returncode:
            raise PublishError(f"Build failed: {name} / {platform} (exit {result.returncode})")


def prepare(config, root, names, platforms, output, changelog_dir, release_type=None, manual=None):
    entries = []
    # Preflight the whole batch before staging or making any remote writes.
    for name in names:
        project = config["projects"][name]
        repo = (root / project["directory"]).resolve()
        version = version_of(repo)
        notes = changelog_dir / name / f"{version}.md"
        if not notes.is_file() or not notes.read_text(encoding="utf-8-sig").strip():
            raise PublishError(f"Write release notes first: {notes}")
        kind = release_type or project.get("release_type", config.get("release_type", "release"))
        if kind not in {"alpha", "beta", "release"}:
            raise PublishError(f"Invalid release type for {name}")
        environments = project.get("environments", config.get("environments", ["Client", "Server"]))
        if (not isinstance(environments, list) or not environments
                or any(value not in {"Client", "Server"} for value in environments)
                or len(environments) != len(set(environments))):
            raise PublishError(f"Invalid environments for {name}: select Client and/or Server")
        for platform in platforms:
            if platform not in project["platforms"]:
                continue
            target = PLATFORMS[platform]
            filename = f"{project['artifact']}-{version}-mc{target['minecraft']}-{platform}.jar"
            source = repo / target["directory"] / "build/libs" / filename
            relations = inspect_jar(source, project["modid"], version, platform, config["dependencies"])
            metadata = {
                "displayName": f"{project['name']} {version} — MC {target['minecraft']} / {target['loader']}",
                "changelog": notes.read_text(encoding="utf-8-sig").strip(), "changelogType": "markdown",
                "gameVersionNames": [target["minecraft"], target["loader"], *environments],
                "releaseType": kind,
                "isMarkedForManualRelease": config.get("manual_release", False) if manual is None else manual,
                "relations": {"projects": relations},
            }
            entries.append({"project": name, "project_id": positive_id(project["project_id"]),
                            "platform": platform, "version": version, "source": str(source),
                            "filename": filename, "sha256": digest(source), "metadata": metadata})
    if not entries:
        raise PublishError("Selected projects do not support the selected platform")
    output.mkdir(parents=True, exist_ok=False)
    for entry in entries:
        staged = output / entry["filename"]
        shutil.copyfile(entry.pop("source"), staged)
        if digest(staged) != entry["sha256"]:
            raise PublishError("Build artifact changed while preparing the upload")
        entry["file"] = str(staged.resolve())
    plan = {"schema": 1, "api": API, "entries": entries}
    save_json(output / "plan.json", plan)
    return output / "plan.json"


def show_plan(path):
    plan = load_json(path)
    for entry in plan["entries"]:
        print(f"\n{entry['filename']} -> CurseForge project {entry['project_id']}")
        print(f"  {entry['metadata']['releaseType']} | {', '.join(entry['metadata']['gameVersionNames'])}")
        print(f"  SHA-256: {entry['sha256']}")
        print("  Dependencies: " + ", ".join(r["slug"] + ":" + r["type"] for r in entry["metadata"]["relations"]["projects"]))
        print(entry["metadata"]["changelog"])
    print(f"\nSaved upload plan: {path}")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def response_detail(error, token):
    """Keep actionable API messages, never echoed credentials or request headers."""
    try:
        body = error.read(65536).decode("utf-8", errors="replace")
    except Exception:
        return "No server error details available"
    for secret in {token, urllib.parse.quote(token, safe=""), json.dumps(token)[1:-1]}:
        if secret:
            body = body.replace(secret, "[REDACTED]")
    try:
        data = json.loads(body)
        if isinstance(data, dict):
            data = {key: value for key, value in data.items()
                    if key.lower() in {"error", "errors", "message", "detail", "title", "errorcode", "errormessage"}}
            body = json.dumps(data, ensure_ascii=False) if data else "No server error details available"
        elif isinstance(data, str):
            body = data
        else:
            body = "No server error details available"
    except ValueError:
        body = re.sub(r"<[^>]*>", " ", body)
    for secret in {token, urllib.parse.quote(token, safe=""), json.dumps(token)[1:-1]}:
        if secret:
            body = body.replace(secret, "[REDACTED]")
    body = " ".join(re.sub(r"[\x00-\x1F\x7F]", " ", body).split())
    return body[:1200] or "No server error details available"


def fetch_api(endpoint, token):
    request = urllib.request.Request(API + endpoint, headers={"X-Api-Token": token,
                                    "User-Agent": "EndXiom/Conflux-CurseForge-Publisher"})
    try:
        with urllib.request.build_opener(NoRedirect).open(request, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        raise PublishError(f"Read-only API check returned HTTP {error.code}: {response_detail(error, token)}") from None
    except Exception:
        raise PublishError("Could not read CurseForge version catalog; no upload attempted") from None


def version_ids(names, versions, types):
    ids = []
    for name in names:
        if re.fullmatch(r"\d+\.\d+(?:\.\d+)?", name):
            # Bukkit and general Minecraft catalog entries reuse the same version name.
            expected_type = "Minecraft " + ".".join(name.split(".")[:2])
        elif name in {"Forge", "NeoForge"}:
            expected_type = "Modloader"
        elif name in {"Client", "Server"}:
            expected_type = "Environment"
        else:
            raise PublishError(f"Unsupported game version tag: {name}")
        type_ids = {row["id"] for row in types if row.get("name") == expected_type}
        matches = [row for row in versions if row.get("name") == name and row.get("gameVersionTypeID") in type_ids]
        if len(matches) != 1:
            raise PublishError(f"Cannot uniquely resolve {expected_type}: {name}; no upload attempted")
        ids.append(positive_id(matches[0]["id"]))
    return ids


def resolved_entries(plan, token, fetcher=None):
    fetcher = fetcher or fetch_api
    versions = fetcher("/api/game/versions", token)
    types = fetcher("/api/game/version-types", token)
    entries = []
    for original in plan["entries"]:
        entry = dict(original)
        metadata = dict(original["metadata"])
        if "gameVersionNames" not in metadata:
            raise PublishError("The review plan must retain human-readable version names")
        names = list(metadata.pop("gameVersionNames"))
        # Plans created before environment tags were required describe our dual-side mods.
        # Retain their immutable artifact snapshot while supplying the missing category.
        if not any(name in {"Client", "Server"} for name in names):
            names.extend(["Client", "Server"])
        metadata["gameVersions"] = version_ids(names, versions, types)
        if "relations" in metadata:
            projects = []
            for original_relation in metadata["relations"].get("projects", []):
                relation = dict(original_relation)
                if "projectID" in relation:
                    relation["projectID"] = positive_id(relation["projectID"])
                projects.append(relation)
            metadata["relations"] = {**metadata["relations"], "projects": projects}
        entry["metadata"] = metadata
        entries.append(entry)
    return entries


def upload_file(entry, token):
    boundary = "conflux-" + uuid.uuid4().hex
    filename = entry["filename"]
    if not re.fullmatch(r"[A-Za-z0-9._+-]+\.jar", filename):
        raise PublishError("Unsafe multipart filename")
    if "gameVersionNames" in entry["metadata"] or not entry["metadata"].get("gameVersions"):
        raise PublishError("Resolve exact game version IDs before uploading")
    metadata = json.dumps(entry["metadata"], ensure_ascii=False).encode("utf-8")
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"metadata\"\r\n\r\n").encode() + metadata
    body += (f"\r\n--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\n"
             "Content-Type: application/java-archive\r\n\r\n").encode()
    body += Path(entry["file"]).read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    request = urllib.request.Request(f"{API}/api/projects/{positive_id(entry['project_id'])}/upload-file", body,
                headers={"X-Api-Token": token, "Content-Type": f"multipart/form-data; boundary={boundary}",
                         "User-Agent": "EndXiom/Conflux-CurseForge-Publisher"}, method="POST")
    # Never follow an authenticated redirect or retry a POST after an ambiguous result.
    with urllib.request.build_opener(NoRedirect).open(request, timeout=180) as response:
        result = json.loads(response.read().decode("utf-8"))
    return positive_id(result.get("id"))


@contextlib.contextmanager
def exclusive_lock(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    try:
        fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    except FileExistsError as error:
        raise PublishError(f"Publisher already locked: {path}. If its process ended, remove this lock file.") from error
    try:
        with os.fdopen(fd, "w") as stream:
            stream.write(str(os.getpid()))
        yield
    finally:
        path.unlink(missing_ok=True)


def upload_plan(path, ledger_path, token, sender=None):
    if not token or any(c in token for c in "\r\n"):
        raise PublishError("Set CURSEFORGE_API_TOKEN locally; do not put the token in config or command arguments")
    plan = load_json(path)
    if plan.get("schema") != 1 or plan.get("api") != API or not plan.get("entries"):
        raise PublishError("Invalid upload plan")
    for entry in plan["entries"]:
        positive_id(entry["project_id"])
        if digest(entry["file"]) != entry["sha256"]:
            raise PublishError(f"Staged file changed: {entry['filename']}")
        check_mixin_refmaps(entry["file"])
    entries = resolved_entries(plan, token) if sender is None else plan["entries"]
    sender = sender or upload_file
    ledger_path = Path(ledger_path)
    with exclusive_lock(ledger_path.with_suffix(".lock")):
        ledger = load_json(ledger_path) if ledger_path.exists() else {}
        keys = [f"{e['project_id']}:{e['sha256']}" for e in entries]
        if len(keys) != len(set(keys)):
            raise PublishError("Duplicate artifact in upload plan")
        for key in keys:
            if ledger.get(key, {}).get("state") in {"pending", "unknown"}:
                raise PublishError(f"Uncertain previous upload: {key}. Check CurseForge and resolve it before retrying.")
        for key, entry in zip(keys, entries):
            previous = ledger.get(key, {})
            if previous.get("state") == "uploaded":
                print(f"Already uploaded: {entry['filename']} (file ID {previous['file_id']})")
                continue
            record = {"state": "pending", "filename": entry["filename"], "time": time.time()}
            ledger[key] = record
            save_json(ledger_path, ledger)
            try:
                file_id = sender(entry, token)
                record.update(state="uploaded", file_id=positive_id(file_id))
            except urllib.error.HTTPError as error:
                record["state"] = "failed" if 400 <= error.code < 500 else "unknown"
                record["http_status"] = error.code
                record["error"] = response_detail(error, token)
                save_json(ledger_path, ledger)
                raise PublishError(f"Upload returned HTTP {error.code}: {record['error']}; no automatic retry. See {ledger_path}") from None
            except Exception:
                record["state"] = "unknown"
                save_json(ledger_path, ledger)
                # Do not print exception contents: third-party responses can echo credentials.
                raise PublishError(f"Upload result unknown; check CurseForge before retrying. See {ledger_path}") from None
            save_json(ledger_path, ledger)
            print(f"Uploaded: {entry['filename']} (file ID {file_id})")


def resolve_upload(ledger_path, key, file_id):
    if file_id < 0:
        raise PublishError("Use a positive file ID, or 0 after confirming there was no upload")
    with exclusive_lock(ledger_path.with_suffix(".lock")):
        ledger = load_json(ledger_path)
        if ledger.get(key, {}).get("state") not in {"pending", "unknown"}:
            raise PublishError("That entry is not an uncertain upload")
        # Zero explicitly means the author checked CurseForge and found no uploaded file.
        ledger[key].update(state="failed" if file_id == 0 else "uploaded", resolved=True)
        if file_id:
            ledger[key]["file_id"] = positive_id(file_id)
        save_json(ledger_path, ledger)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    core = Path(__file__).resolve().parents[1]
    parser.add_argument("--config", type=Path, default=core / "curseforge.local.json")
    parser.add_argument("--workspace-root", type=Path)
    parser.add_argument("--projects", help="Comma-separated project keys")
    parser.add_argument("--platform", choices=["both", *PLATFORMS], default="both")
    parser.add_argument("--build", action="store_true")
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--java21-home")
    parser.add_argument("--java17-home")
    parser.add_argument("--release-type", choices=["alpha", "beta", "release"])
    parser.add_argument("--manual-release", action="store_true", default=None)
    parser.add_argument("--plan", type=Path, help="Use an already prepared immutable upload plan")
    parser.add_argument("--upload", action="store_true")
    parser.add_argument("--check-api", action="store_true", help="Resolve exact version IDs using authenticated GETs; never upload")
    parser.add_argument("--interactive", action="store_true")
    parser.add_argument("--resolve-unknown", nargs=2, metavar=("KEY", "FILE_ID"))
    args = parser.parse_args(argv)
    if not args.config.exists():
        shutil.copyfile(core / "curseforge.example.json", args.config)
        print(f"Created local configuration: {args.config}")
    config = load_json(args.config)
    if "token" in config or "api_token" in config:
        raise PublishError("Tokens belong in the environment, not configuration files")
    root = (args.workspace_root or args.config.parent / config.get("workspace_root", "..")).resolve()
    state_dir = (args.config.parent / config.get("state_directory", "build/curseforge")).resolve()
    ledger = state_dir / "uploads.json"
    if args.resolve_unknown:
        resolve_upload(ledger, args.resolve_unknown[0], int(args.resolve_unknown[1]))
        return 0
    plan = args.plan
    if plan and (args.build or args.projects or args.release_type or args.manual_release is not None):
        raise PublishError("An existing plan fixes files and metadata; prepare a new plan to change them")
    if plan is None:
        names = args.projects.split(",") if args.projects else None
        if args.interactive:
            print("Projects: " + ", ".join(config["projects"]))
            value = input("Select projects [" + ",".join(config["default_projects"]) + "]: ").strip()
            names = value.split(",") if value else names
            args.build = input("Build before preparing? [Y/n]: ").strip().lower() != "n"
        names = selected_projects(config, [n.strip() for n in names] if names else None)
        platforms = list(PLATFORMS) if args.platform == "both" else [args.platform]
        notes = (args.config.parent / config.get("changelog_directory", "release-notes")).resolve()
        for name in names:
            repo = root / config["projects"][name]["directory"]
            notes_file = notes / name / f"{version_of(repo)}.md"
            if not notes_file.is_file() or not notes_file.read_text(encoding="utf-8-sig").strip():
                raise PublishError(f"Write release notes first: {notes_file}")
        java_homes = dict(config.get("java_homes", {}))
        for number, value in [("21", args.java21_home), ("17", args.java17_home)]:
            if value:
                java_homes[number] = value
        with exclusive_lock(state_dir / "prepare.lock"):
            if args.build:
                build_projects(config, root, names, platforms, java_homes, args.offline)
            output = state_dir / (time.strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:8])
            plan = prepare(config, root, names, platforms, output, notes, args.release_type, args.manual_release)
    show_plan(plan)
    if args.check_api:
        if args.upload or args.interactive:
            raise PublishError("--check-api cannot be combined with upload/interactive mode")
        token = os.environ.get(config.get("token_environment", "CURSEFORGE_API_TOKEN"), "")
        if not token:
            raise PublishError("Set CURSEFORGE_API_TOKEN locally to check the API")
        entries = resolved_entries(load_json(plan), token)
        for entry in entries:
            print(f"API tags verified: {entry['filename']} -> {entry['metadata']['gameVersions']}")
        save_json(plan.parent / "resolved-metadata.json", {"entries": entries})
        return 0
    if args.interactive:
        args.upload = input("Upload these files to CurseForge? [y/N]: ").strip().lower() == "y"
    if args.upload:
        upload_plan(plan, ledger, os.environ.get(config.get("token_environment", "CURSEFORGE_API_TOKEN"), ""))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (PublishError, OSError, ValueError, KeyError) as error:
        print(f"Error: {error}", file=sys.stderr)
        raise SystemExit(1)
