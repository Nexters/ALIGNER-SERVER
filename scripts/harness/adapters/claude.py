"""Claude Code 생성기.

형식이 확인된 유일한 하네스다. 나머지 어댑터가 애매할 때 이쪽을 기준으로 본다.

  .claude/skills/<이름>/SKILL.md   frontmatter: name, description
  .claude/agents/<이름>.md         frontmatter: name, description, tools, model
  .claude/rules/<이름>.md          본문 그대로
  .claude/settings.json            permissions(allow/ask/deny) + hooks
  .claude/hooks/git-guard.sh       harness/hooks/run.py 를 부르는 shim
"""

import json

from . import capabilities
from .base import Adapter, markdown_header, render_frontmatter

SHIM = """#!/bin/sh
{header}
exec /usr/bin/python3 "$CLAUDE_PROJECT_DIR/harness/hooks/run.py" claude
"""


class ClaudeAdapter(Adapter):
    name = "claude"
    output_root = ".claude"

    def __init__(self, warn=None):
        self.warn = warn or (lambda message: None)

    def generate(self, bundle):
        files = {}

        for skill in bundle.skills:
            meta = {"name": skill.name, "description": skill.description}
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

        files[f"{self.output_root}/settings.json"] = self._settings(bundle)
        files[f"{self.output_root}/hooks/git-guard.sh"] = SHIM.format(
            header=markdown_to_shell(bundle)
        )
        return files

    def _settings(self, bundle):
        commands = bundle.policies["commands"]
        deny = [f"Bash({pattern})" for pattern in commands["deny"]]
        deny += [f"Read({pattern})" for pattern in bundle.policies["files"]["denyRead"]]

        hooks = []
        for entry in bundle.policies["hooks"]["preToolUse"]:
            hooks.append(
                {
                    "matcher": "Bash" if entry["match"] == "shell" else entry["match"],
                    "hooks": [
                        {
                            "type": "command",
                            "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/git-guard.sh",
                            "timeout": entry["timeout"],
                        }
                    ],
                }
            )

        settings = {
            "$schema": "https://json.schemastore.org/claude-code-settings.json",
            "_generated": [
                "이 파일은 harness/hooks/policies/permissions.json 에서 생성됩니다.",
                "직접 고치지 마세요. 다시 생성: python3 scripts/harness/generate.py",
            ],
            "permissions": {
                "allow": [f"Bash({pattern})" for pattern in commands["allow"]],
                "deny": deny,
                "ask": [f"Bash({pattern})" for pattern in commands["ask"]],
            },
            "sandbox": {
                "enabled": True,
                "failIfUnavailable": True,
                "allowUnsandboxedCommands": False,
                "filesystem": {
                    "denyRead": [
                        "~/.ssh/**",
                        "~/.aws/**",
                        "~/.config/gh/**",
                        "~/.kube/**",
                        *bundle.policies["files"]["denyRead"],
                    ],
                    "allowRead": ["."],
                },
            },
            "hooks": {"PreToolUse": hooks},
        }
        return json.dumps(settings, indent=2, ensure_ascii=False) + "\n"


def markdown_to_shell(bundle):
    """shim 상단에 붙일 셸 주석 헤더."""
    del bundle
    return (
        "# 이 파일은 harness/ 에서 생성됩니다. 직접 고치지 마세요.\n"
        "# 고칠 곳: harness/hooks/core/git_guard.py (판정),\n"
        "#          harness/hooks/adapters/claude.py (입출력)\n"
        "# 다시 생성: python3 scripts/harness/generate.py"
    )
