"""생성기 테스트 — 원본 파싱, capability 매핑, 생성·검증 왕복."""

import json
import shutil
import sys
import tempfile
import tomllib
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(ROOT / "scripts" / "harness"))

from adapters import capabilities  # noqa: E402
from adapters.base import load_bundle, parse_frontmatter, render_frontmatter  # noqa: E402
import generate  # noqa: E402
import validate  # noqa: E402


class Frontmatter파싱(unittest.TestCase):
    def test_키와_값을_읽는다(self):
        meta, body = parse_frontmatter("---\nname: plan\ndescription: 설명\n---\n\n본문\n")
        self.assertEqual({"name": "plan", "description": "설명"}, meta)
        self.assertEqual("본문\n", body)

    def test_값에_콜론이_있어도_첫_콜론만_자른다(self):
        meta, _ = parse_frontmatter("---\ndescription: a: b: c\n---\n본문\n")
        self.assertEqual("a: b: c", meta["description"])

    def test_frontmatter가_없으면_본문_그대로(self):
        meta, body = parse_frontmatter("# 제목\n내용\n")
        self.assertEqual({}, meta)
        self.assertEqual("# 제목\n내용\n", body)

    def test_닫는_펜스가_없으면_오류(self):
        with self.assertRaises(ValueError):
            parse_frontmatter("---\nname: x\n본문만 있음\n")

    def test_왕복(self):
        meta = {"name": "plan", "description": "설명"}
        parsed, _ = parse_frontmatter(render_frontmatter(meta) + "\n본문\n")
        self.assertEqual(meta, parsed)

    def test_빈_값은_렌더링에서_빠진다(self):
        self.assertNotIn("model", render_frontmatter({"name": "x", "model": ""}))

    def test_목록은_yaml_배열로_렌더링한다(self):
        rendered = render_frontmatter({"tools": ["view_file", "grep_search"]})
        self.assertIn("tools:\n  - view_file\n  - grep_search", rendered)


class Capability매핑(unittest.TestCase):
    def test_claude_도구_이름으로_옮긴다(self):
        self.assertEqual(
            ["Read", "Grep", "Glob", "Bash"],
            capabilities.tools_for("claude", ["read", "search", "shell"]),
        )

    def test_중복은_한_번만(self):
        self.assertEqual(["Grep", "Glob"], capabilities.tools_for("claude", ["search", "search"]))

    def test_모르는_capability는_경고하고_버린다(self):
        warnings = []
        tools = capabilities.tools_for("claude", ["read", "텔레포트"], warnings.append)
        self.assertEqual(["Read"], tools)
        self.assertEqual(1, len(warnings))

    def test_모델_등급을_옮긴다(self):
        self.assertEqual("sonnet", capabilities.model_for("claude", "balanced"))
        self.assertEqual("", capabilities.model_for("claude", ""))

    def test_모르는_등급은_경고한다(self):
        warnings = []
        self.assertEqual("", capabilities.model_for("claude", "초고속", warnings.append))
        self.assertEqual(1, len(warnings))

    def test_모든_하네스가_같은_capability_집합을_다룬다(self):
        for harness, table in capabilities.TOOL_NAMES.items():
            with self.subTest(harness=harness):
                self.assertEqual(set(capabilities.CAPABILITIES), set(table))


class 원본규약(unittest.TestCase):
    def setUp(self):
        self.bundle = load_bundle(ROOT)

    def test_스킬과_에이전트가_비어있지_않다(self):
        self.assertTrue(self.bundle.skills)
        self.assertTrue(self.bundle.agents)
        self.assertTrue(self.bundle.rules)

    def test_원본_검사를_통과한다(self):
        self.assertEqual([], validate.check_sources(self.bundle))

    def test_에이전트_capability가_전부_알려진_값이다(self):
        for agent in self.bundle.agents:
            with self.subTest(agent=agent.name):
                self.assertTrue(agent.capabilities())
                for capability in agent.capabilities():
                    self.assertIn(capability, capabilities.CAPABILITIES)

    def test_스킬_이름이_디렉터리_이름과_같다(self):
        for skill in self.bundle.skills:
            with self.subTest(skill=skill.name):
                self.assertEqual(f"harness/skills/{skill.name}/SKILL.md", skill.source)


