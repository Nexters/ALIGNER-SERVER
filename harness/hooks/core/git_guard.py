"""main·develop 보호 로직 — 하네스와 무관한 순수 판정부.

CONTRIBUTING.md 8 "main·develop에 직접 푸시하지 않습니다"와 1 "force push 금지",
AGENTS.md 7 "--no-verify·SKIP_HOOKS=1로 우회하지 않는다"를 강제한다.
저장소 브랜치 보호 설정이 서버 쪽 최종 방어선이고, 이건 그 앞단이다.

차단 대상
  1. main / develop 으로의 push (인자로 지정했든, 현재 브랜치가 그것이든)
  2. --force / -f push (--force-with-lease 는 허용 — 작업 브랜치 rebase 후 필요하다)
  3. --all / --mirror / 와일드카드 refspec push (보호 브랜치가 묻어 올라간다)
  4. 커밋 훅 우회 — git commit --no-verify / -n, SKIP_HOOKS=1 git ...

명령을 토큰으로 쪼개 "서브커맨드의 인자"만 본다. 문자열을 통째로 정규식에 넣으면
커밋 메시지에 push·develop 이 들어간 git commit 까지 걸린다.

서브커맨드를 찾을 때 git 전역 옵션(-C, -c, --git-dir …)을 건너뛰는 것이 중요하다.
이걸 빠뜨리면 `git -C . push origin HEAD:main` 이 "." 을 서브커맨드로 오인해 통과한다.

이 모듈은 stdin·환경변수·종료 코드를 모르게 둔다. 하네스마다 다른 입출력은
harness/hooks/adapters/ 가 처리한다.
"""

import re
import shlex
import subprocess
from dataclasses import dataclass

PROTECTED = ("main", "develop")

_ENV_PREFIX = re.compile(r"^([A-Za-z_][A-Za-z0-9_]*)=(.*)$")

# 절 구분자 — && || | ; 개행
_CLAUSE_SEPARATOR = re.compile(r"&&|\|\||\||;|\n")

# 뒤에 값이 따라오는 git 전역 옵션 (--opt=value 형태는 값이 붙어 있으니 제외)
_GLOBAL_WITH_VALUE = frozenset(
    ("-C", "-c", "--git-dir", "--work-tree", "--namespace", "--exec-path", "--super-prefix")
)
_ENV_WITH_VALUE = frozenset(("-u", "--unset", "-C", "--chdir", "-S", "--split-string"))

# 뒤에 값이 따라오는 push 옵션 — refspec 으로 오인하면 안 된다
_PUSH_WITH_VALUE = frozenset(("-o", "--push-option", "--repo", "--receive-pack", "--exec"))

# git commit 에서 값을 먹는 단축 옵션 — 클러스터(-nm) 해석을 여기서 끊는다
_COMMIT_SHORT_WITH_VALUE = frozenset("mcCFuS")

_DOC_BRANCH = "CONTRIBUTING.md 8. 하지 말 것"
_DOC_FORCE = "CONTRIBUTING.md 1. 브랜치 전략"
_DOC_GUARD = "AGENTS.md 7. 가드"


@dataclass(frozen=True)
class Decision:
    """차단 판정. blocked=False 면 나머지 필드는 비어 있다."""

    blocked: bool
    reason: str = ""
    clause: str = ""

    @property
    def message(self) -> str:
        if not self.blocked:
            return ""
        return f"차단: {self.reason}\n명령: {self.clause}"


ALLOWED = Decision(blocked=False)


def _current_branch() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            capture_output=True,
            text=True,
            timeout=5,
        )
        return out.stdout.strip()
    except Exception:
        return ""


def _push_configuration(remote: str | None) -> tuple[tuple[str, ...], str]:
    """Git 설정 기반 push refspec과 push.default를 읽는다."""

    try:
        if remote:
            refs_command = ["git", "config", "--get-all", f"remote.{remote}.push"]
        else:
            refs_command = ["git", "config", "--get-regexp", r"^remote\..*\.push$"]
        refs_result = subprocess.run(
            refs_command,
            capture_output=True,
            text=True,
            timeout=5,
        )
        refs = []
        for line in refs_result.stdout.splitlines():
            refs.append(line.rsplit(maxsplit=1)[-1])

        mode_result = subprocess.run(
            ["git", "config", "--get", "push.default"],
            capture_output=True,
            text=True,
            timeout=5,
        )
        return tuple(refs), mode_result.stdout.strip()
    except Exception:
        return (), "unknown"


def _positional(args, with_value):
    """값을 먹는 옵션의 값까지 함께 걷어내고 위치 인자만 남긴다."""
    result, skip = [], False
    for arg in args:
        if skip:
            skip = False
            continue
        if arg in with_value:
            skip = True
            continue
        if arg.startswith("-"):
            continue
        result.append(arg)
    return result


def _has_short_flag(args, flag: str) -> bool:
    """-n, -nm 같은 단축 옵션 클러스터에서 flag 를 찾는다."""
    for arg in args:
        if arg == "--":
            break
        if not arg.startswith("-") or arg.startswith("--"):
            continue
        for char in arg[1:]:
            if char == flag:
                return True
            if char in _COMMIT_SHORT_WITH_VALUE:
                break  # 이 뒤는 옵션 값이다
    return False


def _dangerous_refspec(ref: str) -> bool:
    normalized = ref.strip()
    if normalized in (":", "+:"):
        return True
    destination = normalized.split(":")[-1].rsplit("/", 1)[-1]
    return destination in PROTECTED or "*" in destination


