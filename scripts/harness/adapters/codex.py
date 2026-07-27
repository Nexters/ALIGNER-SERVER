"""Codex 네이티브 설정을 생성한다."""

import json
import shlex

from .base import Adapter, json_header, toml_header


def _toml_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def _command_prefix(pattern: str) -> list[str]:
    suffix = ":*"
    command = pattern[: -len(suffix)] if pattern.endswith(suffix) else pattern
    return shlex.split(command)


class CodexAdapter(Adapter):
    name = "codex"
    output_root = ".codex"

    def __init__(self, warn=None):
        self.warn = warn or (lambda message: None)

    def generate(self, bundle):
        files = {}

        for agent in bundle.agents:
            writable = bool({"edit", "write"} & set(agent.capabilities()))
            sandbox_mode = "workspace-write" if writable else "read-only"
            lines = [
                toml_header(agent.source),
                "",
                f"name = {_toml_string(agent.name)}",
                f"description = {_toml_string(agent.description)}",
                f"developer_instructions = {_toml_string(agent.body)}",
                f'sandbox_mode = "{sandbox_mode}"',
            ]
            files[f"{self.output_root}/agents/{agent.name}.toml"] = "\n".join(lines) + "\n"

        files[f"{self.output_root}/config.toml"] = self._config(bundle)
        files[f"{self.output_root}/hooks.json"] = self._hooks(bundle)
        files[f"{self.output_root}/rules/aligner.rules"] = self._rules(bundle)
        return files

    def _config(self, bundle):
        lines = [toml_header("harness/hooks/policies/permissions.json"), ""]
        lines.append('approval_policy = "on-request"')
        lines.append('sandbox_mode = "workspace-write"')
        lines.append("")
        lines.append("[features]")
        lines.append("hooks = true")
        return "\n".join(lines) + "\n"

    def _hooks(self, bundle):
        entries = []
        for entry in bundle.policies["hooks"]["preToolUse"]:
            entries.append(
                {
                    "matcher": "Bash",
                    "hooks": [
                        {
                            "type": "command",
                            "command": (
                                '/usr/bin/env python3 "$(git rev-parse --show-toplevel)/'
                                f'{entry["entrypoint"]}" {self.name}'
                            ),
                            "timeout": entry["timeout"],
                            "statusMessage": "Git 안전 정책 확인",
                        }
                    ],
                }
            )
        body = {
            "description": " ".join(json_header("harness/hooks/policies/permissions.json")),
            "hooks": {"PreToolUse": entries},
        }
        return json.dumps(body, indent=2, ensure_ascii=False) + "\n"

    def _rules(self, bundle):
        lines = [
            "# 이 파일은 harness/hooks/policies/permissions.json 에서 생성됩니다.",
            "# 직접 고치지 마세요. 다시 생성: python3 scripts/harness/generate.py",
            "",
        ]
        decisions = (("allow", "allow"), ("ask", "prompt"), ("deny", "forbidden"))
        for policy, decision in decisions:
            for command in bundle.policies["commands"][policy]:
                lines.extend(
                    [
                        "prefix_rule(",
                        f"    pattern = {json.dumps(_command_prefix(command), ensure_ascii=False)},",
                        f'    decision = "{decision}",',
                        f"    justification = {_toml_string('Aligner 저장소 공통 명령 정책')},",
                        ")",
                        "",
                    ]
                )
        return "\n".join(lines)
