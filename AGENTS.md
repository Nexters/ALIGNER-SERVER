# AGENTS.md

Aligner 서버 저장소에서 작업하는 에이전트를 위한 컨텍스트 문서.
사람이 읽어도 무방하며, 프로젝트 배경은 `docs/context/`에 원본이 있다.

---

## 1. 프로젝트 개요

**Aligner** — 넥스터즈 팀 프로젝트 (팀명: 한두살차이). 8주 일정, 현재 중간발표 완료 시점.

> 혼자선 왜 안 되는지 모를 때, 원하는 부위부터 잡아주는 **운동 자세 코칭 서비스**

- **타깃**: 코치 없이 혼자 요가하는 2030 여성
  - Target A — 막 시작하는 유저: 부위·고민(거북목·허리라인)으로 진입
  - Target B — 안 되는 자세가 명확한 유저: 피크포즈(머리서기·백벤드)로 진입
- **핵심 가설**: 유저의 진짜 벽은 "방법을 몰라서"가 아니라 **"왜 안 되는지 원인을 몰라서"**다.
  원인을 짚어주는 보강 코스를 주면 혼자서도 실행하고, **도장깨기**(자세 달성)가 리텐션을 만든다.
- **차별점**: "느끼는 부위 ≠ 교정해야 하는 부위". 목이 불편해도 원인은 대개 흉추·어깨 →
  코스는 원인 부위까지 포함해 편성한다. (경쟁 앱은 '무엇을 할지' 루틴 제공에서 멈춤)
- **플랫폼**: 모바일 **웹앱** (네이티브 아님, FE는 Vercel 배포)

### 팀 구성
| 파트 | 인원 |
| --- | --- |
| PM | 안시언 |
| Design | 안시언, 한선영 |
| Web | 김소정, 이소흔, 권동희 |
| **Server** | **이강혁, 이동훈** (이 저장소) |

인프라(K8s)는 이동훈 주도.

---

## 2. 도메인 용어 (코드 네이밍 기준)

서버 코드의 클래스·테이블·API 네이밍은 이 용어집을 따른다.

| 한글 | 영문 | 설명 |
| --- | --- | --- |
| 부위 | `BodyPart` | 목 / 등·허리 / 골반·다리 / 복부. MVP는 4개 |
| 고민 유형 | `Concern` | 같은 부위 안에서 목적 분기 (거북목 / 통증 / 라인). **P1** |
| 자가 스크리닝 | `Screening` | 관찰형 1~2문항. "손등이 벽에 닿나요?"처럼 되고·안 되고가 눈에 보이는 것만 물음 |
| 원인 | `Cause` | 스크리닝 응답으로 판별된 원인 부위 (흉추 가동성, 전거근, 햄스트링 등) |
| 코스 | `Course` | 원인별 처방. 보강 운동 스텝 + 목적지 요가 자세로 구성 |
| 스텝 | `Step` | 코스 내 단계. 각 스텝에 보강 운동 세트가 붙음 |
| 보강 운동 | `Exercise` | 캣카우, 폼롤러 흉추 신전, 브리지 등 개별 동작 |
| 목표 자세 / 피크포즈 | `TargetPose` / `PeakPose` | 코스의 목적지 (낙타·코브라·머리서기·하누만 등) |
| 자세 포인트 | `PoseCheckpoint` | 자세별 핵심 포인트 3~4개. 낙타 = 무릎 너비 / 골반 전방 / 가슴 열림 / 손이 발뒤꿈치 |
| 세션 | `Session` | 코스 1회 수행 단위. 시작·완료·기록 저장 |
| 도장 | `Stamp` | 전 포인트 체크 시 획득 → 다음 목표 자세 해금 |

### 핵심 도메인 루프 (서버가 구현해야 하는 것)

```
① 부위 + 고민 선택
   ↓
② 자가 스크리닝 (관찰형 1~2문항)
   ↓
③ 응답에 따라 분기 → ④ 원인 판별
   ↓
⑤ 원인별 코스 처방 (보강 스텝 + 목적지 자세)
   ↓
⑥ 세션 수행 → 완료 기록 저장
   ↓
⑦ 자세 포인트 체크리스트 (유저 자가 체크)
   ├─ 전 포인트 통과 → 도장 획득 → 다음 자세 해금
   └─ 미달 포인트 → 원인 부위 매핑 → 다음날 코스에 보강 동작 자동 편성
```

