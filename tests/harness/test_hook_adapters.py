"""하네스 어댑터 테스트 — payload 를 읽고 판정을 그 하네스 형식으로 내보내는지.

새 하네스를 붙일 때 여기에 payload 샘플을 추가한다. 어댑터가 명령을 못 찾으면
가드가 조용히 통과시키므로, 이 테스트가 없으면 보호가 빠진 걸 아무도 모른다.
"""

import json
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(ROOT / "harness"))

from hooks.adapters import base  # noqa: E402
from hooks.adapters.antigravity import AntigravityAdapter  # noqa: E402
from hooks.adapters.claude import ClaudeAdapter  # noqa: E402
from hooks.adapters.codex import CodexAdapter  # noqa: E402
from hooks.core.git_guard import Decision  # noqa: E402

BLOCKED = Decision(True, "테스트 사유", "git push origin main")


class Payload파싱(unittest.TestCase):
    def test_claude_형식(self):
        event = base.parse_event(
            json.dumps({"tool_name": "Bash", "tool_input": {"command": "git status"}})
        )
        self.assertEqual("git status", event.command)
        self.assertTrue(event.is_shell)

    def test_대체_경로들(self):
        for payload in (
            {"tool_name": "shell", "arguments": {"command": "git status"}},
            {"tool": "run_command", "params": {"command": "git status"}},
            {"name": "execute", "input": {"command": "git status"}},
            {
                "toolCall": {
                    "name": "run_command",
                    "args": {"CommandLine": "git status"},
                }
            },
            {"command": "git status"},
        ):
            with self.subTest(payload=payload):
                event = base.parse_event(json.dumps(payload))
                self.assertEqual("git status", event.command)
                self.assertTrue(event.is_shell)

    def test_셸이_아닌_도구는_검사대상이_아니다(self):
        event = base.parse_event(
            json.dumps({"tool_name": "Read", "tool_input": {"file_path": "a.kt"}})
        )
        self.assertFalse(event.is_shell)

    def test_깨진_payload는_빈_이벤트(self):
        for text in ("", "not json", "[]", "null"):
            with self.subTest(text=text):
                event = base.parse_event(text)
                self.assertEqual("", event.command)
                self.assertFalse(event.is_shell)


class 응답형식(unittest.TestCase):
    def test_claude는_종료코드_2로_차단한다(self):
        out, err, code = ClaudeAdapter().emit(BLOCKED)
        self.assertEqual(2, code)
        self.assertIn("테스트 사유", err)
        self.assertEqual("", out)

    def test_claude는_통과시_0(self):
        self.assertEqual(0, ClaudeAdapter().emit(Decision(False))[2])

    def test_codex는_json과_종료코드를_함께_낸다(self):
        out, err, code = CodexAdapter().emit(BLOCKED)
        self.assertEqual(2, code)
        output = json.loads(out)["hookSpecificOutput"]
        self.assertEqual("PreToolUse", output["hookEventName"])
        self.assertEqual("deny", output["permissionDecision"])
        self.assertEqual("테스트 사유", output["permissionDecisionReason"])
        self.assertIn("테스트 사유", err)

    def test_antigravity는_json과_종료코드를_함께_낸다(self):
        out, err, code = AntigravityAdapter().emit(BLOCKED)
        self.assertEqual(2, code)
        self.assertEqual("deny", json.loads(out)["decision"])
        self.assertIn("테스트 사유", err)


class 진입점(unittest.TestCase):
    """run.py 를 실제 프로세스로 돌려 종료 코드를 확인한다."""

    def run_hook(self, harness, payload):
        return subprocess.run(
            [sys.executable, str(ROOT / "harness" / "hooks" / "run.py"), harness],
            input=json.dumps(payload),
            capture_output=True,
            text=True,
            timeout=15,
        )

    def test_세_하네스_모두_보호브랜치_push를_막는다(self):
        payloads = {
            "claude": {"tool_name": "Bash", "tool_input": {"command": "git push origin main"}},
            "codex": {"tool_name": "Bash", "tool_input": {"command": "git push origin main"}},
            "antigravity": {
                "toolCall": {
                    "name": "run_command",
                    "args": {"CommandLine": "git push origin main"},
                }
            },
        }
        for harness, payload in payloads.items():
            with self.subTest(harness=harness):
                self.assertEqual(2, self.run_hook(harness, payload).returncode)

    def test_안전한_명령은_통과한다(self):
        payloads = {
            "claude": {"tool_name": "Bash", "tool_input": {"command": "git status"}},
            "codex": {"tool_name": "Bash", "tool_input": {"command": "git status"}},
            "antigravity": {
                "toolCall": {
                    "name": "run_command",
                    "args": {"CommandLine": "git status"},
                }
            },
        }
        for harness, payload in payloads.items():
            with self.subTest(harness=harness):
                self.assertEqual(0, self.run_hook(harness, payload).returncode)

    def test_모르는_하네스는_차단한다(self):
        result = self.run_hook("없는하네스", {"command": "git push origin main"})
        self.assertEqual(2, result.returncode)
        self.assertIn("모르는 하네스", result.stderr)

    def test_깨진_payload는_차단한다(self):
        result = subprocess.run(
            [sys.executable, str(ROOT / "harness" / "hooks" / "run.py"), "claude"],
            input="}{ 깨진 json",
            capture_output=True,
            text=True,
            timeout=15,
        )
        self.assertEqual(2, result.returncode)

    def test_셸이_아닌_도구는_통과한다(self):
        result = self.run_hook(
            "claude",
            {"tool_name": "Read", "tool_input": {"file_path": "README.md"}},
        )
        self.assertEqual(0, result.returncode)


if __name__ == "__main__":
    unittest.main()
