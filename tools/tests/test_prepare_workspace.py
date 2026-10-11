"""Exercise isolation and refusal paths with temporary Git repositories, not real projects."""
import argparse
import contextlib
import importlib.util
import io
import subprocess
import tempfile
import unittest
from pathlib import Path

script = Path(__file__).resolve().parents[1] / "prepare-addon-workspace.py"
spec = importlib.util.spec_from_file_location("prepare_workspace", script)
helper = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helper)


class WorkspaceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="conflux-workspace-test-")
        self.base = Path(self.temp.name)
        self.core = self.repo("Ars-Conflux")
        self.addon = self.repo("Example-Nouveau")
        self.args = argparse.Namespace(
            addon=str(self.addon), core=str(self.core), workspace=str(self.base / "tasks" / "one"),
            branch="agent/example", addon_ref="HEAD", core_ref="HEAD", apply=True,
        )

    def tearDown(self):
        # Each worktree registration and its owning test repository live inside this temp root.
        # Read-only pack files on Windows must be made writable for TemporaryDirectory cleanup.
        import os, stat
        for root, _, files in os.walk(self.base):
            for file in files:
                os.chmod(Path(root) / file, stat.S_IWRITE | stat.S_IREAD)
        self.temp.cleanup()

    def repo(self, name):
        path = self.base / name
        path.mkdir()
        subprocess.run(["git", "init", "-b", "main", str(path)], check=True, capture_output=True)
        helper.git(path, "config", "user.name", "Workspace test")
        helper.git(path, "config", "user.email", "workspace@users.invalid")
        helper.git(path, "config", "commit.gpgsign", "false")
        (path / ".gitignore").write_text("libs/\nforge/libs/\nbuild/\n", encoding="utf-8")
        (path / "VERSION").write_text("test-baseline\n", encoding="utf-8")
        helper.git(path, "add", ".")
        helper.git(path, "commit", "-m", "Fixture baseline")
        (path / "libs").mkdir()
        (path / "libs" / "dependency.fixture").write_bytes(b"separate development input")
        return path

    def prepare(self):
        with contextlib.redirect_stdout(io.StringIO()):
            return helper.prepare(self.args)

    def test_frozen_core_separate_build_inputs_and_addon_edits(self):
        core_before = helper.git(self.core, "rev-parse", "HEAD").stdout
        plan = self.prepare()
        frozen = Path(plan["core"]["worktree"])
        working = Path(plan["addon"]["worktree"])
        self.assertEqual(plan["state"], "prepared")
        self.assertEqual(helper.git(frozen, "rev-parse", "HEAD").stdout, core_before)
        self.assertEqual(helper.git(frozen, "status", "--porcelain", "--untracked-files=no").stdout, "")
        (working / "VERSION").write_text("only my addon changed\n", encoding="utf-8")
        (frozen / "libs" / "dependency.fixture").write_bytes(b"local test input")
        self.assertEqual((self.addon / "VERSION").read_text(), "test-baseline\n")
        self.assertEqual((self.core / "libs" / "dependency.fixture").read_bytes(), b"separate development input")
        self.assertEqual(helper.git(self.core, "rev-parse", "HEAD").stdout, core_before)
        self.assertTrue(Path(plan["manual_test_mods"]).is_dir())

    def test_existing_destination_is_not_touched(self):
        destination = Path(self.args.workspace)
        destination.mkdir(parents=True)
        marker = destination / "existing.txt"
        marker.write_text("keep me", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "already exists"):
            self.prepare()
        self.assertEqual(marker.read_text(), "keep me")
        self.assertNotEqual(helper.git(self.addon, "show-ref", "--verify", "refs/heads/agent/example", check=False).returncode, 0)

    def test_dirty_source_prevents_worktree_creation(self):
        (self.core / "VERSION").write_text("uncommitted Core work\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Tracked changes"):
            self.prepare()
        self.assertFalse(Path(self.args.workspace).exists())
        self.assertEqual(helper.git(self.addon, "worktree", "list", "--porcelain").stdout.count("worktree "), 1)


if __name__ == "__main__":
    unittest.main()
