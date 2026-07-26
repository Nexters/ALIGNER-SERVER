---
name: pr
description: 작업 브랜치를 푸시하고 develop 대상 Pull Request를 템플릿에 맞게 만든다. "PR 올려줘", "PR 만들어줘", "풀리퀘 작성", 커밋이 끝나 리뷰를 요청할 때 사용한다.
---

# PR 작성

`gh`가 필요하다. 없으면 (`gh: command not found`) 다음을 안내하고 본문 파일만 남긴다.

```bash
brew install gh && gh auth login
```

우회 수단을 찾지 않는다. 사용자가 웹에서 붙여넣을 수 있게 본문 파일 경로를 알려주면 된다.

## 1. 상태 확인

```bash
git branch --show-current
git log origin/develop..HEAD --oneline
git diff origin/develop...HEAD --stat
```

- 브랜치가 `main`·`develop`이면 **중단한다.** PR을 올릴 브랜치가 아니다.
- 커밋이 없으면 중단하고 알린다.
- 커밋 메시지들이 `.claude/rules/git-workflow.md` 컨벤션에 맞는지 훑는다.

## 2. develop 최신화

```bash
git fetch origin
git rebase origin/develop
```

충돌이 나면 **사용자에게 알리고 멈춘다.** 임의로 한쪽을 골라 해결하지 않는다.

## 3. 셀프 점검

아직 `/review`를 돌리지 않았다면 여기서 돌린다. 남에게 보내기 전에 본인이 먼저 본다.
특히 `build.gradle.kts`가 바뀌었으면 `architecture-reviewer`를 반드시 태운다.

## 4. 푸시

```bash
git push -u origin <브랜치명>
```

rebase 이후면 `git push --force-with-lease`. **`--force`는 쓰지 않는다.**
`main`·`develop` 푸시는 훅이 차단한다.

## 5. 본문 작성

`.github/PULL_REQUEST_TEMPLATE.md`를 읽고 그 구조를 채운다. 섹션을 임의로 바꾸지 않는다.

- **제목** — 커밋 컨벤션과 같은 형식. `feat: 카카오 소셜 로그인 API 추가`
- **관련 이슈** — `Closes #<번호>`. 이슈 번호는 브랜치명에서 가져온다
- **작업 내용** — 무엇을 왜 했는지. 커밋 목록 나열이 아니라 **리뷰어가 이해할 요약**
- **리뷰 포인트** — 집중해서 봐줬으면 하는 부분.
  `build.gradle.kts`를 바꿨으면 **어떤 의존성을 왜 `api`/`implementation`으로 골랐는지 반드시 적는다.**
  팀 리뷰의 최우선 대상이다(`CONTRIBUTING.md` §5)
- **체크리스트** — 실제로 확인한 것만 체크한다. **빌드를 안 돌렸으면 체크하지 않는다.**
  `build.gradle.kts`를 안 건드렸으면 마지막 두 항목은 지운다

본문은 **한글**로 쓴다.

### PR은 작성자가 쓴 글이다

**PR 본문의 화자는 에이전트가 아니라 작성자다.** 동료가 읽었을 때 팀원이 쓴 글로 읽혀야 한다.

- 에이전트가 무엇을 했는지 쓰지 않는다. 검증 스크립트를 돌렸다거나, 셀프 리뷰에서
  몇 건을 잡았다거나, 서브에이전트를 태웠다는 **작업 과정은 본문에 넣지 않는다.**
  리뷰어에게 필요한 건 결과다.
- 커밋 트레일러(`Co-Authored-By`, `Generated with`)를 **넣지 않는다.**
- 사람이 안 쓸 문장을 쓰지 않는다 — "~를 확인했습니다"의 반복, 과장된 강조,
  절 번호 남발. 담백하게 쓴다.

### 짧게 쓴다

**리뷰어가 5분 안에 읽을 분량이 상한이다.** 길수록 리뷰가 밀린다.

- 각 섹션은 **불릿 3~5개**, 불릿 하나는 **1~2줄**. 넘으면 문서로 뺄 내용이다.
- **판단을 리뷰어에게 미루지 않는다.** 고칠 수 있는 건 PR 올리기 전에 고친다.
  "이건 어떻게 할까요" 목록을 본문에 늘어놓는 것은 리뷰 요청이 아니라 숙제 넘기기다.
- **후속 작업은 본문에 쓰지 않는다.** 이슈로 만들고 링크만 남긴다.
  이 PR에서 하지 않은 일을 설명하는 문단이 생기면 그건 본문이 아니라 이슈다.
- 근거 문서는 **링크 한 줄**로 대신한다. 결정의 배경을 본문에서 다시 설명하지 않는다 —
  `docs/architecture.md`가 정본이다.

## 6. 생성 — 항상 draft, 사전 승인 없이

사용자가 PR을 지시했다면 **본문 승인을 따로 받지 않고 바로 만든다.** 본문은 생성 후 보고에 담는다.

```bash
gh pr create --draft --base develop --title "<제목>" --body-file <파일>
```

- **`--draft`는 필수다.** 이 저장소의 PR은 전부 draft로 열린다.
- `--base`는 항상 `develop`이다. `main`이 아니다.
- 리뷰어 지정은 사용자가 요청할 때만 (`--reviewer`).

### draft를 직접 해제하지 않는다

**`gh pr ready`를 실행하지 않는다.** 리뷰 요청 시점은 사람이 정한다 —
"이제 봐도 된다"는 판단은 작성자의 것이지 에이전트의 것이 아니다.
사용자가 명시적으로 요청해도 확인을 한 번 받는다.

### 그래도 멈추는 경우

- rebase 충돌
- 시크릿이 커밋에 포함됨
- 현재 브랜치가 `main`·`develop`
- 커밋이 없거나 이슈 번호를 알 수 없음

## 7. 보고

PR URL·번호와 **본문 전문**을 보여준다. 그리고 다음을 안내한다.

```
PR #34 (draft) https://github.com/Nexters/ALIGNER-SERVER/pull/34

리뷰를 받을 준비가 되면 GitHub에서 "Ready for review"를 눌러주세요.
```

이후 흐름:

- 리뷰 코멘트가 달리면 → `/pr-feedback <PR번호>`
- 남의 PR을 리뷰할 때는 → `/pr-review <PR번호>`
