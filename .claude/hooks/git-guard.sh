#!/bin/sh
# main·develop 보호 훅 (PreToolUse: Bash)
#
# CONTRIBUTING.md §8 "main·develop에 직접 푸시하지 않습니다"와 §1 "force push 금지",
# 그리고 AGENTS.md §7 "--no-verify·SKIP_HOOKS=1로 우회하지 않는다"를 로컬에서 강제한다.
# 저장소 브랜치 보호 설정이 서버 쪽 최종 방어선이고, 이건 그 앞단이다.
#
# 차단 대상
#   1. main / develop 으로의 push (인자로 지정했든, 현재 브랜치가 그것이든)
#   2. --force / -f push (--force-with-lease 는 허용 — 작업 브랜치 rebase 후 필요하다)
#   3. --all / --mirror / 와일드카드 refspec push (보호 브랜치가 묻어 올라간다)
#   4. 커밋 훅 우회 — git commit --no-verify / -n, SKIP_HOOKS=1 git ...
#
# 명령을 토큰으로 쪼개 "서브커맨드의 인자"만 본다. 문자열을 통째로 정규식에 넣으면
# 커밋 메시지에 push·develop 이 들어간 git commit 까지 걸린다.
#
# 서브커맨드를 찾을 때 git 전역 옵션(-C, -c, --git-dir …)을 건너뛰는 것이 중요하다.
# 이걸 빠뜨리면 `git -C . push origin HEAD:main` 이 "." 을 서브커맨드로 오인해 통과한다.
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
ENV_PREFIX = re.compile(r"^([A-Za-z_][A-Za-z0-9_]*)=(.*)$")

# 뒤에 값이 따라오는 git 전역 옵션 (--opt=value 형태는 값이 붙어 있으니 제외)
GLOBAL_WITH_VALUE = ("-C", "-c", "--git-dir", "--work-tree", "--namespace",
                     "--exec-path", "--super-prefix")
# 뒤에 값이 따라오는 push 옵션 — refspec 으로 오인하면 안 된다
PUSH_WITH_VALUE = ("-o", "--push-option", "--repo", "--receive-pack", "--exec")
# git commit 에서 값을 먹는 단축 옵션 — 클러스터(-nm) 해석을 여기서 끊는다
COMMIT_SHORT_WITH_VALUE = set("mcCFuS")


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


def split_value_args(args, with_value):
    """값을 먹는 옵션의 값까지 함께 걷어내고 위치 인자만 남긴다."""
    positional, skip = [], False
    for arg in args:
        if skip:
            skip = False
            continue
        if arg in with_value:
            skip = True
            continue
        if arg.startswith("-"):
            continue
        positional.append(arg)
    return positional


def has_short_flag(args, flag):
    """-n, -nm 같은 단축 옵션 클러스터에서 flag 를 찾는다."""
    for arg in args:
        if arg == "--":
            break
        if not arg.startswith("-") or arg.startswith("--"):
            continue
        for ch in arg[1:]:
            if ch == flag:
                return True
            if ch in COMMIT_SHORT_WITH_VALUE:
                break  # 이 뒤는 옵션 값이다
    return False


def check_push(args, clause):
    if any(a in ("--force", "-f") for a in args):
        block(
            "force push 는 --force-with-lease 만 허용합니다. "
            "남의 커밋을 날릴 수 있습니다 (CONTRIBUTING.md 1. 브랜치 전략).",
            clause,
        )

    if any(a in ("--all", "--mirror") for a in args):
        block(
            "--all·--mirror push 는 로컬 main·develop 까지 함께 올립니다. "
            "올릴 브랜치를 명시하세요 (CONTRIBUTING.md 8. 하지 말 것).",
            clause,
        )

    # push 뒤의 위치 인자 — 리모트 이름과 refspec 들
    refs = split_value_args(args, PUSH_WITH_VALUE)

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
        if "*" in destination:
            block(
                "와일드카드 refspec 은 보호 브랜치를 함께 갱신할 수 있습니다. "
                "올릴 브랜치를 명시하세요 (CONTRIBUTING.md 8. 하지 말 것).",
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


def check_commit(args, clause):
    if "--no-verify" in args or has_short_flag(args, "n"):
        block(
            "커밋 훅을 우회할 수 없습니다 (--no-verify·-n). "
            "훅이 막으면 원인을 고치고, 오탐이면 사용자에게 알리세요 (AGENTS.md 7. 가드).",
            clause,
        )


# && · || · | · ; · 개행으로 이어붙인 경우까지 각 절을 따로 검사한다
for clause in re.split(r"&&|\|\||\||;|\n", command):
    clause = clause.strip()
    if not clause:
        continue

    try:
        tokens = shlex.split(clause)
    except ValueError:
        tokens = clause.split()

    # FOO=bar git push ... 형태의 환경변수 접두사를 건너뛴다
    i, env = 0, {}
    while i < len(tokens):
        matched = ENV_PREFIX.match(tokens[i])
        if not matched:
            break
        env[matched.group(1)] = matched.group(2)
        i += 1
    if i >= len(tokens) or tokens[i] != "git":
        continue

    if env.get("SKIP_HOOKS", "") not in ("", "0"):
        block(
            "SKIP_HOOKS 로 훅을 우회할 수 없습니다. "
            "훅이 막으면 원인을 고치고, 오탐이면 사용자에게 알리세요 (AGENTS.md 7. 가드).",
            clause,
        )

    # git 전역 옵션(-C <경로>, -c <k=v>, --git-dir=… 등)을 건너뛰고 서브커맨드를 찾는다
    j = i + 1
    while j < len(tokens):
        token = tokens[j]
        if not token.startswith("-"):
            break
        j += 2 if token in GLOBAL_WITH_VALUE else 1
    if j >= len(tokens):
        continue

    subcommand, args = tokens[j], tokens[j + 1:]
    if subcommand == "push":
        check_push(args, clause)
    elif subcommand == "commit":
        check_commit(args, clause)

sys.exit(0)
'
