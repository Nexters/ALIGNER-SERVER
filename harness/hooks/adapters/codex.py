"""Codex 어댑터.

⚠️ **형식 미검증.** Claude Code 의 PreToolUse 규약과 달리 Codex 의 hook 입출력 형식은
이 저장소에서 실제로 확인하지 못했다. 아래는 "JSON 판정을 stdout 으로 내고
종료 코드로도 알린다"는 보수적인 가정이다.

두 방식을 동시에 내보내는 이유는 어느 쪽 규약이든 차단이 걸리게 하기 위해서다.
JSON 을 무시하는 구현이면 종료 코드가, 종료 코드를 무시하는 구현이면 JSON 이 막는다.
실제 형식이 확인되면 emit() 만 고치면 된다 — core 와 다른 어댑터는 손대지 않는다.
"""

import json

from .base import HookAdapter

BLOCK_EXIT_CODE = 2


class CodexAdapter(HookAdapter):
    name = "codex"

    def emit(self, decision):
        if not decision.blocked:
            return json.dumps({"decision": "allow"}), "", 0
        body = json.dumps(
            {"decision": "block", "reason": decision.reason, "command": decision.clause},
            ensure_ascii=False,
        )
        return body, decision.message + "\n", BLOCK_EXIT_CODE
