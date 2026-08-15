"""git-guard 판정 테스트.

가드는 "막는다고 문서에 적어둔 것을 실제로 막는가"가 전부인 코드다.
그래서 차단 케이스만큼 **통과 케이스**를 많이 둔다 — 과잉 차단으로 작업이 막히면
사람들이 훅을 꺼버리고, 그 순간 보호가 0이 된다.

실행: python3 -m unittest discover -s tests/harness
"""

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(ROOT / "harness"))

from hooks.core import git_guard  # noqa: E402


def inspect(command, branch="feature/1-작업", configured_refs=(), push_default=""):
    return git_guard.inspect(
        command,
        branch_of=lambda: branch,
        push_configuration_of=lambda remote: (configured_refs, push_default),
    )


class 보호브랜치푸시(unittest.TestCase):
    def test_명시적_refspec(self):
        for command in (
            "git push origin main",
            "git push origin develop",
            "git push origin HEAD:main",
            "git push origin HEAD:refs/heads/develop",
            "git push origin :develop",
        ):
            with self.subTest(command=command):
                self.assertTrue(inspect(command).blocked)

    def test_전역_옵션_뒤에_숨은_push(self):
        """git -C . push 가 '.' 을 서브커맨드로 오인해 통과하던 자리."""
        for command in (
            "git -C . push origin HEAD:main",
            "git -C /tmp/repo push origin main",
            "git -c core.hooksPath=/dev/null push origin main",
            "git --git-dir=.git push origin main",
            "git -C . -c user.name=x push origin develop",
        ):
            with self.subTest(command=command):
                self.assertTrue(inspect(command).blocked)

    def test_현재_브랜치가_보호대상(self):
        self.assertTrue(inspect("git push", branch="main").blocked)
        self.assertTrue(inspect("git push origin", branch="develop").blocked)

    def test_작업브랜치에서_브랜치_미지정_push는_통과(self):
        self.assertFalse(inspect("git push", branch="feature/1-작업").blocked)

    def test_matching_refspec과_git설정_대상(self):
        self.assertTrue(inspect("git push origin :").blocked)
        self.assertTrue(inspect("git push origin +:").blocked)
        self.assertTrue(inspect("git push", push_default="matching").blocked)
        self.assertTrue(
            inspect("git push origin", configured_refs=("HEAD:refs/heads/main",)).blocked
        )


class 위험한푸시옵션(unittest.TestCase):
    def test_force는_차단(self):
        self.assertTrue(inspect("git push --force origin feature/1-작업").blocked)
        self.assertTrue(inspect("git push -f origin feature/1-작업").blocked)

    def test_force_with_lease는_허용(self):
        """rebase 후 작업 브랜치에 필요하다. 막으면 워크플로가 성립하지 않는다."""
        self.assertFalse(inspect("git push --force-with-lease").blocked)
        self.assertFalse(
            inspect("git push --force-with-lease origin feature/1-작업").blocked
        )

    def test_all과_mirror는_차단(self):
        """작업 브랜치에 있어도 로컬 main·develop 이 함께 올라간다."""
        self.assertTrue(inspect("git push --all origin").blocked)
        self.assertTrue(inspect("git push --mirror origin").blocked)

    def test_와일드카드_refspec은_차단(self):
        self.assertTrue(inspect("git push origin refs/heads/*:refs/heads/*").blocked)


class 훅우회(unittest.TestCase):
    def test_no_verify는_차단(self):
        self.assertTrue(inspect('git commit --no-verify -m "feat: x"').blocked)

    def test_단축옵션_n은_차단(self):
        self.assertTrue(inspect('git commit -n -m "feat: x"').blocked)
        self.assertTrue(inspect('git commit -nm "feat: x"').blocked)

    def test_skip_hooks_환경변수는_차단(self):
        self.assertTrue(inspect('SKIP_HOOKS=1 git commit -m "feat: x"').blocked)
        self.assertTrue(inspect('env SKIP_HOOKS=1 git commit -m "feat: x"').blocked)

    def test_command와_env_wrapper도검사한다(self):
        self.assertTrue(inspect("command git push origin main").blocked)
        self.assertTrue(inspect("env X=1 git push origin develop").blocked)

    def test_skip_hooks가_0이면_통과(self):
        self.assertFalse(inspect('SKIP_HOOKS=0 git commit -m "feat: x"').blocked)

    def test_push의_n은_dry_run이라_통과(self):
        """commit 의 -n 과 push 의 -n 은 뜻이 다르다. 한꺼번에 막으면 오탐이다."""
        self.assertFalse(inspect("git push -n origin feature/1-작업").blocked)


class 오탐방지(unittest.TestCase):
    def test_커밋_메시지에_push나_develop이_들어가도_통과(self):
        self.assertFalse(inspect('git commit -m "feat: push 재시도 로직 추가"').blocked)
        self.assertFalse(inspect('git commit -m "fix: develop 머지 충돌 수정"').blocked)
        self.assertFalse(inspect('git commit -m "chore: main 브랜치 보호 설정"').blocked)

    def test_읽기_명령은_통과(self):
        for command in (
            "git log --oneline -3",
            "git status --short",
            "git -C . status",
            "git diff origin/develop",
            "git branch -a",
        ):
            with self.subTest(command=command):
                self.assertFalse(inspect(command).blocked)

    def test_값을_먹는_push_옵션은_refspec으로_오인하지_않는다(self):
        self.assertFalse(inspect("git push -o ci.skip origin feature/1-작업").blocked)

    def test_git이_아닌_명령은_통과(self):
        self.assertFalse(inspect("echo git push origin main").blocked)
        self.assertFalse(inspect("rg 'git push origin main' docs/").blocked)

    def test_빈_명령은_통과(self):
        self.assertFalse(inspect("").blocked)


class 여러절(unittest.TestCase):
    def test_연결된_명령의_뒤쪽_절도_검사한다(self):
        self.assertTrue(inspect("git add . && git push origin main").blocked)
        self.assertTrue(inspect("git fetch origin; git push origin develop").blocked)
        self.assertTrue(inspect("git status | git push origin main").blocked)

    def test_모든_절이_안전하면_통과(self):
        self.assertFalse(
            inspect("git add README.md && git push origin feature/1-작업").blocked
        )


class 판정메시지(unittest.TestCase):
    def test_차단_사유와_명령을_함께_돌려준다(self):
        decision = inspect("git push origin main")
        self.assertIn("main·develop", decision.reason)
        self.assertIn("CONTRIBUTING.md", decision.reason)
        self.assertEqual("git push origin main", decision.clause)
        self.assertIn("차단:", decision.message)

    def test_통과면_메시지가_비어_있다(self):
        self.assertEqual("", inspect("git status").message)


if __name__ == "__main__":
    unittest.main()
