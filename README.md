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
| `pre-commit` | `main`·`develop` 직접 커밋, 시크릿 파일·값, 충돌 마커, ktlint 실패 |
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
| 서비스 배경, 사용자 리서치 26명 인사이트 | `docs/context/기획.md` |

`CLAUDE.md`는 `AGENTS.md`의 심볼릭 링크입니다. 에이전트와 사람이 같은 문서를 읽습니다.

---

## 에이전트 하네스

Claude Code · Codex · Antigravity에서 같은 워크플로(`/plan` `/design` `/implement`
`/review` `/commit` `/pr`)를 씁니다. 스킬·에이전트·규칙·가드의 **원본은 `harness/` 한
곳**이고, `.claude/` · `.codex/` · `.agents/`는 거기서 생성됩니다.

**생성물을 직접 고치지 마세요.** 다음 생성 때 날아갑니다.

```bash
python3 scripts/harness/generate.py          # harness/ 를 고쳤으면 실행
python3 scripts/harness/validate.py          # 원본·생성물 drift 검사
python3 -m unittest discover -s tests/harness
```

CI가 같은 검사를 돌립니다(`.github/workflows/harness.yml`). 생성기는 표준 라이브러리만
쓰므로 별도 설치가 필요 없습니다. 상세는 [AGENTS.md §7](./AGENTS.md)에 있습니다.

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
