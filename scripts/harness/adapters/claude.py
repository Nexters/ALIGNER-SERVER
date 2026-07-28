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
                "network": {
                    # gh 는 Go 바이너리라 TLS 인증서 검증을 trustd 에 XPC 로 위임한다.
                    # 이걸 막으면 gh 의 모든 네트워크 호출이
                    # `x509: OSStatus -26276` 으로 죽고, gh 는 그걸 "token in keyring
                    # is invalid" 로 잘못 보고한다 (토큰·키체인은 멀쩡하다).
                    # /pr·/pr-review·/pr-feedback 이 통째로 못 돈다.
                    #
                    # 같은 증상을 enableWeakerNetworkIsolation 으로도 풀 수 있지만
                    # 그건 trustd 를 포함한 격리를 통째로 낮춘다. 필요한 서비스
                    # 하나만 연다.
                    "allowMachLookup": ["com.apple.trustd.agent"],
                },
                "filesystem": {
                    # ~/.config/gh 는 막지 않는다. gh 는 모든 서브커맨드에서 config.yml 을
                    # 먼저 읽으므로 차단하면 gh 자체가 실행 불가가 되고, 같은 파일이 자동 허용한
                    # `gh pr create` 와 /pr·/flow 스킬이 통째로 죽는다.
                    "denyRead": [
                        "~/.ssh/**",
                        "~/.aws/**",
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
