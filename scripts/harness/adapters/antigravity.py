"""Antigravity 네이티브 설정을 생성한다."""

import json

from . import capabilities
from .base import Adapter, markdown_header, render_frontmatter


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
                "tools": tools,
                "subagent": "true",
                "mainAgent": "false",
                "model": capabilities.model_for(
                    self.name, agent.tier(), lambda m: self.warn(f"{agent.source}: {m}")
                )
                or "inherit",
                "commandExecutionPolicy": "sandbox",
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
        entries = []
        for entry in bundle.policies["hooks"]["preToolUse"]:
            entries.append(
                {
                    "matcher": "run_command",
                    "hooks": [
                        {
                            "type": "command",
                            "command": (
                                'python3 "$(git rev-parse --show-toplevel)/'
                                f'{entry["entrypoint"]}" {self.name}'
                            ),
                            "timeout": entry["timeout"],
                        }
                    ],
                }
            )
        body = {"aligner-git-guard": {"PreToolUse": entries}}
        return json.dumps(body, indent=2, ensure_ascii=False) + "\n"
