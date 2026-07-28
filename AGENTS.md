# Repository Guidelines

Aligner는 코치 없이 요가하는 사용자가 **원인**을 찾아 보강 코스와 목표 자세를 수행하도록 돕는 모바일 웹 서버다. 이 파일은 루트 판단에만 쓴다. 세부 절차·예시는 정본 문서와 Skill에 둔다.

## 1. 프로젝트 개요

- MVP는 모바일 웹앱이며, 단일 배포 Spring 서버와 PostgreSQL을 사용한다.
- 핵심 루프: `BodyPart` 선택 → `Screening` 응답 → `Cause` 판별 → `Course` 처방 → `Session` 기록 → `PoseCheckpoint` 확인 → `Stamp`/다음 코스 보강.
- “느끼는 부위”가 아니라 원인 부위를 처방한다. 원인 매핑·문항·자세 포인트는 감수 전 데이터이므로 코드에 하드코딩하지 않는다.
- 배경·사용자 리서치·IA는 `docs/context/user-research-insights.md`에서 필요한 범위만 읽는다.

## 2. 도메인 용어 (코드 네이밍 기준)

다른 영문 번역을 만들지 않는다. 상세 정의가 필요하면 이 절과 `docs/context/user-research-insights.md`를 함께 확인한다.

| 한글 | 코드 이름 |
| --- | --- |
| 부위 / 자가 스크리닝 / 원인 | `BodyPart` / `Screening` / `Cause` |
| 코스 / 스텝 / 보강 운동 | `Course` / `Step` / `Exercise` |
| 목표 자세 / 자세 포인트 / 세션 / 도장 | `TargetPose` 또는 `PeakPose` / `PoseCheckpoint` / `Session` / `Stamp` |
| 고민 유형 | `Concern` — **P1**, 현재 구현하지 않는다 |

## 3. MVP 구현 범위 (P0)

P0: 카카오 로그인·회원, 부위 선택과 스크리닝, 원인·목표 자세 매핑, 맞춤 코스, 운동 정보, 세션 기록, 진행도·완수 자세, 프로필.

P1: `Concern`, 사진·영상 분석, 전후 비교, 배지·리워드, 리마인드·푸시, 통증 라우팅, 외부 앱 연동, 스텝 잠금해제, 근거·감수 보기. 요청이 P1·새 도메인·미확정 코루틴 범위에 걸리면 구현 전에 사용자에게 알린다.

## 4. 기술 스택과 아키텍처

- Kotlin 2.4.10, JDK 25, Spring Boot 4.1.0, Gradle 9.6.1, PostgreSQL, Spring Data JDBC, Liquibase, Kotest·TestContainers, ktlint. JPA·QueryDSL·Exposed는 사용하지 않는다. **버전 정본은 `gradle/libs.versions.toml`이다** — 임의로 올리거나 내리지 않는다. 코드 위치·의존성 판단의 정본은 `docs/architecture.md`다.
- **정본:** 코드 위치·모듈·Bean·스키마 판단은 작업 전에 `docs/architecture.md`를 읽는다. 이 파일은 그 결정을 복제하지 않는다.
- Hexagonal Modular Monolith: `application-api`만 실행한다. 도메인은 기본 `model`, `infrastructure`, `service`, `repository-jdbc`, `api`, `schema`로 나눈다. 도메인 연결이 실제로 필요할 때만 `contract`/`adapter-*`를 만든다.
- `api → service → infrastructure port` 방향을 지킨다. `api → repository-jdbc`, `service → CrudRepository/JdbcClient`, 도메인 간 직접 참조·DB FK는 금지다.
- Command는 애그리거트 단위 저장, Query는 `JdbcClient` 조회 모델 직결이다. schema는 도메인별 PostgreSQL schema와 Liquibase changelog로 관리한다.
- ComponentScan을 쓰지 않는다. `@AutoConfiguration`과 `AutoConfiguration.imports`로 Bean을 조립한다. `support-core`·`support-web`에는 비즈니스 로직을 넣지 않는다.

## 5. 협업 규칙

`CONTRIBUTING.md`가 브랜치·커밋·PR의 정본이다.

- 작업은 이슈에서 시작한다. `develop`에서 `feature/*`, `fix/*`, `refactor/*`를 분기하고 PR 대상은 항상 `develop`이다.
- 커밋은 한글 `<type>: <요약>`이고, PR은 draft로만 만든다. 에이전트는 병합·draft 해제를 하지 않는다.
- `main`·`develop` 직접 push/force push는 금지다. rebase 뒤 자기 작업 브랜치에는 `--force-with-lease`만 쓴다.
- `build.gradle.kts` 의존성 변경은 최우선 리뷰 대상이다. 공개 API·DB schema·CI·보안 경계 변경은 영향과 검증을 먼저 설명한다.

## 6. 에이전트 작업 규칙

- 현재 checkout·브랜치·사용자 변경을 보존한다. 요구사항 밖의 리팩터링·호환성 계층·추상화를 추가하지 않는다.
- 코드·문서·주석·커밋 메시지는 한글, 코드 식별자는 §2의 영문 용어를 쓴다.
- 구현 전에는 해당 정본을 읽고 “어느 도메인의 어느 계층인가”를 결정한다. 미확정 사항은 단정하지 않고 확인 필요로 보고한다.
- 변경한 동작에는 비례한 검증을 실행한다. 실행하지 못한 검증은 완료라고 말하지 않는다.
- 파일·경로·생성물·외부 동작을 추측하지 않는다. 실제 설정·CI·정본 문서로 확인한다.

## 7. 에이전트 하네스

하네스 원본은 `harness/`이고 `.claude/`, `.codex/`, `.agents/`는 Git 추적하지 않는 생성물이다. 생성물을 직접 고치지 않는다. clone 직후와 원본 변경 후 `generate.py`를 실행한다. `harness/`에는 Skill·Agent·Rule·hook 원본이, `scripts/harness/`에는 생성기와 도구별 adapter가 있다.

```bash
python3 scripts/harness/generate.py
python3 scripts/harness/validate.py
python3 -m unittest discover -s tests/harness
```

워크플로는 `/plan → /design → /implement → /review → /commit → /pr`이다. `/flow`는 P1, 설계 미승인, 빌드 실패, `[필수]` 잔존, rebase 충돌, 시크릿에서 멈춘다.

hook·Git hook은 보조 경계다. native sandbox·명령 정책과 GitHub branch protection을 주 경계로 유지한다. `--no-verify`·`SKIP_HOOKS=1`로 우회하지 않는다.

## 정본 지도

| 판단 | 먼저 읽을 파일 |
| --- | --- |
| 모듈·의존성·Bean·스키마 | `docs/architecture.md` |
| 브랜치·커밋·PR·리뷰 | `CONTRIBUTING.md` |
| 기획 근거·MVP 배경 | `docs/context/user-research-insights.md` |
| 하네스 원본·생성·검증 | 이 문서 §7, `harness/`, `scripts/harness/` |
