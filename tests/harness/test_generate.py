"""생성기 테스트 — 원본 파싱, capability 매핑, 생성물 drift.

가장 중요한 것은 test_생성물이_원본과_일치한다 다. 이게 깨졌다는 건
누군가 .claude/ 를 직접 고쳤거나 harness/ 를 고치고 다시 생성하지 않았다는 뜻이다.
"""

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(ROOT / "scripts" / "harness"))

from adapters import capabilities  # noqa: E402
from adapters.base import load_bundle, parse_frontmatter, render_frontmatter  # noqa: E402
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
    def test_생성물이_원본과_일치한다(self):
        """drift 검사. 실패하면 generate.py 를 돌리고 결과를 커밋해야 한다."""
        files, roots = validate.build(warn=lambda message: None)
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


if __name__ == "__main__":
    unittest.main()
