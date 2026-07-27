---
name: pr-feedback
description: 내 PR에 달린 리뷰 코멘트를 확인해 반영하고 답글을 남긴다. "리뷰 반영해줘", "PR 코멘트 확인해줘", "받은 리뷰 처리", 올린 PR에 피드백이 달렸을 때 사용한다.
---
<!--
  이 파일은 harness/ 에서 생성됩니다. 직접 고치지 마세요.
  고칠 곳: harness/skills/pr-feedback/SKILL.md
  다시 생성: python3 scripts/harness/generate.py
-->

# 리뷰 반영 — 받은 코멘트를 처리한다

인자로 PR 번호를 받는다. 없으면 현재 브랜치의 PR을 찾는다.

```bash
gh pr status                  # 내가 올린 PR
gh pr view --json number      # 현재 브랜치의 PR
```

## 1. 코멘트 수집 — 두 종류를 모두 가져온다

GitHub은 **PR 대화 코멘트**와 **코드 라인 코멘트**를 다른 API로 준다. 하나만 보면 놓친다.

```bash
# ① 리뷰 총평 + 대화 코멘트
gh pr view <번호> --comments

# ② 코드 라인별 리뷰 코멘트 (id·path·line 포함 — 답글에 id가 필요하다)
gh api repos/{owner}/{repo}/pulls/<번호>/comments \
  --jq '.[] | {id, path, line, user: .user.login, body, in_reply_to_id}'

# ③ 리뷰 상태 (APPROVED / CHANGES_REQUESTED)
gh api repos/{owner}/{repo}/pulls/<번호>/reviews \
  --jq '.[] | {user: .user.login, state, body}'
```

이미 답글이 달린 코멘트(`in_reply_to_id`가 있는 것)는 스레드로 묶어서 본다.

## 2. 분류

각 코멘트를 접두사로 나눈다. 접두사가 없으면 내용으로 판단하되, **애매하면 `[질문]`으로 다룬다** —
멋대로 `[필수]`로 해석해 코드를 고치는 것보다 묻는 게 낫다.

| 접두사 | 처리 |
| --- | --- |
| `[필수]` | **반드시 고친다.** 동의가 안 되면 고치지 말고 반박 근거를 답글로 |
| `[제안]` | 판단해서 반영하거나, 반영하지 않는 이유를 답글로. 무시하고 넘기지 않는다 |
| `[질문]` | 코드를 안 고치고 답변만. 답변하다 설명이 어려우면 그건 고쳐야 한다는 신호다 |

## 3. 처리 계획 제시 — 코드를 고치기 전에

표로 정리해 **사용자에게 보여주고 승인받는다.**

| # | 파일:라인 | 접두사 | 코멘트 요지 | 처리 |
| --- | --- | --- | --- | --- |
| 1 | `CourseController.kt:42` | `[필수]` | 분기가 컨트롤러에 있음 | service로 이동 |
| 2 | `CourseEntity.kt:12` | `[제안]` | 네이밍 | 반영 안 함 — §4 이름 규칙과 현재가 맞음 |

승인 없이 코드를 고치지 않는다.

## 4. 반영

- 수정은 `harness/rules/architecture.md` 체크리스트를 다시 통과해야 한다.
  리뷰 반영이 새 위반을 만드는 일이 흔하다.
- 수정 범위를 **코멘트가 지적한 곳으로 제한한다.** 김에 다른 걸 고치지 않는다.
- 검증을 다시 돌린다.

```bash
./gradlew ktlintCheck
./gradlew build
```

## 5. 커밋

리뷰 반영은 **별도 커밋**으로 남긴다. 리뷰어가 무엇이 바뀌었는지 볼 수 있어야 한다.

```
fix: 코스 처방 분기를 service 계층으로 이동

리뷰 반영 — 원인 판별이 컨트롤러에 있던 것을 CourseCommandService로 옮김
```

Squash and merge라 최종 히스토리에는 남지 않으니, 커밋을 나누는 비용이 없다.

## 6. 푸시

```bash
git push
```

rebase가 필요했다면 `git push --force-with-lease`. **`--force`는 쓰지 않는다.**

## 7. 답글

**모든 코멘트에 답한다.** 반영했든 안 했든 답이 없으면 리뷰어가 다시 확인해야 한다.

```bash
# 라인 코멘트에 스레드로 답글
gh api repos/{owner}/{repo}/pulls/<번호>/comments/<코멘트id>/replies \
  --method POST -f body='반영했습니다. CourseCommandService로 옮겼습니다. (커밋 a1b2c3d)'

# PR 전체에 요약 답글
gh pr comment <번호> --body-file <파일>
```

답글 원칙:

- 반영했으면 **커밋 해시**를 같이 적는다
- 반영하지 않았으면 **이유**를 적는다. "안 했습니다"로 끝내지 않는다
- 답글도 **한글**로 쓴다
- 답글 초안도 사용자 승인 후 등록한다

## 8. 재리뷰 요청

`CHANGES_REQUESTED`였다면 반영 후 재리뷰가 필요하다.
답글로 반영 완료를 알리는 것까지가 에이전트의 몫이다.

**`gh pr ready`·`gh pr edit --add-reviewer`를 직접 실행하지 않는다.**
draft 해제와 리뷰어 지정은 사용자가 GitHub에서 한다. 요청받으면 확인을 한 번 받고 실행한다.

## 9. 보고

처리한 코멘트 수, 반영/미반영 내역, 새 커밋, 남은 논의를 정리해 알린다.
합의가 안 된 항목이 있으면 **PR에서 끌지 말고 허들로 옮기자고 제안한다**(`CONTRIBUTING.md` §5).
