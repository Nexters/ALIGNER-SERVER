"""Antigravity PreToolUse의 allow·deny 응답을 생성한다."""

import json

from .base import HookAdapter

BLOCK_EXIT_CODE = 2


class AntigravityAdapter(HookAdapter):
    name = "antigravity"

    def emit(self, decision):
        if not decision.blocked:
            return json.dumps({"decision": "allow"}), "", 0
        body = json.dumps(
            {"decision": "deny", "reason": decision.reason},
            ensure_ascii=False,
        )
        return body, decision.message + "\n", BLOCK_EXIT_CODE
