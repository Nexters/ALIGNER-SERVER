#!/usr/bin/env python3
"""원본과 생성물이 어긋났는지 검사한다. 파일을 고치지 않는다.

    python3 scripts/harness/validate.py

CI 와 로컬에서 같은 명령을 쓴다. 실패하면 "무엇이 다른지"와 "어떻게 고치는지"까지
출력한다 — drift 를 알려주고 방법을 안 알려주면 사람이 생성물을 손으로 고쳐서
맞춰버리고, 그러면 다음 생성 때 또 날아간다.

검사 항목
  1. 원본 frontmatter 가 규약을 지키는가 (name·description 필수, capability 이름)
  2. 생성물이 지금 원본으로 다시 만든 결과와 바이트 단위로 같은가
  3. 생성물에만 있는 파일이 없는가 (원본에서 지웠는데 남은 것)
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from adapters.base import load_bundle  # noqa: E402
from adapters.capabilities import CAPABILITIES  # noqa: E402
from generate import ADAPTERS, MANIFEST_NAME, ROOT, build  # noqa: E402

FIX_HINT = "고치는 법: python3 scripts/harness/generate.py 를 돌리고 결과를 커밋하세요."
CODEX_SKILL_BODY_MAX_BYTES = 8 * 1024


def check_sources(bundle):
    problems = []
    for group, documents in (
        ("skills", bundle.skills),
        ("agents", bundle.agents),
        ("rules", bundle.rules),
    ):
        for document in documents:
            if group == "rules":
                continue
            if not document.meta.get("name"):
                problems.append(f"{document.source}: frontmatter 에 name 이 없습니다")
            if not document.description:
                problems.append(f"{document.source}: frontmatter 에 description 이 없습니다")
            for capability in document.capabilities():
                if capability not in CAPABILITIES:
                    problems.append(
                        f"{document.source}: 모르는 capability '{capability}' — "
                        f"가능한 값: {', '.join(CAPABILITIES)}"
                    )
            if group == "skills" and len(document.body.encode("utf-8")) > CODEX_SKILL_BODY_MAX_BYTES:
                problems.append(
                    f"{document.source}: Codex Skill 본문이 8 KiB를 넘습니다"
                )
    return problems


def check_generated(files, roots):
    problems = []

    for relative, content in sorted(files.items()):
        path = ROOT / relative
        if not path.exists():
            problems.append(f"{relative}: 생성물이 없습니다")
            continue
        if path.read_text(encoding="utf-8") != content:
            problems.append(f"{relative}: 원본과 내용이 다릅니다")

    for root in roots:
        target = ROOT / root
        if not target.exists():
            continue
        manifest = target / MANIFEST_NAME
        if not manifest.exists():
            problems.append(f"{manifest.relative_to(ROOT)}: 생성물 manifest가 없습니다")

    return problems


def main():
    warnings = []
    try:
        bundle = load_bundle(ROOT)
        problems = check_sources(bundle)
        files, roots = build(warn=warnings.append)
        problems += check_generated(files, roots)
    except Exception as error:
        print(f"검증 실패 — 원본을 읽을 수 없습니다: {error}", file=sys.stderr)
        return 1

    for message in warnings:
        print(f"경고: {message}", file=sys.stderr)

    if problems:
        print(f"검증 실패 — {len(problems)}건", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        print(f"\n{FIX_HINT}", file=sys.stderr)
        return 1

    print(f"검증 통과 — 생성물 {len(files)}개가 원본과 일치합니다")
    return 0


if __name__ == "__main__":
    sys.exit(main())
