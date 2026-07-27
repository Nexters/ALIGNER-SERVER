# Aligner Server

> 혼자선 왜 안 되는지 모를 때, 원하는 부위부터 잡아주는 **운동 자세 코칭 서비스**

넥스터즈 팀 프로젝트(팀명: 한두살차이)의 서버 저장소입니다.
코치 없이 혼자 요가하는 사용자에게 **"왜 안 되는지"** 원인을 짚고 보강 코스를 처방합니다.

느끼는 부위와 교정해야 하는 부위는 다릅니다. 목이 불편해도 원인은 대개 흉추·어깨이므로
코스는 원인 부위까지 포함해 편성합니다. 이 판단이 서버 로직의 핵심이고, 단순 CRUD가 아닙니다.

---

## 시작하기

### 1. 클론 후 훅 설정 — **1회 필수**

`.git/hooks`는 버전 관리가 안 되므로 훅 경로를 지정해야 합니다. **이 한 줄을 빠뜨리면
시크릿·커밋 컨벤션 검사가 전부 동작하지 않습니다.**

```bash
git config core.hooksPath .githooks
```

| 훅 | 막는 것 |
| --- | --- |
| `pre-commit` | `main`·`develop` 직접 커밋, 시크릿 파일·값, 충돌 마커 |
| `commit-msg` | `<type>: <한글 요약>` 형식, 마침표, 영문 요약, 72자 초과 |

### 2. 빌드 — **아직 없습니다**

> ⚠️ 현재 저장소에는 아키텍처 결정과 협업 규칙만 있습니다. `gradlew`가 없으므로
> 아래 명령은 **아직 실행되지 않습니다.** 빌드 기반(`build-logic`, `support-core`,
> `support-web`, `application-api`) 구축이 다음 작업입니다.

빌드 기반이 들어오면 다음 명령을 씁니다.

```bash
./gradlew build            # 빌드 + 단위 테스트
./gradlew ktlintCheck      # 린트
./gradlew integrationTest  # 통합 테스트 (Docker 필요 — TestContainers)
```

---

## 기술 스택

버전을 포함한 정본은 [AGENTS.md §4](./AGENTS.md)입니다. **임의로 올리거나 내리지 않습니다.**

| 영역 | 선택 |
| --- | --- |
| 언어 · JDK | Kotlin, Amazon Corretto 25 |
| 프레임워크 | Spring Boot |
| DB · 영속성 | PostgreSQL, **Spring Data JDBC** (JPA·QueryDSL 탈락) |
| 스키마 | Liquibase — 도메인별 changelog |
| 테스트 | Kotest `DescribeSpec` + TestContainers |
| 아키텍처 | **Hexagonal Modular Monolith**, 단일 배포 단위 |
| 인프라 | K3s 기반 HA Kubernetes (3 노드), ArgoCD |

**JPA와 QueryDSL은 쓰지 않습니다.** 이 구조에서는 `model`이 순수해야 해서 `@Entity`를 변환해
내보내게 되는데, 그 순간 더티체킹·지연로딩이 전부 무력화되고 복잡도만 남습니다.
근거는 [`docs/architecture.md` §2](./docs/architecture.md)에 있습니다.

---

## 아키텍처

도메인 하나는 서브모듈 6개로 이뤄집니다. 계층 규칙은 **컨벤션 플러그인이 강제**합니다 —
`model`에서 Spring을 import하면 클래스패스에 없어서 컴파일이 실패합니다.

```text
{domain}/
├── model/            순수 도메인 모델 + 읽기 뷰 모델 + 도메인 예외
├── infrastructure/   out-port 인터페이스만 (구현 금지)
├── service/          Command / Query 서비스
├── repository-jdbc/  port 구현 (CrudRepository / JdbcClient)
├── api/              REST Controller + DTO
└── schema/           Liquibase changelog + 마스터 데이터 시드
```

```text
api ──→ service ──→ infrastructure(port) ←── repository-jdbc
         └──→ model
```

