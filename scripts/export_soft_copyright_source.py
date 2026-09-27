#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE_ROOT = REPO_ROOT / "multi-platform-app" / "src"
DEFAULT_OUTPUT_DIR = REPO_ROOT / "docs" / "compliance" / "generated"
PAGE_LINES = 50
TOTAL_PAGES = 60
MAX_LINES = PAGE_LINES * TOTAL_PAGES

ALLOWED_SUFFIXES = {
    ".js",
    ".json",
    ".vue",
    ".ts",
    ".css",
    ".scss",
    ".less",
}

PRIORITY_FILES = [
    "manifest.json",
    "pages.json",
    "App.vue",
    "main.js",
    "main.ts",
]

EXCLUDED_PARTS = {
    "node_modules",
    "dist",
    "build",
    ".git",
    "uni_modules",
}


def iter_source_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if any(part in EXCLUDED_PARTS for part in path.parts):
            continue
        if path.suffix.lower() not in ALLOWED_SUFFIXES:
            continue
        files.append(path)

    def sort_key(path: Path) -> tuple[int, str]:
        name = path.name
        priority = PRIORITY_FILES.index(name) if name in PRIORITY_FILES else len(PRIORITY_FILES)
        return priority, str(path.relative_to(REPO_ROOT))

    return sorted(files, key=sort_key)


def file_block(path: Path) -> list[str]:
    relative = path.relative_to(REPO_ROOT)
    try:
        content = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        content = path.read_text(encoding="utf-8", errors="replace").splitlines()
    lines = [f"// FILE: {relative}"]
    lines.extend(content)
    lines.append("")
    return lines


def trim_for_soft_copyright(lines: list[str]) -> list[str]:
    if len(lines) <= MAX_LINES:
        return lines
    head = lines[: MAX_LINES // 2]
    tail = lines[-MAX_LINES // 2 :]
    divider = [
        "",
        "// ... middle source omitted for application packet review ...",
        "",
    ]
    return head + divider + tail


def write_output(lines: Iterable[str], files: list[Path], output_dir: Path, version: str) -> tuple[Path, Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    source_path = output_dir / f"qiuou-soft-copyright-source-{version}.txt"
    manifest_path = output_dir / f"qiuou-soft-copyright-files-{version}.txt"

    source_path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
    manifest_lines = [
        "丘偶软著源码摘录文件清单",
        f"source_root={DEFAULT_SOURCE_ROOT.relative_to(REPO_ROOT)}",
        f"file_count={len(files)}",
        "",
    ]
    manifest_lines.extend(str(path.relative_to(REPO_ROOT)) for path in files)
    manifest_path.write_text("\n".join(manifest_lines).rstrip() + "\n", encoding="utf-8")
    return source_path, manifest_path


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate a reviewable source excerpt for software copyright filing."
    )
    parser.add_argument(
        "--source-root",
        default=str(DEFAULT_SOURCE_ROOT),
        help="Source directory to collect files from.",
    )
    parser.add_argument(
        "--output-dir",
        default=str(DEFAULT_OUTPUT_DIR),
        help="Directory to write the generated excerpt files to.",
    )
    parser.add_argument(
        "--version",
        default="v1.0",
        help="Suffix used in generated file names.",
    )
    args = parser.parse_args()

    source_root = Path(args.source_root).resolve()
    output_dir = Path(args.output_dir).resolve()
    if not source_root.exists():
        raise SystemExit(f"source root not found: {source_root}")

    files = iter_source_files(source_root)
    if not files:
        raise SystemExit(f"no source files found under: {source_root}")

    all_lines: list[str] = []
    for path in files:
        all_lines.extend(file_block(path))

    trimmed_lines = trim_for_soft_copyright(all_lines)
    source_path, manifest_path = write_output(trimmed_lines, files, output_dir, args.version)

    print(f"generated source excerpt: {source_path}")
    print(f"generated file manifest: {manifest_path}")
    print(f"total source files scanned: {len(files)}")
    print(f"output lines written: {len(trimmed_lines)}")


if __name__ == "__main__":
    main()
