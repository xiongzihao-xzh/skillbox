"""Observe the line-counter CLI through isolated filesystem fixtures."""

import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "java_line_counts.py"


class LineCountsTest(unittest.TestCase):
    def test_physical_lines_include_comments_and_nested_types_with_common_endings(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fixtures = {"Empty.java": (b"", 0), "NoFinalNewline.java": (b"a\nb", 2),
                        "Windows.java": (b"a\r\nb\r\n", 2), "OldMac.java": (b"a\rb\r", 2),
                        "Nested.java": (b"package p;\n\n// comment\nclass Outer {\n class Inner {}\n}\n", 6)}
            for name, (data, _) in fixtures.items():
                (root / name).write_bytes(data)
            result = subprocess.run([sys.executable, str(SCRIPT), "--root", directory, "--all"],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            actual = {f["path"]: f["lines"] for f in json.loads(result.stdout)["files"]}
            self.assertEqual(actual, {name: expected for name, (_, expected) in fixtures.items()})

    def test_missing_generation_basis_and_non_git_default_fail_explicitly(self):
        with tempfile.TemporaryDirectory() as directory:
            for arguments in [[], ["--all", "--generated-root", "target/generated-sources="]]:
                with self.subTest(arguments=arguments):
                    result = subprocess.run([sys.executable, str(SCRIPT), "--root", directory, *arguments],
                                            capture_output=True, text=True)
                    self.assertEqual(result.returncode, 2)
                    self.assertTrue(result.stderr)

    def test_outside_root_files_are_rejected_instead_of_counted(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "project"
            root.mkdir()
            (root.parent / "Outside.java").write_text("class Outside {}\n")
            result = subprocess.run(
                [sys.executable, str(SCRIPT), "--root", str(root), "../Outside.java"],
                capture_output=True, text=True,
            )
            self.assertEqual(result.returncode, 2)
            self.assertIn("outside", result.stderr.lower())

    def test_default_scope_combines_staged_unstaged_and_new_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def git(*args):
                subprocess.run(["git", "-C", directory, *args], check=True, capture_output=True)
            git("init", "-q")
            for name in ["Unchanged.java", "Edited.java", "Staged.java", "Deleted.java", "Reverted.java"]:
                (root / name).write_text("class Example {}\n")
            git("add", ".")
            git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
                "-c", "commit.gpgsign=false", "commit", "-qm", "fixture")
            (root / "Edited.java").write_text("// edited\nclass Example {}\n")
            (root / "Staged.java").write_text("// staged\nclass Example {}\n")
            git("add", "Staged.java")
            (root / "Reverted.java").write_text("// staged edit\nclass Example {}\n")
            git("add", "Reverted.java")
            (root / "Reverted.java").write_text("class Example {}\n")
            (root / "New file.java").write_text("class NewFile {}\n")
            (root / "Deleted.java").unlink()
            result = subprocess.run([sys.executable, str(SCRIPT), "--root", directory],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            report = json.loads(result.stdout)
            self.assertEqual(report["scope"], "changed")
            self.assertEqual([f["path"] for f in report["files"]],
                             ["Edited.java", "New file.java", "Reverted.java", "Staged.java"])

    def test_generated_exclusion_is_explicit_and_reports_its_basis(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            generated = root / "target/generated-sources"
            generated.mkdir(parents=True)
            (generated / "OrderMapperImpl.java").write_text("// generated\n" * 501)
            (root / "Entity.java").write_text("// handwritten\n" * 501)
            result = subprocess.run(
                [sys.executable, str(SCRIPT), "--root", directory, "--all",
                 "--generated-root", "target/generated-sources=MapStruct via pom.xml"],
                capture_output=True, text=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            report = json.loads(result.stdout)
            self.assertEqual([f["path"] for f in report["files"]], ["Entity.java"])
            self.assertTrue(report["files"][0]["over_limit"])
            self.assertEqual(report["excluded"], [{
                "path": "target/generated-sources/OrderMapperImpl.java",
                "basis": "MapStruct via pom.xml",
            }])

    def test_500_is_allowed_and_501_is_a_candidate_regardless_of_name(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "Order.java").write_text("// line\n" * 500)
            (root / "Configuration.java").write_text("// line\n" * 501)
            result = subprocess.run(
                [sys.executable, str(SCRIPT), "--root", directory,
                 "Order.java", "Configuration.java"],
                capture_output=True, text=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            files = {item["path"]: item for item in json.loads(result.stdout)["files"]}
            self.assertEqual(files["Order.java"]["lines"], 500)
            self.assertFalse(files["Order.java"]["over_limit"])
            self.assertEqual(files["Configuration.java"]["lines"], 501)
            self.assertTrue(files["Configuration.java"]["over_limit"])


if __name__ == "__main__":
    unittest.main()
