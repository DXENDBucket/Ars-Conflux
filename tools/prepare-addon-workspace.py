#!/usr/bin/env python3
"""Create isolated addon/Core worktrees. Plan only unless --apply is supplied."""
import argparse
import json
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path


def git(root, *args, check=True):
    result = subprocess.run(
        ["git", "-C", str(root), *args], text=True, encoding="utf-8",
        errors="replace", capture_output=True,
    )
    if check and result.returncode:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
    return result


def repository(path):
    root = Path(path).resolve(strict=True)
    actual = Path(git(root, "rev-parse", "--show-toplevel").stdout.strip()).resolve()
    if root != actual:
        raise ValueError(f"Use the repository root, not a subdirectory: {actual}")
    return root


def prepare(args):
    addon, core = repository(args.addon), repository(args.core)
    if addon == core or addon.name.casefold() == "ars-conflux":
        raise ValueError("This helper prepares addon work, not Core implementation work")
    workspace = Path(args.workspace).resolve()
    if workspace.exists():
        raise ValueError(f"Workspace already exists; no files will be replaced: {workspace}")
    if workspace == addon or addon in workspace.parents or workspace == core or core in workspace.parents:
        raise ValueError("Place the workspace outside both source repositories")
    git(addon, "check-ref-format", "--branch", args.branch)
    if git(addon, "show-ref", "--verify", f"refs/heads/{args.branch}", check=False).returncode == 0:
        raise ValueError(f"Branch already exists: {args.branch}")
    addon_sha = git(addon, "rev-parse", "--verify", f"{args.addon_ref}^{{commit}}").stdout.strip()
    core_sha = git(core, "rev-parse", "--verify", f"{args.core_ref}^{{commit}}").stdout.strip()
    addon_target, core_target = workspace / addon.name, workspace / "Ars-Conflux"
    if addon_target == core_target:
        raise ValueError("Addon directory conflicts with the Core directory")
    inputs = []
    for source, target in [(addon, addon_target), (core, core_target)]:
        for relative in ["libs", "forge/libs", "forge/build/hex-dependencies", "build/hex-dependencies"]:
            dep = source / relative
            if dep.is_dir():
                inputs.append((dep, target / relative))
    plan = {
        "created_utc": datetime.now(timezone.utc).isoformat(),
        "state": "plan",
        "workspace": str(workspace),
        "addon": {"source": str(addon), "worktree": str(addon_target), "branch": args.branch, "start_commit": addon_sha},
        "core": {"source": str(core), "worktree": str(core_target), "fixed_commit": core_sha, "tracked_files_policy": "read-only"},
        "manual_test_mods": str(workspace / "test-instance" / "mods"),
        "manual_test_note": "Destination only; not a ready Minecraft launcher instance. GameTest uses each worktree's own run directory.",
        "dependency_copies": [{"from": str(src), "to": str(dst)} for src, dst in inputs],
        "shared_instance_deployment": "coordinator-only",
    }
    if not args.apply:
        print(json.dumps(plan, ensure_ascii=False, indent=2))
        return plan
    for source in [addon, core]:
        dirty = git(source, "status", "--porcelain", "--untracked-files=no").stdout.strip()
        if dirty:
            raise ValueError(f"Tracked changes in source repository; prepare a committed baseline first: {source}\n{dirty}")
    workspace.mkdir(parents=True, exist_ok=False)
    manifest = workspace / "workspace.json"

    def record():
        manifest.write_text(json.dumps(plan, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    plan["state"] = "preparing"
    record()
    try:
        git(core, "worktree", "add", "--detach", str(core_target), core_sha)
        git(addon, "worktree", "add", "-b", args.branch, str(addon_target), addon_sha)
        for source, target in inputs:
            shutil.copytree(source, target, dirs_exist_ok=False)
        (workspace / "test-instance" / "mods").mkdir(parents=True)
        # Keep instructions available even when the pinned Core commit predates them.
        workflow = core / "docs" / "development-workflow.md"
        if workflow.is_file():
            (workspace / "docs").mkdir()
            copied = workspace / "docs" / workflow.name
            shutil.copy2(workflow, copied)
            plan["workflow_file"] = str(copied)
        if git(core_target, "status", "--porcelain", "--untracked-files=no").stdout.strip():
            raise RuntimeError("Prepared Core unexpectedly has tracked modifications")
        plan["state"] = "prepared"
        record()
    except Exception as error:
        plan["state"] = "incomplete"
        plan["error"] = str(error)
        record()
        raise RuntimeError(f"Preparation stopped. Kept files and worktree registrations for inspection: {manifest}") from error
    print(json.dumps(plan, ensure_ascii=False, indent=2))
    return plan


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--addon", required=True, help="Addon repository root")
    parser.add_argument("--core", default=str(Path(__file__).resolve().parents[1]))
    parser.add_argument("--workspace", required=True, help="New task directory outside both repositories")
    parser.add_argument("--branch", required=True, help="New addon branch; never reuse an agent's active branch")
    parser.add_argument("--addon-ref", default="HEAD")
    parser.add_argument("--core-ref", default="HEAD")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    try:
        prepare(args)
    except (ValueError, RuntimeError, FileNotFoundError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
