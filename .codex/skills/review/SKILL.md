---
name: review
description: 커밋 전 로컬 변경사항을 PR 리뷰 관점으로 셀프 리뷰한다. "리뷰해줘", "검토해줘", "이대로 올려도 되나", 구현을 마치고 커밋·PR 전에 점검할 때 사용한다. 이미 올라간 PR을 리뷰할 때는 pr-review를 쓴다.
---
<!--
  이 파일은 harness/ 에서 생성됩니다. 직접 고치지 마세요.
  고칠 곳: harness/skills/review/SKILL.md
  다시 생성: python3 scripts/harness/generate.py
-->

# 셀프 리뷰 — 커밋 전 점검

**아직 커밋·PR 전인 로컬 변경**을 본다. 이미 올라간 남의 PR은 `/pr-review`다.

기준은 `harness/rules/review.md`, 아키텍처 체크는 `harness/rules/architecture.md`.

## 1. 범위 파악

```bash
git status --short
git diff --stat
git diff origin/develop...HEAD --stat   # 브랜치 전체를 볼 때
```

변경 파일이 많으면 **먼저 `build.gradle.kts` 변경 여부**를 확인한다. 있으면 그것부터 본다.

## 2. 병렬 리뷰 — 서브에이전트를 쓴다

두 관점을 동시에 돌린다. 컨텍스트를 아끼고 시각이 갈린다.

| 에이전트 | 보는 것 |
| --- | --- |
| `architecture-reviewer` | 계층 배치, 의존 방향, `build.gradle.kts`, Bean 등록, 스키마 규칙 |
| `code-reviewer` | 버그, 엣지 케이스, 테스트 품질, 도메인 용어, 시크릿 노출 |

각 에이전트에 **검토 대상 파일 목록과 diff 범위를 명시**해서 넘긴다.

## 3. 직접 확인하는 것

에이전트 결과를 받은 뒤, 다음은 본인이 확인한다.

- **에이전트가 서로 어긋나게 말한 부분** — 원문(`docs/architecture.md`)을 열어 판정한다.
- **컨트롤러 `@Bean` 등록 누락** — 빌드도 기동도 성공하고 호출만 404가 되는 자리다.
  `api` 모듈이 바뀌었으면 `AutoConfiguration`과 `AutoConfiguration.imports`를 눈으로 본다.
- **시크릿** — diff에 키·비밀번호·토큰이 섞였는지.
  `.env`, `application-local.yml`, `*.key`가 변경 목록에 들어갔는지.

## 4. 검증 실행

```bash
./gradlew ktlintCheck
./gradlew build
```

`gradlew`가 없으면 그 사실을 결과에 명시한다. **돌리지 않은 걸 통과했다고 쓰지 않는다.**

## 5. 결과 보고

`harness/rules/review.md`의 형식을 따른다.

1. 한 줄 총평 — 커밋 가능한가, `[필수]` 몇 건인가
2. `[필수]` → `[제안]` → `[질문]` 순, 각 항목에 `파일:라인`과 문서 절 번호 근거
3. **못 본 것** — 실행하지 못한 검증, 확인하지 못한 범위

## 6. 수정

`[필수]` 항목은 사용자 확인 후 고친다. **바로 고쳐도 되는지 먼저 묻는다** —
리뷰만 원했는데 코드가 바뀌면 곤란하다.

`[필수]`가 0건이면 `/commit`으로 넘어갈 수 있다고 안내한다.