⑦의 **미달 포인트 → 보강 동작 자동 편성**이 이 서비스의 리텐션 엔진이자
서버 로직의 핵심 차별점이다. 단순 CRUD로 축소하지 말 것.

### 스크리닝 → 원인 → 코스 매핑 예시

| 부위 | 스크리닝 문항 | 분기 |
| --- | --- | --- |
| 목 | 벽에 등·엉덩이 붙이고 만세 — 손등이 벽에 닿나요? | 안 닿음 → 흉추·어깨 가동성 / 닿는데 목 앞 당김 → 심부목굴곡근 강화 |
| 어깨 | 벽 밀 때 날개뼈가 튀어나오나요(윙잉)? | 예 → 전거근 강화 / 아니오 → 흉추·회전근개 안정 |
| 허리·코어 | 누워 무릎 세우고 — 허리 밑으로 손이 쉽게 들어가나요? | 예 → 코어·골반 안정 / 앞 숙일 때 허리부터 굽음 → 햄스트링 |
| 골반·유연성 | 앉아서 전굴 — 손끝이 무릎 위/아래? | 위 → 햄스트링 / 아래인데 런지서 앞 당김 → 고관절 굴곡근 |
| 코어·복부 | 플랭크 30초 — 허리가 처지나요? | 예 → 코어 강화 / 데드버그 시 허리 뜸 → 복횡근 조절 |

문항·매핑표는 **요가 지도자 감수 예정** → 데이터는 코드 하드코딩이 아니라 **DB 시드/마스터 데이터**로 관리해야 변경에 견딘다.

---

## 3. MVP 구현 범위 (P0)

중간발표에서 확정된 서버 MVP 기능 8개:

1. 카카오 소셜 로그인 및 회원 관리
2. 목·등·골반·복부 부위 선택 및 자가 스크리닝
3. 스크리닝 응답에 따른 보강 영역과 목표 자세 매핑
4. 사용자별 맞춤 운동 코스 제공
5. 운동 영상·수행 시간·세트·음성 가이드 정보 제공
6. 운동 세션 시작·완료 및 수행 기록 저장
7. 코스 진행도 및 완수한 자세 관리
8. 사용자 프로필 조회 및 관리

**P1(MVP 이후)로 밀린 것** — 지금 만들지 말 것:
고민 유형 1탭, 사후 사진·영상 분석, 전후 비교 아카이브, 배지·리워드,
이력 리마인드, 푸시 알림, 통증→전문가 라우팅, 외부 앱 기록 통합,
스텝별 통과 테스트·잠금해제, 근거·전문가 감수 보기

---

## 4. 기술 스택

`docs/context/Server-기술스택.md`와 중간발표 자료 기준. 버전은 2026-07-25에 지정된 값이다.

| 영역 | 선택 | 버전 |
| --- | --- | --- |
| 언어 | **Kotlin** | `2.4.10` |
| JDK | Amazon Corretto | `25` |
| 프레임워크 | **Spring Boot** (Ktor 검토 후 탈락) | `4.1.0` |
| DB | **PostgreSQL** | |
| 영속성 | **Spring Data JDBC** (JPA·QueryDSL·Exposed 탈락 — 아래 참고) | |
| 스키마 | **Liquibase** — 도메인별 changelog | |
| 인증 | Spring Security + 카카오 소셜 로그인 | |
| 테스트 | **Kotest** (`DescribeSpec`) + **TestContainers** | |
| 린트 | **Ktlint** (Detekt는 도입 안 함) | ktlint plugin `14.2.0` |
| 비동기 | 코루틴 (사용 범위 미정 — `docs/architecture.md` §11) | kotlinx-coroutines `1.11.0` |
| 빌드 | Gradle + **`build-logic`** 컨벤션 플러그인 | |
| 아키텍처 | **Hexagonal Modular Monolith**, 단일 배포 단위 | |
| 인프라 | **K3s** 기반 HA Kubernetes 클러스터 (3 노드), Ingress로 트래픽 분산 |
| CD | ArgoCD 또는 FluxCD + GitHub Actions, Helm Chart |
| FE 배포 | Vercel |

