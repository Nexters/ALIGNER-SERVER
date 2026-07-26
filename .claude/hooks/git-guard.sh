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
# 명령을 토큰으로 쪼개 "git push 의 인자"만 본다. 문자열을 통째로 정규식에 넣으면
# 커밋 메시지에 push·develop 이 들어간 git commit 까지 걸린다.
#
# 종료 코드 2 = 도구 호출 차단. stderr 메시지가 모델에게 전달된다.

exec /usr/bin/python3 -c '
import json, re, shlex, subprocess, sys

try:
    payload = json.load(sys.stdin)
except Exception:
    sys.exit(0)

command = (payload.get("tool_input") or {}).get("command") or ""
PROTECTED = ("main", "develop")
ENV_PREFIX = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*=")


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
    if not clause:
        continue

    try:
        tokens = shlex.split(clause)
    except ValueError:
        tokens = clause.split()

    # FOO=bar git push ... 형태의 환경변수 접두사를 건너뛴다
    i = 0
    while i < len(tokens) and ENV_PREFIX.match(tokens[i]):
        i += 1
    if i >= len(tokens) or tokens[i] != "git":
        continue

    args = tokens[i + 1:]
    positional = [a for a in args if not a.startswith("-")]

    # 첫 위치 인자가 서브커맨드다. push 가 아니면 볼 것이 없다
    if not positional or positional[0] != "push":
        continue

    if any(a in ("--force", "-f") for a in args):
        block(
            "force push 는 --force-with-lease 만 허용합니다. "
            "남의 커밋을 날릴 수 있습니다 (CONTRIBUTING.md 1. 브랜치 전략).",
            clause,
        )

    # push 뒤의 인자 — 리모트 이름과 refspec 들
    refs = positional[1:]

    # refspec 의 목적지 브랜치를 본다
    #   main / HEAD:develop / HEAD:refs/heads/develop / :develop (삭제)
    for ref in refs:
        destination = ref.split(":")[-1].rsplit("/", 1)[-1]
        if destination in PROTECTED:
            block(
                "main·develop 에 직접 푸시할 수 없습니다. "
                "작업 브랜치를 push 하고 develop 으로 PR 을 올리세요 "
                "(CONTRIBUTING.md 8. 하지 말 것).",
                clause,
            )

    # 브랜치를 명시하지 않은 push — 현재 브랜치가 보호 대상이면 차단
    # (refs 가 리모트 이름 하나뿐인 경우도 포함)
    if len(refs) <= 1 and current_branch() in PROTECTED:
        block(
            f"현재 브랜치가 {current_branch()} 입니다. 직접 푸시할 수 없습니다. "
            "작업 브랜치를 만들어 develop 으로 PR 을 올리세요 (CONTRIBUTING.md 8. 하지 말 것).",
            clause,
        )

sys.exit(0)
'
