"""Release validation and upload lifecycle tests; all requests are mocked."""
import argparse
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import zipfile

SCRIPT = Path(__file__).resolve().parents[1] / "curseforge_publish.py"
spec = importlib.util.spec_from_file_location("curseforge_publish", SCRIPT)
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class PublisherTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / "Example"
        self.repo.mkdir()
        (self.repo / "VERSION").write_text("1.2.3", encoding="utf-8")
        self.notes = self.root / "notes"
        (self.notes / "example").mkdir(parents=True)
        (self.notes / "example/1.2.3.md").write_text("修复图标。\n\n- Keep **Markdown**.", encoding="utf-8")
        self.config = {"default_projects": ["example"], "dependencies": {"ars_nouveau": "ars-nouveau"},
                       "projects": {"example": {"directory": "Example", "name": "Example & Nouveau",
                         "modid": "example", "artifact": "example", "project_id": 123,
                         "platforms": ["neoforge", "forge"]}}}
        self.jar("neoforge")
        self.jar("forge")

    def jar(self, platform, version="1.2.3", minecraft=None, dependency="ars_nouveau"):
        target = publisher.PLATFORMS[platform]
        path = self.repo / target["directory"] / "build/libs" / f"example-1.2.3-mc{target['minecraft']}-{platform}.jar"
        path.parent.mkdir(parents=True, exist_ok=True)
        required = 'type="required"' if platform == "neoforge" else "mandatory=true"
        mc = minecraft or target["minecraft"]
        meta = f'''license="MIT"
[[mods]]
modId="example"
version="{version}"
[[dependencies.example]]
modId="{platform}"
{required}
[[dependencies.example]]
modId="minecraft"
versionRange="[{mc}]"
{required}
[[dependencies.example]]
modId="{dependency}"
{required}
'''
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("META-INF/neoforge.mods.toml" if platform == "neoforge" else "META-INF/mods.toml", meta)
            jar.writestr("LICENSE", "Test license")
            jar.writestr("dev/example/Test.class", b"test-bytecode")
        return path

    def plan(self):
        return publisher.prepare(self.config, self.root, ["example"], ["neoforge", "forge"],
                                 self.root / "staged", self.notes)

    def quiet_upload(self, plan, sender, token="secret-that-must-not-print"):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            publisher.upload_plan(plan, self.root / "ledger.json", token, sender)
        self.assertNotIn(token, output.getvalue())

    def test_platform_metadata_markdown_and_immutable_copy(self):
        plan = publisher.load_json(self.plan())
        self.assertEqual(2, len(plan["entries"]))
        for entry, expected in zip(plan["entries"], [["1.21.1", "NeoForge", "Client", "Server"], ["1.20.1", "Forge", "Client", "Server"]]):
            self.assertEqual(expected, entry["metadata"]["gameVersionNames"])
            self.assertIn("修复图标", entry["metadata"]["changelog"])
            self.assertIn("**Markdown**", entry["metadata"]["changelog"])
            self.assertEqual("requiredDependency", entry["metadata"]["relations"]["projects"][0]["type"])
            self.assertEqual(entry["sha256"], publisher.digest(entry["file"]))
        original = self.jar("neoforge", version="changed")
        self.assertNotEqual(publisher.digest(original), plan["entries"][0]["sha256"])
        self.assertEqual(publisher.digest(plan["entries"][0]["file"]), plan["entries"][0]["sha256"])

    def test_wrong_version_blocks_whole_batch_before_staging(self):
        self.jar("forge", version="old")
        with self.assertRaisesRegex(publisher.PublishError, "modid/version"):
            self.plan()
        self.assertFalse((self.root / "staged").exists())

    def test_wrong_minecraft_and_unmapped_required_dependency_refused(self):
        self.jar("forge", minecraft="1.19.2")
        with self.assertRaisesRegex(publisher.PublishError, "Minecraft version"):
            self.plan()
        self.jar("forge", dependency="missing_mod")
        with self.assertRaisesRegex(publisher.PublishError, "dependency mapping"):
            self.plan()

    def test_missing_release_notes_not_synthesized_from_git(self):
        (self.notes / "example/1.2.3.md").unlink()
        with self.assertRaisesRegex(publisher.PublishError, "release notes"):
            self.plan()

    def test_duplicate_uploaded_files_are_skipped(self):
        plan = self.plan()
        calls = []
        def sender(entry, token):
            calls.append(entry["filename"])
            return 100 + len(calls)
        self.quiet_upload(plan, sender)
        self.quiet_upload(plan, sender)
        self.assertEqual(2, len(calls))

    def test_partial_success_can_resume_after_definite_rejection(self):
        plan = self.plan()
        calls = []
        def sender(entry, token):
            calls.append(entry["filename"])
            if len(calls) == 2:
                raise urllib.error.HTTPError("endpoint", 400, token, {}, None)
            return 321
        with self.assertRaisesRegex(publisher.PublishError, "HTTP 400"):
            self.quiet_upload(plan, sender)
        self.quiet_upload(plan, sender)
        self.assertEqual(3, len(calls))

    def test_uncertain_response_requires_reconciliation_without_token_leak(self):
        plan = self.plan()
        def sender(entry, token):
            raise TimeoutError(token)
        with self.assertRaises(publisher.PublishError) as raised:
            self.quiet_upload(plan, sender)
        self.assertNotIn("secret-that", str(raised.exception))
        with self.assertRaisesRegex(publisher.PublishError, "Uncertain previous"):
            self.quiet_upload(plan, sender)
        ledger_path = self.root / "ledger.json"
        key = next(iter(publisher.load_json(ledger_path)))
        publisher.resolve_upload(ledger_path, key, 777)
        self.quiet_upload(plan, lambda entry, token: 778)
        self.assertTrue(all(v["state"] == "uploaded" for v in publisher.load_json(ledger_path).values()))

    def test_changed_staged_file_blocks_every_upload(self):
        plan = self.plan()
        entries = publisher.load_json(plan)["entries"]
        Path(entries[-1]["file"]).write_bytes(b"altered")
        def sender(entry, token):
            self.fail("No file may upload when any staged file changed")
        with self.assertRaisesRegex(publisher.PublishError, "Staged file changed"):
            self.quiet_upload(plan, sender)

    def test_real_http_request_shape_and_redirect_protection_with_fake_transport(self):
        plan = publisher.load_json(self.plan())
        entry = plan["entries"][0]
        entry["metadata"].pop("gameVersionNames")
        entry["metadata"]["gameVersions"] = [11779, 10150, 9638, 9639]
        requests = []
        class Response(io.BytesIO):
            pass
        class Opener:
            def open(self, req, timeout):
                requests.append(req)
                return Response(b'{"id":1234}')
        with patch.object(publisher.urllib.request, "build_opener", return_value=Opener()):
            self.assertEqual(1234, publisher.upload_file(entry, "fake-token"))
        request = requests[0]
        self.assertEqual("POST", request.get_method())
        self.assertEqual(publisher.API + "/api/projects/123/upload-file", request.full_url)
        self.assertEqual("fake-token", request.get_header("X-api-token"))
        self.assertNotIn("fake-token", request.full_url)
        self.assertIn(b'name="metadata"', request.data)
        self.assertIn(b'name="file"', request.data)
        self.assertIn("修复图标".encode(), request.data)
        self.assertIn(b'"gameVersions": [11779, 10150, 9638, 9639]', request.data)
        self.assertNotIn(b'"gameVersionNames"', request.data)
        self.assertNotIn(b'Content-Type: application/json', request.data)
        self.assertIn(Path(entry["file"]).read_bytes(), request.data)
        self.assertIsNone(publisher.NoRedirect().redirect_request(request, None, 302, "redirect", {}, "https://example.com"))

    def test_lock_and_missing_token_never_upload(self):
        plan = self.plan()
        with self.assertRaisesRegex(publisher.PublishError, "TOKEN"):
            self.quiet_upload(plan, lambda e, t: self.fail("No token"), token="")
        lock = self.root / "held.lock"
        with publisher.exclusive_lock(lock):
            with self.assertRaisesRegex(publisher.PublishError, "already locked"):
                with publisher.exclusive_lock(lock):
                    self.fail("Duplicate lock")
        self.assertFalse(lock.exists())

    def test_builds_use_correct_jdk_and_final_forge_tasks_without_credentials(self):
        homes = {}
        for number in ["21", "17"]:
            home = self.root / ("jdk" + number)
            (home / "bin").mkdir(parents=True)
            (home / "bin/java.exe").touch()
            homes[number] = str(home)
        (self.repo / "gradlew.bat").touch()
        (self.repo / "forge/gradlew.bat").touch()
        with patch.dict(os.environ, {"CURSEFORGE_API_TOKEN": "test-secret"}):
            with patch.object(publisher.subprocess, "run", return_value=argparse.Namespace(returncode=0)) as run:
                with contextlib.redirect_stdout(io.StringIO()):
                    publisher.build_projects(self.config, self.root, ["example"], ["neoforge", "forge"], homes, True)
        self.assertEqual(2, run.call_count)
        for call, number in zip(run.call_args_list, ["21", "17"]):
            self.assertEqual(homes[number], call.kwargs["env"]["JAVA_HOME"])
            self.assertNotIn("CURSEFORGE_API_TOKEN", call.kwargs["env"])
            self.assertIn("--offline", call.args[0])
        self.assertIn(":reobfJarJar", run.call_args_list[1].args[0])
        with patch.object(publisher.subprocess, "run") as run:
            with self.assertRaisesRegex(publisher.PublishError, "Java 17"):
                publisher.build_projects(self.config, self.root, ["example"], ["neoforge", "forge"], {"21": homes["21"]}, False)
            run.assert_not_called()

    def test_version_resolution_excludes_bukkit_and_general_same_name(self):
        versions = [
            {"id": 12735, "name": "1.21.1", "gameVersionTypeID": 1},
            {"id": 16115, "name": "1.21.1", "gameVersionTypeID": 615},
            {"id": 11779, "name": "1.21.1", "gameVersionTypeID": 77784},
            {"id": 10150, "name": "NeoForge", "gameVersionTypeID": 68441},
        ]
        types = [{"id": 77784, "name": "Minecraft 1.21"}, {"id": 68441, "name": "Modloader"}]
        self.assertEqual([11779, 10150], publisher.version_ids(["1.21.1", "NeoForge"], versions, types))
        with self.assertRaisesRegex(publisher.PublishError, "uniquely"):
            publisher.version_ids(["1.21.1"], versions, [])
        with self.assertRaisesRegex(publisher.PublishError, "uniquely"):
            publisher.version_ids(["1.21.1"], versions + [versions[2]], types)

    def test_response_details_recorded_without_plain_or_json_escaped_token(self):
        plan = self.plan()
        secret = "test-secret"
        def sender(entry, token):
            body = b'{"errorCode":42,"errorMessage":"Invalid versions; \\u0074est-secret","requestHeaders":"hidden"}'
            raise urllib.error.HTTPError("endpoint", 400, "Bad Request", {}, io.BytesIO(body))
        with self.assertRaisesRegex(publisher.PublishError, "Invalid versions") as raised:
            self.quiet_upload(plan, sender, secret)
        self.assertNotIn(secret, str(raised.exception))
        ledger = publisher.load_json(self.root / "ledger.json")
        record = next(iter(ledger.values()))
        self.assertEqual(400, record["http_status"])
        self.assertIn("[REDACTED]", record["error"])
        self.assertNotIn(secret, record["error"])
        self.assertNotIn("requestHeaders", record["error"])

    def test_environment_group_is_resolved_without_unrelated_same_names(self):
        versions = [{"id": 9638, "name": "Client", "gameVersionTypeID": 75208},
                    {"id": 9639, "name": "Server", "gameVersionTypeID": 75208},
                    {"id": 999, "name": "Client", "gameVersionTypeID": 1}]
        types = [{"id": 75208, "name": "Environment"}]
        self.assertEqual([9638, 9639], publisher.version_ids(["Client", "Server"], versions, types))
        with self.assertRaisesRegex(publisher.PublishError, "uniquely"):
            publisher.version_ids(["Client"], versions, [])

    def test_legacy_plan_gains_environments_without_mutating_review_snapshot(self):
        plan = publisher.load_json(self.plan())
        plan["entries"] = plan["entries"][:1]
        plan["entries"][0]["metadata"]["gameVersionNames"] = ["1.21.1", "NeoForge"]
        types = [{"id": 10, "name": "Minecraft 1.21"}, {"id": 11, "name": "Modloader"},
                 {"id": 12, "name": "Environment"}]
        versions = [{"id": 100, "name": "1.21.1", "gameVersionTypeID": 10},
                    {"id": 101, "name": "NeoForge", "gameVersionTypeID": 11},
                    {"id": 102, "name": "Client", "gameVersionTypeID": 12},
                    {"id": 103, "name": "Server", "gameVersionTypeID": 12}]
        resolved = publisher.resolved_entries(plan, "fake-token",
                   lambda endpoint, token: types if endpoint.endswith("version-types") else versions)
        self.assertEqual([100, 101, 102, 103], resolved[0]["metadata"]["gameVersions"])
        self.assertEqual(["1.21.1", "NeoForge"], plan["entries"][0]["metadata"]["gameVersionNames"])
        self.assertEqual(plan["entries"][0]["sha256"], resolved[0]["sha256"])

    def test_project_environment_override_and_invalid_environment_preflight(self):
        self.config["projects"]["example"]["environments"] = ["Server"]
        plan = publisher.load_json(self.plan())
        self.assertEqual(["1.21.1", "NeoForge", "Server"], plan["entries"][0]["metadata"]["gameVersionNames"])
        self.config["projects"]["example"]["environments"] = []
        with self.assertRaisesRegex(publisher.PublishError, "Invalid environments"):
            publisher.prepare(self.config, self.root, ["example"], ["neoforge"], self.root/"invalid", self.notes)
        self.assertFalse((self.root/"invalid").exists())

    def test_read_only_catalog_failure_does_not_start_an_upload(self):
        plan = self.plan()
        with patch.object(publisher, "fetch_api", side_effect=publisher.PublishError("catalog failed")):
            with patch.object(publisher, "upload_file") as sender:
                with self.assertRaisesRegex(publisher.PublishError, "catalog failed"):
                    publisher.upload_plan(plan, self.root / "ledger.json", "token")
                sender.assert_not_called()
        self.assertFalse((self.root / "ledger.json").exists())


if __name__ == "__main__":
    unittest.main()
