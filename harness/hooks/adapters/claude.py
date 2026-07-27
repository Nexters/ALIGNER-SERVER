"""Claude Code 어댑터.

PreToolUse 훅 규약 — 검증된 형식이다.
  입력: stdin 으로 {"tool_name": "Bash", "tool_input": {"command": "..."}}
  출력: 종료 코드 2 면 도구 호출 차단, stderr 메시지가 모델에게 전달된다
        그 외 종료 코드는 통과
"""

from .base import HookAdapter

BLOCK_EXIT_CODE = 2


class ClaudeAdapter(HookAdapter):
    name = "claude"

    def emit(self, decision):
        if not decision.blocked:
            return "", "", 0
        return "", decision.message + "\n", BLOCK_EXIT_CODE