### 배포 구성
- 3개 노드 기반 K3s HA Control Plane
- Spring Boot API 서버를 **2개 이상의 Pod로 이중화** → 무상태(stateless) 설계 필수
- 특정 노드 장애 시 다른 노드에서 서비스 지속

> ⚠️ **JPA와 QueryDSL은 쓰지 않는다.** 2026-07-25에 Spring Data JDBC로 확정됐다. 이 구조에서는
> `model`이 순수해야 하므로 `@Entity`를 변환해 내보내게 되는데, 그 순간 JPA의 더티체킹·지연로딩이
> 전부 무력화되고 복잡도만 남는다. QueryDSL도 함께 탈락했다 (kapt 의존, KSP 미지원).
> 근거는 `docs/architecture.md` §2. **이전 문서의 QueryDSL groupId 주의사항은 무효다.**

### 아키텍처 — 확정 (2026-07-25)

**Hexagonal Modular Monolith.** 상세는 **[`docs/architecture.md`](./docs/architecture.md)가
정본**이다. 모듈 골격을 만들거나 코드를 놓을 위치를 정할 때 반드시 먼저 읽는다.

- **단일 배포 단위** — 루트 `application-api` 하나만 실행 가능. 도메인별 application 모듈 없음
- **단일 DB** — PostgreSQL 한 인스턴스, **도메인별 PostgreSQL schema로 격리**
  (`course.course`, `training.session`처럼 schema-qualified)
- **도메인 서브모듈 6개** — `model` `infrastructure` `service` `repository-jdbc` `api` `schema`.
  도메인 예외는 `model/exception/` 패키지에 둔다(별도 모듈 아님 — 예외만 쓰고 모델은 안 쓰는
  소비자가 없어서 분리 이득이 0이다). 도메인 간 연결이 생길 때만 `contract` / `adapter-*` 추가.
  `member`에서 카카오 로그인을 구현하면 인증 전용 `adapter-auth` 추가
- **`support-core` / `support-web`** — 루트 레벨 공유 모듈. `support-core`는 `BaseException`,
  `ErrorCode` 같은 공통 예외 계약만 담고, `support-web`은 `AlignerPrincipal`,
  SecurityFilterChain, 공통 예외 핸들러를 담는다. **도메인이 아니므로 비즈니스 로직을 넣지 않는다**
- **Command/Query 분리** — `service`와 out-port 두 계층 모두. 쓰기는 애그리거트 단위
  `CrudRepository`, 읽기는 `JdbcClient` + 조인 SQL → 뷰 모델 직결
- **ComponentScan 쓰지 않음** — 각 모듈이 `@AutoConfiguration` + `AutoConfiguration.imports`로
  자기 Bean을 명시 등록. 실행 모듈도 `@SpringBootApplication` 대신
  `@SpringBootConfiguration` + `@EnableAutoConfiguration`. 조립을 Gradle 의존성이 결정한다
- **`build-logic` 컨벤션 플러그인**이 계층 규칙을 강제 — `model`에 Spring을 import하면
  클래스패스에 없어서 컴파일이 실패한다. 전이 누출은 `implementation`/`api` 구분이 막고,
  나머지는 리뷰로 잡는다 (검증 태스크는 위반이 실제로 나오면 만든다)

`docs/context/Server-기술스택.md`의 "모듈별 배포 단위 + gRPC"는 **폐기됐다.** 원본 기록으로만 남긴다.

k8s 학습 목표와 충돌하지 않는다. 단일 배포 단위여도 Pod 이중화·HA 컨트롤 플레인·ArgoCD
파이프라인은 그대로 구축한다.

### 모듈 규칙
- 도메인 간 **직접 참조 금지** — `contract` / `adapter-*` 경유 (`docs/architecture.md` §7)
- 도메인 간 **DB FK 금지** — 상대 식별자만 값으로 저장하고 존재 검증은 port로
- `api` → `repository-jdbc` 직접 참조 금지 (`service` 경유)
- `service` → `CrudRepository`·`JdbcClient` 직접 참조 금지 (port 경유)
- 통합 테스트 작성 (TestContainers)

### 프로젝트 목표 (기술적)
1. MVP에 대응하는 올바른 동작을 제공하는 서버 구축
2. **k8s 생태계 구축·운영 학습** — 인프라 선택은 학습 목적이 명시적으로 포함되어 있다.
   "이 규모엔 과하다"는 지적은 이미 팀이 감수한 트레이드오프이므로 반복하지 말 것.

