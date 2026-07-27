"""Antigravity 어댑터.

⚠️ **형식 미검증.** codex.py 와 같은 이유로 보수적 가정이다.
JSON 판정을 stdout 으로 내고 종료 코드로도 알린다.
실제 형식이 확인되면 emit() 만 고친다.
"""

import json

from .base import HookAdapter

BLOCK_EXIT_CODE = 2


class AntigravityAdapter(HookAdapter):
    name = "antigravity"

    def emit(self, decision):
        if not decision.blocked:
            return json.dumps({"permission": "allow"}), "", 0
        body = json.dumps(
            {"permission": "deny", "message": decision.reason, "command": decision.clause},
            ensure_ascii=False,
        )
        return body, decision.message + "\n", BLOCK_EXIT_CODE