def _check_push(args, clause, branch_of, push_configuration_of) -> Decision:
    if any(a in ("--force", "-f") for a in args):
        return Decision(
            True,
            "force push 는 --force-with-lease 만 허용합니다. "
            f"남의 커밋을 날릴 수 있습니다 ({_DOC_FORCE}).",
            clause,
        )

    if any(a in ("--all", "--mirror") for a in args):
        return Decision(
            True,
            "--all·--mirror push 는 로컬 main·develop 까지 함께 올립니다. "
            f"올릴 브랜치를 명시하세요 ({_DOC_BRANCH}).",
            clause,
        )

    # push 뒤의 위치 인자 — 리모트 이름과 refspec 들
    refs = _positional(args, _PUSH_WITH_VALUE)

    # refspec 의 목적지 브랜치를 본다
    #   main / HEAD:develop / HEAD:refs/heads/develop / :develop (삭제)
    for ref in refs:
        if _dangerous_refspec(ref):
            return Decision(
                True,
                "main·develop 보호 브랜치나 다중 브랜치를 갱신하는 refspec은 사용할 수 없습니다. "
                "작업 브랜치 하나를 명시해 push 하세요 "
                f"({_DOC_BRANCH}).",
                clause,
            )

    # 명령행 refspec이 없으면 Git 설정(remote.*.push, push.default)을 확인한다.
    if len(refs) <= 1:
        remote = refs[0] if refs else None
        configured_refs, push_default = push_configuration_of(remote)
        if push_default in ("matching", "unknown"):
            return Decision(
                True,
                f"push.default={push_default} 설정은 대상 브랜치를 안전하게 확정할 수 없습니다. "
                f"작업 브랜치 refspec을 명시하세요 ({_DOC_BRANCH}).",
                clause,
            )
        if any(_dangerous_refspec(ref) for ref in configured_refs):
            return Decision(
                True,
                "remote.*.push 설정이 보호 브랜치나 다중 브랜치를 갱신합니다. "
                f"안전한 작업 브랜치 refspec을 명시하세요 ({_DOC_BRANCH}).",
                clause,
            )

    # 브랜치를 명시하지 않은 push — 현재 브랜치가 보호 대상이면 차단
    # (refs 가 리모트 이름 하나뿐인 경우도 포함)
    if len(refs) <= 1:
        branch = branch_of()
        if branch in PROTECTED:
            return Decision(
                True,
                f"현재 브랜치가 {branch} 입니다. 직접 푸시할 수 없습니다. "
                f"작업 브랜치를 만들어 develop 으로 PR 을 올리세요 ({_DOC_BRANCH}).",
                clause,
            )

    return ALLOWED


def _unwrap_command(tokens) -> tuple[int, dict]:
    """선행 환경변수와 command/env wrapper 뒤의 실제 명령 위치를 찾는다."""

    index, env = 0, {}
    while index < len(tokens):
        matched = _ENV_PREFIX.match(tokens[index])
        if matched:
            env[matched.group(1)] = matched.group(2)
            index += 1
            continue
        if tokens[index] == "command":
            index += 1
            while index < len(tokens) and tokens[index].startswith("-"):
                index += 1
            continue
        if tokens[index] == "env":
            index += 1
            while index < len(tokens):
                token = tokens[index]
                matched = _ENV_PREFIX.match(token)
                if matched:
                    env[matched.group(1)] = matched.group(2)
                    index += 1
                elif token in _ENV_WITH_VALUE:
                    index += 2
                elif token.startswith("-"):
                    index += 1
                else:
                    break
            continue
        break
    return index, env


def _check_commit(args, clause) -> Decision:
    if "--no-verify" in args or _has_short_flag(args, "n"):
        return Decision(
            True,
            "커밋 훅을 우회할 수 없습니다 (--no-verify·-n). "
            f"훅이 막으면 원인을 고치고, 오탐이면 사용자에게 알리세요 ({_DOC_GUARD}).",
            clause,
        )
    return ALLOWED


def inspect(
    command: str,
    branch_of=_current_branch,
    push_configuration_of=_push_configuration,
) -> Decision:
    """셸 명령 한 줄을 검사한다. 차단할 이유가 없으면 ALLOWED 를 돌려준다."""
    if not command:
        return ALLOWED

    for clause in _CLAUSE_SEPARATOR.split(command):
        clause = clause.strip()
        if not clause:
            continue

        try:
            tokens = shlex.split(clause)
        except ValueError:
            tokens = clause.split()

        index, env = _unwrap_command(tokens)
        if index >= len(tokens) or tokens[index] != "git":
            continue

        if env.get("SKIP_HOOKS", "") not in ("", "0"):
            return Decision(
                True,
                "SKIP_HOOKS 로 훅을 우회할 수 없습니다. "
                f"훅이 막으면 원인을 고치고, 오탐이면 사용자에게 알리세요 ({_DOC_GUARD}).",
                clause,
            )

        # git 전역 옵션(-C <경로>, -c <k=v>, --git-dir=… 등)을 건너뛰고 서브커맨드를 찾는다
        cursor = index + 1
        while cursor < len(tokens):
            token = tokens[cursor]
            if not token.startswith("-"):
                break
            cursor += 2 if token in _GLOBAL_WITH_VALUE else 1
        if cursor >= len(tokens):
            continue

        subcommand, args = tokens[cursor], tokens[cursor + 1 :]
        if subcommand == "push":
            decision = _check_push(args, clause, branch_of, push_configuration_of)
        elif subcommand == "commit":
            decision = _check_commit(args, clause)
        else:
            continue

        if decision.blocked:
            return decision

    return ALLOWED