- **도메인 간 직접 참조 금지** — `contract` / `adapter-*` 경유
- **도메인 간 DB FK 금지** — 상대 식별자만 값으로 저장
- **ComponentScan 쓰지 않음** — 각 모듈이 `@AutoConfiguration`으로 자기 Bean을 명시 등록
- **단일 DB, 도메인별 PostgreSQL schema 격리** — `course.course`, `training.session`

**코드를 놓기 전에 [`docs/architecture.md`](./docs/architecture.md)를 읽으세요.**
"이 코드는 어느 도메인의 어느 계층인가"를 먼저 정합니다. 그 문서가 코드 위치 판단의 정본입니다.

---

## 문서 지도

| 알고 싶은 것 | 문서 |
| --- | --- |
| 코드를 어느 모듈에 놓는가, 계층 규칙, Bean 조립 | [`docs/architecture.md`](./docs/architecture.md) |
| 도메인 용어집, MVP 범위, 기술 스택 버전 | [AGENTS.md](./AGENTS.md) |
| 브랜치·커밋·PR·리뷰·저장소 설정 | [CONTRIBUTING.md](./CONTRIBUTING.md) |
| 서비스 배경, 사용자 리서치 26명 인사이트 | `docs/context/user-research-insights.md` |

`CLAUDE.md`는 `AGENTS.md`의 심볼릭 링크입니다. 에이전트와 사람이 같은 문서를 읽습니다.

---

## 에이전트 하네스

Claude Code · Codex · Antigravity에서 같은 프로젝트 판단과 개발 워크플로를 쓰기 위한
설정입니다. 하네스가 프로젝트 결정을 새로 만들지는 않습니다. 프로젝트 컨텍스트는
[`AGENTS.md`](./AGENTS.md), 코드 배치 판단은 [`docs/architecture.md`](./docs/architecture.md)가
정본입니다.

### 어떻게 동작하나

```text
프로젝트 판단의 정본                     사람이 직접 수정하는 곳
┌─────────────────────────┐             ┌──────────────────────────────┐
│ AGENTS.md               │             │ harness/                     │
│ docs/architecture.md    │ ──참조──▶   │ skills/  agents/  rules/     │
└─────────────────────────┘             │ hooks/{core,policies,adapters}│
                                        └──────────────┬───────────────┘
                                                       │ generate.py
                                                       ▼
                    ┌──────────────────────────────────────────────────┐
                    │ Git 미추적 생성물 — 직접 수정 금지               │
                    ├───────────────┬───────────────┬──────────────────┤
                    │ .claude/      │ .codex/       │ .agents/         │
                    │ Claude Code   │ Codex         │ Antigravity      │
                    └───────────────┴───────────────┴──────────────────┘
                                                       │ validate.py
                                                       ▼
                              관리 대상 생성물이 원본과 같은지 검사
```

`harness/`만 직접 수정합니다. `.claude/` · `.codex/` · `.agents/`는 생성물이라 Git에 올리지
않으며, 다음 생성에서 덮어써질 수 있습니다. 생성기는 manifest에 등록한 파일만 소유하므로,
그 밖의 도구별 개인·native 설정은 보존합니다.

### 처음 사용할 때

클론 직후와 `harness/` 변경 후에는 다음을 실행합니다.

```bash
python3 scripts/harness/generate.py                 # 세 하네스 설정 생성
python3 scripts/harness/validate.py                 # 관리 대상 생성물 검증
python3 -m unittest discover -s tests/harness -v    # 생성기·가드 회귀 테스트
```

생성기는 Python 표준 라이브러리만 사용합니다. 새 checkout을 흉내 낸 임시 경로에서
`generate → validate`가 성립하는지도 테스트하므로, 로컬에 남아 있던 생성물 때문에 통과하지
않습니다.

### 사용할 수 있는 워크플로와 가드