---

## 5. 협업 규칙

전체 규칙은 **[CONTRIBUTING.md](./CONTRIBUTING.md)**가 정본이다. 요약하면:

- **브랜치**: `main`(배포) ← `develop`(통합) ← `feature/*` · `fix/*` · `refactor/*`
  - 이름 규칙: `<타입>/<이슈번호>-<한글-제목>` (예: `feature/12-카카오-소셜-로그인`)
  - PR 대상은 항상 `develop`, 병합은 Squash and merge
- **커밋**: 한글, `<type>: <요약>` (`feat` `fix` `refactor` `test` `docs` `chore` `ci` `style` `perf`)
  - 브랜치 접두사는 `feature/`, 커밋 타입은 `feat` — 다르다
- **이슈**: 모든 작업은 이슈에서 시작. 템플릿 3종(기능/버그/작업)이 `.github/ISSUE_TEMPLATE/`에 있다
- **리뷰**: 하루 안에 응답. 코멘트는 `[필수]` / `[제안]` / `[질문]`로 구분한다.
  **`build.gradle.kts` 의존성 추가가 최우선 리뷰 대상** — 도구가 못 막는 아키텍처 위반이 거기다
- **`develop` 최신화는 rebase.** `main`·`develop`에 force push·직접 푸시 금지
- **병합은 Squash and merge만** 켠다. 병합된 브랜치는 자동 삭제
- **저장소**: https://github.com/Nexters/ALIGNER-SERVER
- 회의: 온라인 / 필요시 허들

---

## 6. 에이전트 작업 시 주의사항

- **문서·코드 주석·커밋 메시지는 한글**로 작성한다. 팀 전원이 한국어 사용자다.
- 도메인 네이밍은 §2 용어집을 따른다. 임의 영문 번역을 만들지 말 것.
- P1 기능을 선제적으로 구현하지 않는다. MVP 범위(§3)가 곧 마감이다.
- 스크리닝 문항·원인 매핑·자세 포인트는 **감수 전 데이터**다. 로직에 하드코딩하지 말고
  마스터 데이터로 분리해 변경 가능하게 둔다.
- 이 프로젝트에는 아직 코드가 없다. **코드를 놓기 전에 `docs/architecture.md`를 읽는다.**
  "이 코드는 어느 도메인의 어느 계층인가"를 먼저 정하고 쓴다.
- 버전은 §4 표를 따른다. 임의로 올리거나 내리지 말 것.
- **도메인 분할은 아직 정해지지 않았다.** 아키텍처 골격(§4)만 확정된 상태다.

## 7. 에이전트 하네스

이 문서가 **에이전트 컨텍스트 정본**이다. Codex와 Antigravity는 `AGENTS.md`를 그대로 읽고,
`CLAUDE.md`는 이 파일을 가리키는 심볼릭 링크다. **도구별 사본을 만들지 않는다.**

하네스는 **기획 → 설계 → 구현 → 리뷰 → 커밋 → PR** 전 과정을 덮는다.

### 원본은 `harness/`, `.claude/`·`.codex/`·`.agents/`는 생성물이다

팀이 Claude Code·Codex·Antigravity를 섞어 쓰기 때문에, 스킬 하나를 고칠 때 세 곳을
손으로 맞추면 반드시 어긋난다. 그래서 **사람이 고치는 원본은 `harness/` 한 곳**이고
각 하네스 설정은 거기서 생성한다.

```text
harness/                    ← 여기만 고친다
├── skills/                 스킬 본문
├── agents/                 서브에이전트 프롬프트
├── rules/                  공유 규칙
└── hooks/
    ├── core/               판정 로직 (하네스 무관)
    ├── policies/           허용·확인·차단 명령 정책
    └── adapters/           하네스별 hook 입출력 변환

scripts/harness/            생성기
├── generate.py             원본 → 각 하네스 설정
├── validate.py             원본·생성물 drift 검사
└── adapters/               하네스별 생성 규칙

.claude/ .codex/ .agents/   ← 생성물. 직접 고치면 다음 생성 때 날아간다
```

