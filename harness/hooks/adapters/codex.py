"""Codex PreToolUse 응답을 생성한다.

현재 권장 hookSpecificOutput 형식과 차단 종료 코드 2를 함께 사용한다.
판정 로직은 core에만 둔다.
"""

import json

from .base import HookAdapter

BLOCK_EXIT_CODE = 2


class CodexAdapter(HookAdapter):
    name = "codex"

    def emit(self, decision):
        if not decision.blocked:
            return (
                json.dumps(
                    {
                        "hookSpecificOutput": {
                            "hookEventName": "PreToolUse",
                            "permissionDecision": "allow",
                        }
                    }
                ),
                "",
                0,
            )
        body = json.dumps(
            {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "deny",
                    "permissionDecisionReason": decision.reason,
                }
            },
            ensure_ascii=False,
        )
        return body, decision.message + "\n", BLOCK_EXIT_CODE
