#!/usr/bin/env python3
"""harness/ 원본에서 하네스별 설정을 생성한다.

    python3 scripts/harness/generate.py            # 전부
    python3 scripts/harness/generate.py --only claude

생성기는 manifest에 등록한 파일만 소유한다. 같은 디렉터리의 사용자 설정은 보존한다.
"""

import argparse
import json
import sys
from pathlib import Path, PurePosixPath

sys.path.insert(0, str(Path(__file__).resolve().parent))

from adapters.antigravity import AntigravityAdapter  # noqa: E402
from adapters.base import load_bundle  # noqa: E402
from adapters.claude import ClaudeAdapter  # noqa: E402
from adapters.codex import CodexAdapter  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent.parent
ADAPTERS = (ClaudeAdapter, CodexAdapter, AntigravityAdapter)
MANIFEST_NAME = ".generated-manifest.json"


def build(only=None, warn=print):
    """{상대경로: 내용} 을 모아 돌려준다. 파일을 쓰지는 않는다."""
    bundle = load_bundle(ROOT)
    files, roots = {}, []
    for adapter_type in ADAPTERS:
        adapter = adapter_type(warn=warn)
        if only and adapter.name != only:
            continue
        roots.append(adapter.output_root)
        files.update(adapter.generate(bundle))
    for root in roots:
        owned = sorted(
            relative.removeprefix(root + "/")
            for relative in files
            if relative.startswith(root + "/")
        )
        files[f"{root}/{MANIFEST_NAME}"] = (
            json.dumps(
                {
                    "_generated": "harness/ 원본에서 생성됩니다. 직접 고치지 마세요.",
                    "files": owned,
                },
                indent=2,
                ensure_ascii=False,
            )
            + "\n"
        )
    return files, roots


def write(files, roots):
    for root in roots:
        target = ROOT / root
        manifest = target / MANIFEST_NAME
        owned = []
        if manifest.exists():
            try:
                owned = json.loads(manifest.read_text(encoding="utf-8")).get("files", [])
            except (OSError, ValueError, TypeError):
                raise ValueError(f"{manifest}: 생성물 manifest를 읽을 수 없습니다")
        elif target.exists():
            for path in target.rglob("*"):
                if not path.is_file():
                    continue
                try:
                    if "직접 고치지 마세요" in path.read_text(encoding="utf-8"):
                        owned.append(path.relative_to(target).as_posix())
                except (OSError, UnicodeDecodeError):
                    continue

        for relative in owned:
            relative_path = PurePosixPath(relative)
            if relative_path.is_absolute() or ".." in relative_path.parts:
                raise ValueError(f"{manifest}: 안전하지 않은 생성물 경로 '{relative}'")
            path = target / relative
            if path.is_file():
                path.unlink()
        if manifest.is_file():
            manifest.unlink()

    for relative, content in sorted(files.items()):
        path = ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        if path.suffix == ".sh":
            path.chmod(0o755)


def main():
    parser = argparse.ArgumentParser(description="하네스 설정 생성")
    parser.add_argument("--only", choices=[a.name for a in ADAPTERS], help="한 하네스만 생성")
    args = parser.parse_args()

    warnings = []

    def warn(message):
        warnings.append(message)

    files, roots = build(only=args.only, warn=warn)
    write(files, roots)

    for message in warnings:
        print(f"경고: {message}", file=sys.stderr)

    print(f"생성 완료 — {len(files)}개 파일, 대상 {', '.join(roots)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