```text
작업 요청
   │
   ▼
 plan ──▶ design ──▶ implement ──▶ review ──▶ commit ──▶ draft PR
   │         │            │             │          │
   └─────────┴────────────┴─────────────┴──────────┴── 범위·설계·필수 리뷰·검증 게이트

`flow`는 위 단계를 순서대로 묶는다.
P1 기능, 미승인 설계, [필수] 리뷰 잔존, rebase 충돌, 시크릿이 있으면 다음 단계로 가지 않는다.
```

| 단계 | 공통 스킬 | 결과 |
| --- | --- | --- |
| 기획 | `plan` | MVP 범위와 대조한 이슈 |
| 설계 | `design` | 코드 없이 계층·모듈 배치 결정 |
| 구현 | `implement` | 아키텍처 규칙에 맞는 코드·테스트 |
| 검토 | `review` | 아키텍처·코드 관점의 셀프 리뷰 |
| 협업 | `commit`, `pr`, `pr-review`, `pr-feedback` | 한글 커밋, draft PR, 리뷰 처리 |
| 전체 | `flow` | 기획부터 draft PR까지의 게이트된 순서 |

```text
AI 도구의 Git 명령 ──▶ harness/hooks/core/git_guard.py
                         └─ main·develop push / force push / 훅 우회 차단

git commit ───────────▶ .githooks/pre-commit · commit-msg
                         └─ 시크릿·충돌 마커·커밋 메시지 검사

Pull Request ─────────▶ GitHub branch protection · CI · 사람 리뷰
                         └─ 최종 병합 경계
```

Git hook은 별도 설치가 필요하므로 [시작하기](#시작하기)의 `core.hooksPath` 설정도 반드시
적용해야 합니다. 로컬 가드는 GitHub의 브랜치 보호와 리뷰를 대체하지 않고 보완합니다.

### CI가 보장하는 것

```text
하네스 변경 ──▶ harness.yml ──▶ 추적 금지 검사 → 단위·왕복 테스트 → generate → validate

모든 PR      ──▶ gradle.yml  ──▶ gradlew 없음 + 소스 없음: 초기 단계로 통과
                                  gradlew 없음 + Kotlin/Gradle 소스: 실패
                                  gradlew 있음: ktlintCheck → build → integrationTest
```

- `harness.yml` — 하네스 관련 변경에서 생성물·bytecode가 Git에 섞이지 않았는지, 생성기 테스트와
  실제 `generate → validate`가 통과하는지 검사합니다.
- `gradle.yml` — 모든 PR에서 Gradle wrapper가 존재하면 `ktlintCheck`, `build`, `integrationTest`를
  실행합니다. 현재는 Gradle 프로젝트 초기화 전이므로 명시적으로 건너뛰며, Kotlin/Gradle 파일만
  추가하고 wrapper를 빠뜨리면 실패합니다.

도구별 native 설정과 상세 운영 규칙은 [AGENTS.md §7](./AGENTS.md), 협업·브랜치 보호 규칙은
[CONTRIBUTING.md](./CONTRIBUTING.md)를 따릅니다.

---

## 협업

정본은 [CONTRIBUTING.md](./CONTRIBUTING.md)입니다. 요약하면:

- **브랜치** — `main` ← `develop` ← `feature/*` · `fix/*` · `refactor/*`.
  이름은 `<타입>/<이슈번호>-<한글-제목>`
- **커밋** — 한글, `<type>: <요약>`. 브랜치 접두사는 `feature/`, 커밋 타입은 `feat`입니다
- **PR** — 대상은 항상 `develop`, draft로 열고 작성자가 직접 해제, 병합은 Squash and merge
- **리뷰** — 하루 안에 응답. `[필수]` / `[제안]` / `[질문]`로 구분.
  **`build.gradle.kts` 의존성 추가가 최우선 리뷰 대상**입니다 — 도구가 못 막는 유일한
  아키텍처 위반 지점이라서입니다
- `develop` 최신화는 **rebase**. `main`·`develop`에 force push·직접 푸시 금지

---

## 팀

| 파트 | 인원 |
| --- | --- |
| PM | 안시언 |
| Design | 안시언, 한선영 |
| Web | 김소정, 이소흔, 권동희 |
| **Server** | **이강혁, 이동훈** |
