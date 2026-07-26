#!/bin/sh
# main·develop 보호 훅 (PreToolUse: Bash)
#
# CONTRIBUTING.md §8 "main·develop에 직접 푸시하지 않습니다"와 §1 "force push 금지"를
# 로컬에서 강제한다. 저장소 브랜치 보호 설정이 서버 쪽 최종 방어선이고, 이건 그 앞단이다.
#
# 차단 대상
#   1. main / develop 으로의 push (인자로 지정했든, 현재 브랜치가 그것이든)
#   2. --force / -f push (--force-with-lease 는 허용 — 작업 브랜치 rebase 후 필요하다)
#
# 종료 코드 2 = 도구 호출 차단. stderr 메시지가 모델에게 전달된다.

exec /usr/bin/python3 -c '
import json, re, subprocess, sys

try:
    payload = json.load(sys.stdin)
except Exception:
    sys.exit(0)

command = (payload.get("tool_input") or {}).get("command") or ""
PROTECTED = ("main", "develop")


def block(reason, clause):
    sys.stderr.write(f"차단: {reason}\n명령: {clause}\n")
    sys.exit(2)


def current_branch():
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            capture_output=True, text=True, timeout=5,
        )
        return out.stdout.strip()
    except Exception:
        return ""


# && · || · ; · 개행으로 이어붙인 경우까지 각 절을 따로 검사한다
for clause in re.split(r"&&|\|\||;|\n", command):
    clause = clause.strip()
    if not re.search(r"\bgit\b.*\bpush\b", clause):
        continue

    if re.search(r"(^|\s)(--force|-f)(\s|$)", clause):
        block(
            "force push 는 --force-with-lease 만 허용합니다. "
            "남의 커밋을 날릴 수 있습니다 (CONTRIBUTING.md 1. 브랜치 전략).",
            clause,
        )

    # 인자에 보호 브랜치가 직접 등장하는 경우 — git push origin main, HEAD:develop
    if re.search(r"(^|[\s:])(main|develop)(\s|$|:)", clause):
        block(
            "main·develop 에 직접 푸시할 수 없습니다. "
            "작업 브랜치를 push 하고 develop 으로 PR 을 올리세요 (CONTRIBUTING.md 8. 하지 말 것).",
            clause,
        )

    # 브랜치를 명시하지 않은 push — 현재 브랜치가 보호 대상이면 차단
    tokens = [t for t in clause.split() if not t.startswith("-")]
    if "push" in tokens and len(tokens) <= 3 and current_branch() in PROTECTED:
        block(
            f"현재 브랜치가 {current_branch()} 입니다. 직접 푸시할 수 없습니다. "
            "작업 브랜치를 만들어 develop 으로 PR 을 올리세요 (CONTRIBUTING.md 8. 하지 말 것).",
            clause,
        )

sys.exit(0)
'
