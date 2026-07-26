# Git 작업 절차

> 정책 정본은 `CONTRIBUTING.md`다. 이 파일은 그 정책을 **실행하는 명령 절차**만 담는다.

## 브랜치

```
<타입>/<이슈번호>-<한글-제목>
```

- 타입: `feature` / `fix` / `refactor`
- 분기점은 항상 `develop`. `main`에서 따지 않는다.
- **커밋 타입은 `feat`, 브랜치 접두사는 `feature`다.** 헷갈리기 쉬운 자리다.

```bash
git fetch origin
git switch -c feature/12-카카오-소셜-로그인 origin/develop
```

이슈 번호가 없으면 브랜치를 만들지 않는다. 이슈를 먼저 만든다(`/plan`).

### develop 최신화는 rebase

```bash
git fetch origin
git rebase origin/develop
git push --force-with-lease
```

merge로 따라잡지 않는다 — PR diff에 남의 커밋이 섞인다.
`--force`가 아니라 **`--force-with-lease`**다.

## 커밋

```
<type>: <한글 요약>
```

| type | 시점 |
| --- | --- |
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `test` | 테스트 |
| `docs` | 문서 |
| `chore` | 빌드·의존성·잡무 |
| `ci` | CI/CD |
| `style` | 포맷팅 |
| `perf` | 성능 |

규칙:

- 제목은 **한글**, 마침표 없이. 명사형 또는 `~추가/수정/삭제`.
- **하나의 커밋은 하나의 목적만.** 목적이 둘이면 커밋을 나눈다.
- 본문이 필요하면 제목 다음 한 줄 띄우고, **무엇이 아니라 왜**를 쓴다.
- `git add .`로 뭉뚱그리지 않는다. 의도한 파일만 스테이징한다.
- 시크릿(`.env`, 카카오 앱 키, DB 비밀번호, JWT 시크릿)이 스테이징에 섞였는지 커밋 전에 확인한다.

```bash
git status --short
git diff --staged
git commit -m "feat: 카카오 소셜 로그인 API 추가"
```

### 커밋 훅

`.githooks/`의 훅이 커밋 시점에 검사한다. 최초 1회 설정이 필요하다.

```bash
git config core.hooksPath .githooks
```

| 훅 | 검사 |
| --- | --- |
| `pre-commit` | 보호 브랜치 커밋, 시크릿 파일·값, 충돌 마커, ktlint |
| `commit-msg` | `<type>: <한글 요약>` 형식, 마침표, 한글 여부, 길이 |

**`--no-verify`·`SKIP_HOOKS=1`로 우회하지 않는다.** 훅이 막으면 원인을 고친다.
오탐이라고 판단되면 우회하지 말고 사용자에게 알린다.

## 절대 하지 않는 것

- `main`·`develop`에 **직접 푸시** — 훅(`.claude/hooks/git-guard.sh`)이 막는다
- `main`·`develop`에 **force push**
- 리뷰 없이 병합
- 시크릿 커밋 — 실수로 올렸으면 히스토리 정리보다 **키 재발급이 먼저**다

## PR

- **항상 `--draft`로 만든다.** draft 해제는 사람이 GitHub에서 한다
- 대상 브랜치는 항상 `develop`
- 제목은 커밋 컨벤션과 같은 형식 — `feat: 카카오 소셜 로그인 API 추가`
- 본문은 `.github/PULL_REQUEST_TEMPLATE.md`를 채운다
- `Closes #<이슈번호>`로 이슈를 연결한다
- 병합은 **Squash and merge**만
- PR은 작게 유지한다. 리뷰 가능한 크기를 넘으면 나눠 올린다

```bash
gh pr create --draft --base develop --title "feat: ..." --body-file <파일>
```

`gh auth login`이 안 돼 있으면 사용자에게 안내한다 (인터랙티브라 대신 못 한다).
PR 본문은 파일로 남겨 웹에서 붙여넣을 수 있게 한다. 임의로 다른 방법을 찾지 않는다.

## 커밋·푸시 시점

사용자가 커밋·PR을 지시했다면 **메시지·본문 승인을 따로 받지 않고 진행한다.**
만든 결과는 사후 보고로 보여준다.

다만 아래는 지시받았어도 **멈추고 알린다.**

- 시크릿이 변경에 포함됨
- 현재 브랜치가 `main`·`develop`
- rebase 충돌
- 커밋 훅 실패

그리고 **`gh pr ready`·`gh pr merge`는 실행하지 않는다.** 리뷰 요청과 병합은 사람의 판단이다.
