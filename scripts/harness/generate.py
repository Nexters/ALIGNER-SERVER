#!/usr/bin/env python3
"""harness/ 원본에서 하네스별 설정을 생성한다.

    python3 scripts/harness/generate.py            # 전부
    python3 scripts/harness/generate.py --only claude

생성 대상 디렉터리(.claude/, .codex/, .agents/)는 매번 비우고 다시 쓴다.
원본에서 파일을 지웠는데 생성물에 남아 있으면 하네스마다 다른 스킬을 보게 된다.
"""

import argparse
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from adapters.antigravity import AntigravityAdapter  # noqa: E402
from adapters.base import load_bundle  # noqa: E402
from adapters.claude import ClaudeAdapter  # noqa: E402
from adapters.codex import CodexAdapter  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent.parent
ADAPTERS = (ClaudeAdapter, CodexAdapter, AntigravityAdapter)

# 생성 대상 밖이지만 지우면 안 되는 것 — 개인 설정은 .gitignore 대상이라 보존한다
PRESERVE = (".claude/settings.local.json",)


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
    return files, roots


def write(files, roots):
    preserved = {}
    for relative in PRESERVE:
        path = ROOT / relative
        if path.exists():
            preserved[relative] = path.read_bytes()

    for root in roots:
        target = ROOT / root
        if target.exists():
            shutil.rmtree(target)

    for relative, content in sorted(files.items()):
        path = ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        if path.suffix == ".sh":
            path.chmod(0o755)

    for relative, content in preserved.items():
        path = ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)


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
