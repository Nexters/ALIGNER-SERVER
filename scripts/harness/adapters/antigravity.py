"""Antigravity 생성기 — Agent Skills 공통 경로(.agents/)도 함께 채운다.

⚠️ **형식 미검증.** codex.py 와 같다. Antigravity 를 쓰는 사람이 확인해야 한다.

  .agents/skills/<이름>/SKILL.md   Skill (Agent Skills 공통 규약)
  .agents/agents/<이름>.md         에이전트 프롬프트
  .agents/rules/<이름>.md          규칙 본문
  .agents/hooks.json               hook 연결

`.agents/skills/` 는 Codex 도 함께 읽는 공통 경로로 잡았다. Skill 을 두 벌 만들면
같은 내용이 서로 어긋나기 시작하는데, 그게 이 구조로 없애려던 문제다.
"""

import json

from . import capabilities
from .base import Adapter, json_header, markdown_header, render_frontmatter


class AntigravityAdapter(Adapter):
    name = "antigravity"
    output_root = ".agents"

    def __init__(self, warn=None):
        self.warn = warn or (lambda message: None)

    def generate(self, bundle):
        files = {}

        for skill in bundle.skills:
            meta = {
                "name": skill.name,
                "description": skill.description,
                # activation metadata — 언제 이 skill 을 걸지
                "activation": "model",
            }
            files[f"{self.output_root}/skills/{skill.name}/SKILL.md"] = (
                render_frontmatter(meta) + markdown_header(skill.source) + "\n" + skill.body
            )

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

        files[f"{self.output_root}/hooks.json"] = self._hooks(bundle)
        return files

    def _hooks(self, bundle):
        commands = bundle.policies["commands"]
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
            "permissions": {
                "allow": commands["allow"],
                "ask": commands["ask"],
                "deny": commands["deny"],
                "denyRead": bundle.policies["files"]["denyRead"],
            },
        }
        return json.dumps(body, indent=2, ensure_ascii=False) + "\n"