class 생성물(unittest.TestCase):
    def test_새_체크아웃에서_생성하고_검증한다(self):
        """생성물이 추적되지 않는 깨끗한 checkout에서도 generate → validate가 성립한다."""
        with tempfile.TemporaryDirectory() as directory:
            clean_root = Path(directory)
            shutil.copytree(ROOT / "harness", clean_root / "harness")

            with (
                mock.patch.object(generate, "ROOT", clean_root),
                mock.patch.object(validate, "ROOT", clean_root),
            ):
                files, roots = generate.build(warn=lambda message: None)
                generate.write(files, roots)
                problems = validate.check_generated(files, roots)

            self.assertEqual([], problems, "\n".join(problems))

    def test_생성물에_직접수정_금지_헤더가_붙는다(self):
        files, _ = validate.build(warn=lambda message: None)
        for relative, content in files.items():
            if relative.endswith(".md") or relative.endswith(".sh"):
                with self.subTest(path=relative):
                    self.assertIn("직접 고치지 마세요", content)

    def test_세_하네스_모두_생성된다(self):
        files, roots = validate.build(warn=lambda message: None)
        self.assertEqual([".claude", ".codex", ".agents"], roots)
        for root in roots:
            with self.subTest(root=root):
                self.assertTrue(any(path.startswith(root + "/") for path in files))

    def test_경고_없이_생성된다(self):
        warnings = []
        validate.build(warn=warnings.append)
        self.assertEqual([], warnings)

    def test_codex_네이티브_스키마(self):
        files, _ = validate.build(warn=lambda message: None)
        agents = {
            path: tomllib.loads(content)
            for path, content in files.items()
            if path.startswith(".codex/agents/")
        }
        self.assertTrue(agents)
        for path, agent in agents.items():
            with self.subTest(path=path):
                self.assertTrue(path.endswith(".toml"))
                self.assertTrue({"name", "description", "developer_instructions"} <= set(agent))

        config = tomllib.loads(files[".codex/config.toml"])
        self.assertNotIn("profiles", config)
        self.assertTrue(config["features"]["hooks"])

        hooks = json.loads(files[".codex/hooks.json"])
        self.assertIsInstance(hooks["description"], str)
        handler = hooks["hooks"]["PreToolUse"][0]
        self.assertEqual("Bash", handler["matcher"])
        self.assertEqual("command", handler["hooks"][0]["type"])
        self.assertIn("prefix_rule(", files[".codex/rules/aligner.rules"])

        source_skill_count = len(load_bundle(ROOT).skills)
        generated_skills = [
            path
            for path in files
            if path.startswith(".agents/skills/") and path.endswith("/SKILL.md")
        ]
        self.assertEqual(source_skill_count, len(generated_skills))
        self.assertFalse(any(path.startswith(".codex/skills/") for path in files))

    def test_antigravity_네이티브_스키마(self):
        files, _ = validate.build(warn=lambda message: None)
        agents = {
            path: content
            for path, content in files.items()
            if path.startswith(".agents/agents/")
        }
        self.assertTrue(agents)
        for path, content in agents.items():
            with self.subTest(path=path):
                self.assertIn("tools:\n  - ", content)
                self.assertRegex(content, r"\nmodel: (inherit|flash|pro)\n")
                self.assertIn("\nsubagent: true\n", content)
                self.assertIn("\nmainAgent: false\n", content)

        hooks = json.loads(files[".agents/hooks.json"])
        handler = hooks["aligner-git-guard"]["PreToolUse"][0]
        self.assertEqual("run_command", handler["matcher"])
        self.assertEqual("command", handler["hooks"][0]["type"])

    def test_claude_sandbox는_fail_closed(self):
        files, _ = validate.build(warn=lambda message: None)
        settings = json.loads(files[".claude/settings.json"])
        sandbox = settings["sandbox"]
        self.assertTrue(sandbox["enabled"])
        self.assertTrue(sandbox["failIfUnavailable"])
        self.assertFalse(sandbox["allowUnsandboxedCommands"])
        self.assertIn("~/.ssh/**", sandbox["filesystem"]["denyRead"])

    def test_생성기는_manifest밖의_파일을_보존한다(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / ".codex"
            output.mkdir()
            (output / "custom.toml").write_text("사용자 설정", encoding="utf-8")
            (output / "old.toml").write_text("이전 생성물", encoding="utf-8")
            (output / generate.MANIFEST_NAME).write_text(
                json.dumps({"files": ["old.toml"]}),
                encoding="utf-8",
            )
            files = {
                ".codex/new.toml": "새 생성물",
                f".codex/{generate.MANIFEST_NAME}": json.dumps({"files": ["new.toml"]}),
            }

            with mock.patch.object(generate, "ROOT", root):
                generate.write(files, [".codex"])

            self.assertEqual("사용자 설정", (output / "custom.toml").read_text(encoding="utf-8"))
            self.assertFalse((output / "old.toml").exists())
            self.assertTrue((output / "new.toml").exists())

    def test_생성기는_manifest_경로이탈을_거부한다(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / ".codex"
            output.mkdir()
            (output / generate.MANIFEST_NAME).write_text(
                json.dumps({"files": ["../../보존.txt"]}),
                encoding="utf-8",
            )

            with mock.patch.object(generate, "ROOT", root):
                with self.assertRaisesRegex(ValueError, "안전하지 않은 생성물 경로"):
                    generate.write({}, [".codex"])


if __name__ == "__main__":
    unittest.main()
