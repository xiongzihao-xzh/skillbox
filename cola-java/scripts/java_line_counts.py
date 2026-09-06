#!/usr/bin/env python3
"""Report physical Java file lengths; architecture decisions remain with the reviewer."""

import argparse
import json
from pathlib import Path
import subprocess


def changed_files(root):
    def git(*args):
        return subprocess.check_output(["git", "-C", str(root), *args], stderr=subprocess.PIPE)
    if Path(git("rev-parse", "--show-toplevel").decode().strip()).resolve() != root:
        raise ValueError("--root must be the Git repository root for changed-file mode")
    try:
        git("rev-parse", "--verify", "HEAD")
    except subprocess.CalledProcessError:
        tracked = git("ls-files", "-z")
    else:
        tracked = git("diff", "--cached", "--name-only", "-z", "--diff-filter=ACMRT", "HEAD", "--")
        tracked += git("diff", "--name-only", "-z", "--diff-filter=ACMRT", "--")
    names = tracked + git("ls-files", "--others", "--exclude-standard", "-z")
    return sorted({root / name.decode("utf-8") for name in names.split(b"\0")
                   if name.endswith(b".java") and (root / name.decode("utf-8")).is_file()})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--all", action="store_true", help="Explicitly inspect every Java file under root")
    parser.add_argument("--generated-root", action="append", default=[], metavar="PATH=BASIS")
    parser.add_argument("files", nargs="*")
    args = parser.parse_args()
    root = args.root.resolve()
    def within_root(path):
        resolved = path.resolve()
        try:
            resolved.relative_to(root)
        except ValueError:
            parser.error("path is outside --root: " + str(path))
        return resolved

    if not root.is_dir():
        parser.error("--root must be an existing directory")
    exclusions = []
    for value in args.generated_root:
        name, separator, basis = value.partition("=")
        if not separator or not name or not basis.strip():
            parser.error("--generated-root requires PATH=BASIS with a verified generator/build basis")
        exclusions.append((within_root(root / name), basis.strip()))
    if args.all and args.files:
        parser.error("choose either --all or explicit files")
    scope = "all" if args.all else "explicit" if args.files else "changed"
    try:
        paths = sorted(root.rglob("*.java")) if args.all else (
            [root / name for name in args.files] if args.files else changed_files(root))
    except (ValueError, subprocess.CalledProcessError) as error:
        parser.error(str(error))
    files = []
    excluded = []
    for path in sorted({within_root(path) for path in paths}):
        name = path.relative_to(root).as_posix()
        if ".git" in path.relative_to(root).parts:
            continue
        if path.suffix != ".java" or not path.is_file():
            parser.error("expected an existing Java file: " + name)
        basis = next((basis for directory, basis in exclusions if directory in path.parents), None)
        if basis:
            excluded.append({"path": name, "basis": basis})
            continue
        try:
            with path.open(encoding="utf-8", newline=None) as source:
                lines = sum(1 for _ in source)
        except (OSError, UnicodeError) as error:
            parser.error(str(error))
        files.append({"path": name, "lines": lines, "over_limit": lines > 500})
    print(json.dumps({"scope": scope, "limit": 500,
                      "files": files, "excluded": excluded}, ensure_ascii=False))


if __name__ == "__main__":
    main()
