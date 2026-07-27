#!/usr/bin/env python3
"""hook 진입점. 하네스 이름을 인자로 받아 어댑터를 고르고 core 판정을 돌린다.

    python3 harness/hooks/run.py claude < payload.json

각 하네스의 생성된 hook 설정이 이 파일을 가리킨다. 판정 로직은
harness/hooks/core/ 에만 있고, 여기서는 어댑터 선택과 종료 코드 전달만 한다.

가드가 예외로 죽으면 조용히 통과시키지 않고 stderr 에 남긴 뒤 통과시킨다.
훅 자체의 버그로 모든 명령이 막히면 작업이 불가능해지기 때문이다.
보호가 필요한 진짜 위반은 .githooks/ 와 저장소 브랜치 보호가 한 번 더 잡는다.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from hooks.adapters.antigravity import AntigravityAdapter  # noqa: E402
from hooks.adapters.claude import ClaudeAdapter  # noqa: E402
from hooks.adapters.codex import CodexAdapter  # noqa: E402
from hooks.core import git_guard  # noqa: E402

ADAPTERS = {
    "claude": ClaudeAdapter,
    "codex": CodexAdapter,
    "antigravity": AntigravityAdapter,
}


def main(argv):
    harness = argv[1] if len(argv) > 1 else "claude"
    adapter_type = ADAPTERS.get(harness)
    if adapter_type is None:
        sys.stderr.write(
            f"git-guard: 모르는 하네스 '{harness}'. "
            f"가능한 값: {', '.join(sorted(ADAPTERS))}\n"
        )
        return 0

    adapter = adapter_type()

    try:
        event = adapter.parse(sys.stdin.read())
    except Exception as error:  # payload 가 깨졌다고 작업을 막지는 않는다
        sys.stderr.write(f"git-guard: payload 파싱 실패 ({error}). 검사를 건너뜁니다.\n")
        return 0

    if not event.is_shell:
        return 0

    try:
        decision = git_guard.inspect(event.command)
    except Exception as error:
        sys.stderr.write(f"git-guard: 검사 중 오류 ({error}). 검사를 건너뜁니다.\n")
        return 0

    out, err, code = adapter.emit(decision)
    if out:
        sys.stdout.write(out)
    if err:
        sys.stderr.write(err)
    return code


if __name__ == "__main__":
    sys.exit(main(sys.argv))
