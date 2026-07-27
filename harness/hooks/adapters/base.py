"""하네스별 hook 입출력을 공통 형식으로 바꾸는 얇은 연결 계층.

여기서 판정 로직을 새로 구현하지 않는다. 판정은 harness/hooks/core/ 가 전부 한다.
어댑터가 하는 일은 셋뿐이다.

  1. 하네스가 준 stdin payload 에서 "실행하려는 셸 명령"을 꺼낸다
  2. core 를 호출한다
  3. core 의 판정을 하네스가 이해하는 승인·차단 응답으로 바꾼다

payload 구조는 하네스마다 다르고 버전에 따라 바뀐다. 그래서 키를 하나로 못 박지 않고
후보 경로를 순서대로 훑는다 — 못 찾으면 "검사할 명령이 없다"로 보고 통과시킨다.
가드가 조용히 죽는 것보다는 낫지만, 이 경우 보호가 걸리지 않으므로
새 하네스를 붙일 때는 tests/harness/ 에 payload 샘플을 꼭 추가한다.
"""

import json
from dataclasses import dataclass, field

# payload 에서 명령 문자열을 찾을 후보 경로. 앞에 있는 것부터 본다.
COMMAND_PATHS = (
    ("tool_input", "command"),
    ("toolInput", "command"),
    ("input", "command"),
    ("arguments", "command"),
    ("params", "command"),
    ("tool", "arguments", "command"),
    ("command",),
)

# 셸 실행 도구의 하네스별 이름. 이 목록에 없으면 검사 대상이 아니다.
SHELL_TOOL_NAMES = frozenset(
    ("bash", "shell", "run_command", "runcommand", "execute", "terminal", "exec")
)

TOOL_NAME_KEYS = ("tool_name", "toolName", "tool", "name")


@dataclass
class HookEvent:
    """하네스 payload 를 공통 형식으로 정규화한 결과."""

    command: str = ""
    tool_name: str = ""
    raw: dict = field(default_factory=dict)

    @property
    def is_shell(self) -> bool:
        """셸 실행인가. 도구 이름을 못 찾으면 명령 문자열 유무로 판단한다."""
        if not self.tool_name:
            return bool(self.command)
        return self.tool_name.strip().lower() in SHELL_TOOL_NAMES


def _dig(payload, path):
    cursor = payload
    for key in path:
        if not isinstance(cursor, dict) or key not in cursor:
            return None
        cursor = cursor[key]
    return cursor if isinstance(cursor, str) else None


def parse_event(stdin_text: str) -> HookEvent:
    """어느 하네스든 공통으로 쓰는 payload 파싱."""
    try:
        payload = json.loads(stdin_text)
    except Exception:
        return HookEvent()
    if not isinstance(payload, dict):
        return HookEvent()

    command = ""
    for path in COMMAND_PATHS:
        found = _dig(payload, path)
        if found:
            command = found
            break

    tool_name = ""
    for key in TOOL_NAME_KEYS:
        value = payload.get(key)
        if isinstance(value, str):
            tool_name = value
            break

    return HookEvent(command=command, tool_name=tool_name, raw=payload)


class HookAdapter:
    """하네스 어댑터의 공통 계약."""

    name = ""

    def parse(self, stdin_text: str) -> HookEvent:
        return parse_event(stdin_text)

    def emit(self, decision):
        """(stdout 문자열, stderr 문자열, 종료 코드) 를 돌려준다."""
        raise NotImplementedError
