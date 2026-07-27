#!/usr/bin/env python3
"""hook 진입점. 하네스 이름을 인자로 받아 어댑터를 고르고 core 판정을 돌린다.

    python3 harness/hooks/run.py claude < payload.json

각 하네스의 생성된 hook 설정이 이 파일을 가리킨다. 판정 로직은
harness/hooks/core/ 에만 있고, 여기서는 어댑터 선택과 종료 코드 전달만 한다.

payload 또는 가드 판정을 신뢰할 수 없으면 명령을 차단한다. 이 hook은 보조
안전장치이며 최종 경계는 하네스 네이티브 정책과 저장소 브랜치 보호다.
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
        return 2

    adapter = adapter_type()

    try:
        event = adapter.parse(sys.stdin.read())
    except Exception as error:
        decision = git_guard.Decision(True, f"hook payload 파싱 실패: {error}")
        out, err, code = adapter.emit(decision)
        sys.stdout.write(out)
        sys.stderr.write(err)
        return code

    if not event.raw:
        decision = git_guard.Decision(True, "hook payload를 해석하지 못했습니다.")
        out, err, code = adapter.emit(decision)
        sys.stdout.write(out)
        sys.stderr.write(err)
        return code
    if not event.is_shell:
        return 0
    if not event.command:
        decision = git_guard.Decision(True, "hook payload에서 실행 명령을 찾지 못했습니다.")
        out, err, code = adapter.emit(decision)
        sys.stdout.write(out)
        sys.stderr.write(err)
        return code
    try:
        decision = git_guard.inspect(event.command)
    except Exception as error:
        decision = git_guard.Decision(True, f"git 안전 검사 중 오류: {error}")

    out, err, code = adapter.emit(decision)
    if out:
        sys.stdout.write(out)
    if err:
        sys.stderr.write(err)
    return code


if __name__ == "__main__":
    sys.exit(main(sys.argv))
