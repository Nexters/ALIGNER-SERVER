"""Codex 생성기.

⚠️ **형식 미검증.** 아래 배치는 리뷰에서 제안된 구조를 그대로 따른 것이고,
Codex 가 실제로 이 경로·키를 읽는지는 확인하지 못했다. Codex 를 쓰는 사람이
검증해야 하는 파일이다. 형식이 틀렸다면 이 파일만 고치면 된다 —
원본(harness/)과 다른 하네스는 손대지 않는다.

  .codex/agents/<이름>.md   에이전트 프롬프트
  .codex/rules/<이름>.md    규칙 본문
  .codex/config.toml        모델·승인 정책
  .codex/hooks.json         hook 연결
  .agents/skills/<이름>/    Skill (Agent Skills 공통 경로 — antigravity 와 공유)

Codex 는 AGENTS.md 를 네이티브로 읽으므로 프로젝트 컨텍스트는 생성하지 않는다.
"""

import json

from . import capabilities
from .base import Adapter, json_header, markdown_header, render_frontmatter, toml_header


def _toml_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


class CodexAdapter(Adapter):
    name = "codex"
    output_root = ".codex"

    def __init__(self, warn=None):
        self.warn = warn or (lambda message: None)

    def generate(self, bundle):
        files = {}

        for agent in bundle.agents:
            tools = capabilities.tools_for(
                self.name, agent.capabilities(), lambda m: self.warn(f"{agent.source}: {m}")
            )
            meta = {
                "name": agent.name,
                "description": agent.description,
                "tools": ", ".join(tools),
                "model": capabilities.model_for(
                    self.name, agent.tier(), lambda m: self.warn(f"{agent.source}: {m}")
                ),
            }
            files[f"{self.output_root}/agents/{agent.name}.md"] = (
                render_frontmatter(meta) + markdown_header(agent.source) + "\n" + agent.body
            )

        for rule in bundle.rules:
            files[f"{self.output_root}/rules/{rule.name}.md"] = (
                markdown_header(rule.source) + "\n" + rule.body
            )

        files[f"{self.output_root}/config.toml"] = self._config(bundle)
        files[f"{self.output_root}/hooks.json"] = self._hooks(bundle)
        return files

    def _config(self, bundle):
        commands = bundle.policies["commands"]
        lines = [toml_header("harness/hooks/policies/permissions.json"), ""]
        lines.append('[profiles.aligner]')
        lines.append('model = "gpt-5-codex"')
        lines.append('approval_policy = "on-request"')
        lines.append("")
        lines.append("# 자동 허용 — 읽기 전용이거나 되돌리기 싼 명령")
        lines.append("auto_approve = [")
        lines.extend(f'  "{_toml_escape(pattern)}",' for pattern in commands["allow"])
        lines.append("]")
        lines.append("")
        lines.append("# 사용자 확인 — 팀에 보이는 outward 동작")
        lines.append("require_approval = [")
        lines.extend(f'  "{_toml_escape(pattern)}",' for pattern in commands["ask"])
        lines.append("]")
        lines.append("")
        lines.append("# 차단 — 되돌릴 수 없는 것과 시크릿")
        lines.append("deny = [")
        lines.extend(f'  "{_toml_escape(pattern)}",' for pattern in commands["deny"])
        lines.extend(
            f'  "read:{_toml_escape(pattern)}",'
            for pattern in bundle.policies["files"]["denyRead"]
        )
        lines.append("]")
        return "\n".join(lines) + "\n"

    def _hooks(self, bundle):
        entries = []
        for entry in bundle.policies["hooks"]["preToolUse"]:
            entries.append(
                {
                    "event": "preToolUse",
                    "match": entry["match"],
                    "command": ["python3", entry["entrypoint"], self.name],
                    "timeout": entry["timeout"],
                }
            )
        body = {
            "_generated": json_header("harness/hooks/policies/permissions.json"),
            "hooks": entries,
        }
        return json.dumps(body, indent=2, ensure_ascii=False) + "\n"