```bash
python3 scripts/harness/generate.py    # 원본을 고쳤으면 반드시 실행
python3 scripts/harness/validate.py    # 어긋났는지 검사 (CI도 같은 명령)
python3 -m unittest discover -s tests/harness
```

생성물에는 "직접 고치지 마세요" 헤더가 붙고, CI(`.github/workflows/harness.yml`)가
drift를 막는다. 생성기는 **표준 라이브러리만 쓴다** — 의존성 설치 단계가 없다.

`.codex/`와 `.agents/`의 설정 형식은 **아직 미검증이다.** 해당 도구를 쓰는 사람이
확인해야 하고, 형식이 틀렸다면 `scripts/harness/adapters/`의 해당 파일만 고치면 된다.

### 스킬 — `/이름`으로 부르거나, 상황에 맞으면 자동으로 걸린다

| 단계 | 스킬 | 하는 일 |
| --- | --- | --- |
| 기획 | `/plan` | 요구사항을 MVP(§3) 범위와 대조하고 GitHub 이슈로 만든다 |
| 설계 | `/design` | 어느 도메인의 어느 계층에 무엇을 놓을지 설계서를 낸다 (코드는 안 쓴다) |
| 구현 | `/implement` | 브랜치를 파고 아키텍처 규칙대로 코드·테스트를 쓴다 |
| 리뷰 | `/review` | 커밋 전 로컬 변경을 셀프 리뷰한다 |
| 커밋 | `/commit` | 한글 커밋 컨벤션으로 목적 단위 커밋 |
| PR | `/pr` | 푸시 + 템플릿 채운 **draft PR** 생성 (대상은 항상 `develop`) |
| PR 리뷰 | `/pr-review` | 올라온 PR을 검토하고 `[필수]`/`[제안]`/`[질문]` 코멘트를 남긴다 |
| 리뷰 반영 | `/pr-feedback` | 내 PR에 달린 코멘트를 확인해 반영하고 답글까지 단다 |
| 전체 | `/flow` | 위를 게이트를 걸고 순서대로 끝까지 진행한다 |

`/flow`는 단계마다 멈춘다 — P1 판정, 새 도메인 필요, 설계 미승인, `[필수]` 잔존, rebase 충돌,
시크릿 포함이면 다음으로 넘어가지 않는다.

**커밋과 PR 생성은 사전 승인 없이 진행한다.** 지시하면 메시지·본문 확인을 거치지 않고
만들고, 결과를 사후 보고한다. 안전장치는 승인이 아니라 훅과 게이트다.

**PR은 항상 draft로 열리고, 에이전트는 draft를 해제하지 않는다.**
"이제 리뷰받아도 된다"는 판단은 작성자의 것이다 (`CONTRIBUTING.md` §4).
병합도 하지 않는다 — draft PR 생성이 종착점이다.

### 서브에이전트

| 에이전트 | 쓰는 때 |
| --- | --- |
| `architecture-reviewer` | 계층·의존 방향·`build.gradle.kts`·Bean 조립·스키마 규칙 점검 |
| `code-reviewer` | 버그·엣지 케이스·테스트 품질·도메인 용어·시크릿 |
| `context-researcher` | `docs/context/`·중간발표 PDF에서 기획 근거 조사 |
| `module-scaffolder` | 새 도메인 6모듈 골격 생성 (`docs/architecture.md` §10 11단계) |
| `build-verifier` | Gradle 빌드·ktlint·테스트 실행과 실패 원인 분석 |

리뷰는 `architecture-reviewer`와 `code-reviewer`를 **병렬로** 돌린다. 보는 것이 다르다.

### 공유 규칙 — `harness/rules/`

| 파일 | 내용 |
| --- | --- |
| `architecture.md` | 코드 배치 판단과 위반 체크리스트 (정본은 `docs/architecture.md`) |
| `git-workflow.md` | 브랜치·커밋·PR 실행 절차 (정본은 `CONTRIBUTING.md`) |
| `review.md` | 리뷰 우선순위와 `[필수]`/`[제안]`/`[질문]` 기준 |

**규칙 파일은 결정을 복제하지 않는다.** 값·표·버전이 필요하면 정본 문서의 해당 절을 읽는다.
둘이 어긋나면 정본이 이긴다.

### 가드

