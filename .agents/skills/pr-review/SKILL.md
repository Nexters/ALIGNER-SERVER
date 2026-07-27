---
name: pr-review
description: GitHub에 올라온 Pull Request를 팀 리뷰 기준으로 검토하고 코멘트를 남긴다. "PR 리뷰해줘", "이 PR 봐줘", "N번 PR 검토", 동료가 올린 PR에 리뷰를 달아야 할 때 사용한다. 아직 안 올린 로컬 변경은 review를 쓴다.
activation: model
---
<!--
  이 파일은 harness/ 에서 생성됩니다. 직접 고치지 마세요.
  고칠 곳: harness/skills/pr-review/SKILL.md
  다시 생성: python3 scripts/harness/generate.py
-->

# PR 리뷰 — 올라온 PR을 검토하고 코멘트를 남긴다

인자로 PR 번호를 받는다. 없으면 열린 PR 목록을 보여주고 고르게 한다.

```bash
gh pr list --state open
```

`gh`가 없으면 `brew install gh && gh auth login`을 안내하고 멈춘다.

서버는 2인이라 **모든 PR이 서로에게 간다.** 리뷰가 밀리면 작업이 멈추므로
"무엇을 보는가"를 정해두고 그것만 본다 — 기준은 `harness/rules/review.md`.

## 1. PR 파악

```bash
gh pr view <번호>                              # 제목, 본문, 상태
gh pr diff <번호> --name-only                  # 변경 파일 목록
gh pr diff <번호>                              # 전체 diff
gh pr view <번호> --json files,additions,deletions,headRefName,baseRefName
```

먼저 확인할 것:

- **`build.gradle.kts`·`settings.gradle.kts`가 변경 목록에 있는가** → 있으면 그것부터 본다
- base가 `develop`인가 (`main`이면 `[필수]`로 지적)
- 본문에 `Closes #`가 있는가
- PR이 리뷰 가능한 크기인가. 넘으면 나눠 올리자고 `[제안]`

## 2. 로컬 체크아웃 (선택)

diff만으로 판단이 안 서면 실제 코드를 본다. 주변 코드 없이 diff만 보면 오판하기 쉽다.

```bash
gh pr checkout <번호>
```

체크아웃했다면 **리뷰가 끝난 뒤 원래 브랜치로 돌아온다.**

## 3. 병렬 검토 — 서브에이전트

| 에이전트 | 보는 것 |
| --- | --- |
| `architecture-reviewer` | 계층 배치, 의존 방향, `build.gradle.kts`, Bean 등록, 스키마 |
| `code-reviewer` | 버그, 엣지 케이스, 테스트 품질, 도메인 용어, 시크릿 |

각 에이전트에 **PR 번호와 변경 파일 목록**을 넘기고, `gh pr diff`로 직접 받아 보게 한다.

## 4. 판정

`harness/rules/review.md`의 우선순위대로 본다.

1. `build.gradle.kts` 의존성 — **도구가 못 막는 유일한 아키텍처 위반 지점**
2. 코드가 놓인 계층
3. 도메인 용어 (`AGENTS.md` §2 용어집)
4. 테스트가 의미 있는지
5. Bean 등록 누락 (컨트롤러 `@Bean`, `AutoConfiguration.imports`)
6. 하드코딩 (스크리닝 문항·원인 매핑·자세 포인트)

**취향 차이는 전부 `[제안]`이다.** `[필수]`는 버그·아키텍처 위반·용어 불일치·시크릿에만 쓴다.

## 5. 사용자 확인 — 반드시

코멘트 초안을 **전부 보여주고 승인받는다.** 승인 없이 PR에 글을 남기지 않는다.
남의 PR에 남기는 코멘트는 되돌리기 어렵고 팀에 보인다.

승인받을 때 리뷰 결론도 함께 정한다.

| 결론 | 쓰는 때 | 명령 |
| --- | --- | --- |
| Comment | `[제안]`·`[질문]`만 있음 | `--comment` |
| Approve | 지적 없음, 또는 `[제안]`만 남아 작성자 판단으로 병합 가능 | `--approve` |
| Request changes | `[필수]`가 있음 | `--request-changes` |

**Approve는 사용자가 명시적으로 승인할 때만 한다.** 기본은 Comment다.

## 6. 코멘트 등록

### 총평만 남길 때

```bash
gh pr review <번호> --comment --body-file <파일>
```

### 라인별 코멘트를 함께 남길 때

리뷰 하나에 인라인 코멘트를 묶어 보낸다. 낱개로 여러 번 보내면 알림이 쏟아진다.

```bash
gh api repos/{owner}/{repo}/pulls/<번호>/reviews \
  --method POST \
  --input <리뷰.json>
```

```json
{
  "event": "COMMENT",
  "body": "총평 한 줄. [필수] 2건입니다.",
  "comments": [
    {
      "path": "course/api/CourseController.kt",
      "line": 42,
      "side": "RIGHT",
      "body": "[필수] 원인 판별 분기가 컨트롤러에 있습니다. docs/architecture.md §3 기준으로 비즈니스 판단은 service 계층입니다. CourseCommandService로 옮기면 감수 결과가 바뀔 때 고칠 자리도 한 곳이 됩니다."
    }
  ]
}
```

- `event`는 `COMMENT` / `APPROVE` / `REQUEST_CHANGES`
- `line`은 **diff에 등장하는 라인**이어야 한다. 아니면 422가 난다.
  실패하면 인라인을 포기하고 총평 본문에 `파일:라인`을 적어 넣는다
- 본문은 **한글**로 쓴다

## 7. 보고

남긴 코멘트 수(`[필수]`/`[제안]`/`[질문]` 각각), 리뷰 결론, PR URL을 알린다.
**보지 못한 범위**(실행 못 한 빌드, 확인 못 한 파일)를 마지막에 명시한다.