승인을 걷어낸 만큼 되돌리기 비싼 사고는 훅이 막는다. 3중이다.

| 층 | 파일 | 막는 것 |
| --- | --- | --- |
| 도구 호출 | `harness/hooks/core/git_guard.py` | `main`·`develop` 푸시, `--force`·`--all`·`--mirror` 푸시, 훅 우회(`--no-verify`·`SKIP_HOOKS=1`) |
| 커밋 | `.githooks/pre-commit` | 보호 브랜치 커밋, 시크릿 파일·값, 충돌 마커, ktlint 실패 |
| 커밋 | `.githooks/commit-msg` | `<type>: <한글 요약>` 형식·마침표·영문 요약·72자 초과 |

`.githooks/`는 **클론 후 1회 설정이 필요하다** — `git config core.hooksPath .githooks`.
`--no-verify`·`SKIP_HOOKS=1`로 우회하지 않는다 — 규칙이자 가드가 실제로 막는다.
훅이 막으면 원인을 고치고, 오탐이면 우회하지 말고 검사 패턴을 고친다.

권한 정책의 원본은 `harness/hooks/policies/permissions.json`이다
(`.claude/settings.json`은 여기서 생성된다).

- **자동 허용** — 읽기 전용 명령, `git commit`·`git push`·`git rebase`, `gh pr create`
- **확인 후 실행** — `gh pr ready`, `gh pr merge`, `gh issue create`,
  `gh pr review`·`comment`, `gh api` (팀에 보이는 outward 동작이라 남겨뒀다)
- **차단** — 시크릿 파일 읽기, `git reset --hard`, `git clean -fd`
- `.claude/settings.local.json` — 개인 설정. `.gitignore` 대상이다.

### 하네스를 고칠 때

- 프로젝트 판단 기준은 **이 문서와 `docs/architecture.md`**에만 둔다. 스킬에 복사하지 않는다.
- 반복 작업이 3단계 이상이면 스킬로 만든다. 그보다 짧으면 그냥 시킨다.
- 스킬이 하는 일이 겹치기 시작하면 합친다. 개수를 늘리는 게 목표가 아니다.

---

## 8. 참고 문서

- **`docs/architecture.md`** — 아키텍처 확정본. 모듈 구조·계층 규칙·Command/Query·Bean 조립.
  코드 위치를 판단할 때의 **정본**
- `docs/context/기획.md` — 사용자 리서치 26명 인사이트, 최종 가설, IA 트리 (원본, 가장 상세)
- `docs/context/Server-기술스택.md` — 서버 팀 기술 스택 논의 **(아키텍처 부분은 폐기됨, §4 참고)**
- `Aligner_중간_최종.pdf` — 중간발표 자료 (36p). MVP 범위·배포 구성·화면 디자인 포함
- `~/Desktop/취준/팀플렉스/hexagonal-module-sample` — **아키텍처 참조 구현** (flex-module-sample).
  헥사고날 멀티모듈, AutoConfiguration 조립, cross-domain contract/adapter 패턴의 실제 예시.
  Spring Data JDBC 기반이라 우리 스택과 그대로 맞는다. **그 저장소의**
  `.claude/rules/architecture.md`와 `docs/`에 상세 규칙이 있다 (우리 저장소의 같은 경로 파일과
  다른 문서다). 저장소 밖 로컬 경로이므로 팀원은 별도로 받아야 한다.
- flex 공개 기술 블로그 — 우리가 채택한 구조의 근거와 운영 경험
  - [1/7 컴파일이 지키는 아키텍처](https://flex.team/blog/2026/03/20/backend1) — Hexagonal
    Modular Monolith 선택 이유, 단일 런타임, 빌드가 아키텍처를 강제한다는 원칙
  - [2/7 모듈 경계를 넘는 이벤트](https://flex.team/blog/2026/03/23/backend2) — 도메인 간
    데이터 전달 (Outbox+CDC. 우리 규모엔 과하지만 문제 정의가 참고됨)
  - [AI가 읽을 수 있는 코드베이스 1/5](https://flex.team/blog/2026/05/20/backend15) — 자연어
    가이드보다 빌드 가드레일이 에이전트를 더 잘 가르친다
  - [3/5 Standalone App](https://flex.team/blog/2026/05/27/backend17) — 도메인 슬라이스 독립 실행
