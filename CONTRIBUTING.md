# 기여 가이드

Aligner 서버 저장소의 협업 규칙입니다. 프로젝트 배경과 도메인 용어는 [AGENTS.md](./AGENTS.md)를 참고하세요.

---

## 1. 브랜치 전략

| 브랜치 | 역할 |
| --- | --- |
| `main` | 배포 브랜치. `develop`에서만 병합된다 |
| `develop` | 기본 통합 브랜치. 모든 작업 브랜치의 분기점이자 PR 대상 |
| `feature/*` | 기능 개발 |
| `fix/*` | 버그 수정 |
| `refactor/*` | 리팩터링 (동작 변경 없음) |

> `main` + `develop` 2개 장수 브랜치를 두므로 엄밀히는 GitHub Flow가 아닌 Git Flow의 축약형입니다.
> 팀 문서상 명칭은 "GitHub Flow"로 부르고 있으나, 실제 운영은 위 표를 따릅니다.

### 브랜치 이름 규칙

```
<타입>/<이슈번호>-<한글-제목>
```

- 이슈를 먼저 만들고, 그 번호를 브랜치명에 넣습니다.
- 제목은 한글, 공백은 `-`로 연결합니다.

```
feature/1-프로젝트-초기-설정-및-에이전트-하네스-설정
feature/12-카카오-소셜-로그인
fix/23-세션-완료-시-기록-누락
refactor/31-코스-추천-로직-분리
```

### develop 최신화

작업 중 `develop`이 앞서가면 **rebase로 따라잡습니다.**

```bash
git fetch origin
git rebase origin/develop
```

merge로 따라잡으면 병합 커밋이 쌓여서 PR diff에 남의 커밋이 섞입니다. Squash and merge를
쓰므로 최종 히스토리에는 영향이 없지만, **리뷰할 때 diff가 지저분해지는 게 문제**입니다.

### force push

- 자기 작업 브랜치에는 `--force-with-lease`를 씁니다. rebase 후에는 불가피합니다.
- **`main`과 `develop`에는 절대 force push하지 않습니다.** 저장소 설정으로도 막습니다 (§6).
- `--force`가 아니라 `--force-with-lease`입니다. 남의 커밋을 날리는 걸 막아줍니다.

---

## 2. 커밋 컨벤션

```
<type>: <한글 요약>
```

- 제목은 **한글**, 마침표 없이, 명사형 또는 '~추가/수정/삭제'로 끝냅니다.
- 하나의 커밋은 하나의 목적만 담습니다.
- 본문이 필요하면 제목 다음 한 줄 띄고 작성합니다.

### type 목록

| type | 사용 시점 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 코드 구조 개선 |
| `test` | 테스트 코드 추가·수정 |
| `docs` | 문서 수정 |
| `chore` | 빌드 설정, 의존성, 기타 잡무 |
| `ci` | GitHub Actions 등 CI/CD 설정 |
| `style` | 포맷팅, 세미콜론 등 코드 의미에 영향 없는 변경 |
| `perf` | 성능 개선 |

> 브랜치 접두사는 `feature/`, 커밋 타입은 `feat`입니다. 헷갈리기 쉬우니 주의하세요.

### 커밋 훅 — 클론 후 1회 설정

`.git/hooks`는 버전 관리가 안 되므로 훅을 `.githooks/`에 두고 경로를 지정합니다.
**클론한 뒤 한 번 실행하세요.**

```bash
git config core.hooksPath .githooks
```

| 훅 | 막는 것 |
| --- | --- |
| `pre-commit` | `main`·`develop` 직접 커밋, 시크릿 파일·값, 충돌 마커 |
| `commit-msg` | `<type>: <한글 요약>` 형식 위반, 마침표, 영문 요약, 72자 초과 |

훅을 우회하지 않습니다. 오탐이면 검사 패턴을 고친 뒤 다시 커밋합니다.

### 예시

```
feat: 카카오 소셜 로그인 API 추가
fix: 세션 완료 시 수행 기록이 저장되지 않던 문제 수정
docs: readme 추가
chore: ktlint 설정 추가

refactor: 코스 추천 로직을 도메인 서비스로 분리

컨트롤러에 있던 원인 판별 분기를 CoursePrescriber로 옮겼다.
스크리닝 응답 → 원인 매핑이 한 곳에 모여 감수 결과 반영이 쉬워진다.
```

---

## 3. 이슈

- 모든 작업은 **이슈에서 시작**합니다. 브랜치명에 이슈 번호가 들어가기 때문입니다.
- 템플릿 3종을 제공합니다: **기능 개발 / 버그 / 작업(설정·문서·리팩터링)**
- 가벼운 메모성 이슈는 빈 이슈로 만들어도 됩니다.

### 라벨

템플릿이 자동으로 붙이는 3개를 기본으로 씁니다. 라벨 체계를 키우지 않습니다 — 2인 팀에서
라벨 분류에 드는 시간이 얻는 것보다 큽니다.

| 라벨 | 붙는 곳 |
| --- | --- |
| `feature` | 기능 개발 템플릿 |
| `bug` | 버그 템플릿 |
| `chore` | 작업 템플릿 |

필요하면 `blocked`(다른 작업 대기), `question`(논의 필요) 정도만 손으로 답니다.

---

## 4. Pull Request

### 흐름

1. `develop`에서 작업 브랜치를 분기합니다.
2. 작업 후 `develop`으로 **draft PR**을 올립니다.
3. 리뷰를 받을 준비가 되면 **작성자가 직접 "Ready for review"**를 눌러 draft를 해제합니다.
   준비되지 않은 PR에 리뷰 알림이 가지 않게 하는 장치이고, **"이제 봐도 된다"는 판단은
   작성자만 할 수 있기 때문**입니다. 에이전트도 draft 해제는 하지 않습니다.
4. **리뷰어 1명 이상 승인** 후 병합합니다. (서버 2인 체제이므로 사실상 상호 리뷰)
5. 병합 방식은 **Squash and merge**를 기본으로 합니다. 커밋 히스토리가 깔끔하게 유지됩니다.
6. 병합된 브랜치는 삭제합니다.

### 규칙

- PR 제목은 커밋 컨벤션과 동일한 형식을 씁니다. — `feat: 카카오 소셜 로그인 API 추가`
- 본문의 `Closes #이슈번호`로 이슈를 연결합니다. 병합 시 이슈가 자동으로 닫힙니다.
- PR은 작게 유지합니다. 리뷰 가능한 크기를 넘으면 나눠 올립니다.
- CI가 통과하지 않은 PR은 병합하지 않습니다. (CI 구축 후 적용)

---

## 5. 코드 리뷰

서버는 2인이라 **모든 PR이 서로에게 갑니다.** 리뷰가 밀리면 그대로 작업이 멈추므로,
"무엇을 보는가"와 "얼마나 기다리는가"를 정해둡니다.

### 반드시 보는 것

포맷·스타일은 Ktlint가, 계층 규칙 대부분은 컨벤션 플러그인이 이미 막습니다. 도구가 못 잡는
것에 리뷰 시간을 씁니다.

1. **`build.gradle.kts`의 의존성 추가** — 가장 중요합니다.
   [`docs/architecture.md` §3 의존성 표](./docs/architecture.md)와 대조합니다.
   `implementation`/`api` 구분은 §8 표를 봅니다. **도구가 못 막는 유일한 아키텍처 위반이
   여기라서**, 빌드 파일이 바뀐 PR은 이 항목부터 봅니다.
2. **코드가 놓인 계층** — "이게 `service`에 있어야 하는가, `api`에 있어야 하는가".
   비즈니스 판단이 컨트롤러에, DB 관심사가 서비스에 새지 않았는지.
3. **도메인 용어** — [AGENTS.md 용어집](./AGENTS.md#2-도메인-용어-코드-네이밍-기준)과 다른
   이름을 새로 만들지 않았는지.
4. **테스트가 의미 있는지** — 통과만 하는 테스트인지, 깨지면 진짜 문제를 알려주는 테스트인지.

### 리뷰 코멘트

- **막는 것과 제안을 구분해서 적습니다.** 접두사를 붙이면 오해가 없습니다.
  - `[필수]` — 고쳐야 병합 가능
  - `[제안]` — 반영해도 좋고 안 해도 됨. 답만 남기면 됨
  - `[질문]` — 이해가 안 돼서 묻는 것. 답변으로 충분
- **취향 차이는 `[제안]`입니다.** `[필수]`는 버그·아키텍처 위반·용어 불일치에만 씁니다.
- 코드가 아니라 코드를 봅니다. "왜 이렇게 했어요?"보다 "이 경우엔 X가 되지 않나요?"가 낫습니다.

### 응답 시간

- **리뷰는 하루 안에** 답니다. 못 볼 상황이면 허들이나 메시지로 알립니다.
- 급하면 리뷰 요청과 함께 직접 말합니다. PR만 올려두고 기다리지 않습니다.
- `[제안]`만 남았다면 작성자 판단으로 병합해도 됩니다. 승인을 다시 기다리지 않습니다.

### 의견이 갈릴 때

되돌리기 비용으로 판단합니다. **되돌리기 쉬운 건 작성자 뜻대로 가고**(구현 방식, 네이밍
디테일), **되돌리기 비싼 건 합의하고 갑니다**(모듈 경계, DB 스키마, API 스펙, 의존성 추가).
합의가 안 되면 PR에서 끌지 말고 허들로 옮깁니다.

---

## 6. 저장소 설정

아래는 **GitHub에서 실제로 설정해야** 지켜집니다. 문서에만 있으면 안 지켜집니다.

### 브랜치 보호 — `main`, `develop`

- Require a pull request before merging (승인 1명 이상)
- Require status checks to pass — CI 구축 후 활성화
- **Do not allow force pushes**
- **Do not allow deletions**

### Pull Request 설정

- Allow **squash merging** — 기본이자 유일하게 켭니다
- Allow merge commits — **끕니다**
- Allow rebase merging — **끕니다**
- **Automatically delete head branches** — 켭니다 (병합된 브랜치 자동 삭제)

Squash 하나만 켜두면 실수로 다른 방식이 선택될 일이 없습니다.

### 기본 브랜치

`develop`으로 둡니다. PR 대상이 기본으로 `develop`이 되고, 클론했을 때 바로 작업 브랜치를
딸 수 있습니다.

---

## 7. CI

GitHub Actions는 하네스와 Gradle 검증을 분리해 실행합니다.

### 하네스

`.github/workflows/harness.yml`은 하네스 원본·생성기·테스트가 바뀐 PR에서 다음을 실행합니다.

- Python bytecode와 생성물 추적 금지
- 생성기 단위·왕복 테스트
- 깨끗한 checkout과 같은 상태에서 `generate.py` 실행 후 `validate.py` 검증

생성물은 Git에 올리지 않으므로, CI가 생성한 결과를 검증할 뿐 diff나 커밋을 요구하지 않습니다.

### Gradle

`.github/workflows/gradle.yml`은 모든 PR과 `main`·`develop` push에서 Gradle 프로젝트를 검증합니다.
현재는 `gradlew`와 Kotlin/Gradle 소스가 없는 초기 단계라 명시적으로 건너뜁니다. 이후 소스나
Gradle 파일을 추가하면서 wrapper를 누락하면 CI가 실패합니다. wrapper가 존재하면 다음을 실행합니다.

| 검사 | 명령 | 실패 시 |
| --- | --- | --- |
| 빌드·단위 테스트 | `./gradlew build` | 병합 불가 |
| 린트 | `./gradlew ktlintCheck` | 병합 불가 |
| 통합 테스트 | `./gradlew integrationTest` | 병합 불가 (Docker 필요) |

- 대상 브랜치는 모든 PR 및 `develop`, `main`입니다.
- 통합 테스트는 TestContainers를 쓰므로 러너에 Docker가 필요합니다. GitHub 호스티드
  `ubuntu-latest`에는 기본 포함돼 있습니다.
- Gradle 캐시를 걸어 시간을 줄입니다.

### Gabia 개발 환경 배포

`develop`은 운영 배포 브랜치가 아니라 클라이언트 연동용 Gabia 개발 환경의 자동 배포
트리거이기도 합니다. Gradle 검증이 성공하면 `application-api` bootJar를 GHCR 이미지로
발행하고, Gabia의 Docker Compose가 해당 커밋 SHA 이미지를 중단 배포합니다.

- 운영 배포 기준은 계속 `main`입니다. 현재 Gabia 자동 배포는 개발 환경에만 해당합니다.
- 애플리케이션 환경값은 GitHub Environment `gabia-development`의 `APPLICATION_ENV`가 소유하고,
  GitHub Actions가 배포 시 Gabia 경로의 `.env`로 전달합니다.
- GitHub Environment `gabia-development`에는 애플리케이션 설정과 Gabia SSH 접속 정보를 등록합니다.
  - Secrets: `APPLICATION_ENV`, `GABIA_HOST`, `GABIA_USER`, `GABIA_SSH_PRIVATE_KEY`, `GABIA_KNOWN_HOSTS`
  - Variables: `GABIA_SSH_PORT`, `GABIA_DEPLOY_PATH`, `DEPLOY_SMOKE_URL`
- 서버의 `GABIA_DEPLOY_PATH/.env`는 배포 시 생성하고 권한을 `600`으로 제한합니다.
- 개발 환경은 Traefik, API, PostgreSQL, Dozzle을 같은 Compose로 실행합니다. 외부에는 Traefik의 HTTP
  포트만 공개하고, `/logs` 경로로 Dozzle 실시간 컨테이너 로그 뷰어를 제공하여 프론트엔드/백엔드 개발자가
  브라우저에서 직접 로그를 확인할 수 있습니다. PostgreSQL은 `postgres-data` 볼륨에 데이터를 보존합니다.
  현재 코드에 사용처가 없는 Valkey·메시지 큐·오브젝트 스토리지는 추가하지 않습니다.
- K3s는 같은 GHCR 이미지를 tag가 아닌 SHA256 digest로 배포합니다. Server CI는 Platform에
  digest 갱신 Draft PR만 만들고 K3s에 직접 접속하지 않습니다.
- Cross-repository PR에는 `ALIGNER-PLATFORM` 한 저장소의 Contents·Pull requests 쓰기 권한만 가진
  fine-grained token을 Repository Secret `PLATFORM_REPO_TOKEN`으로 등록합니다. 없으면 기존 Gabia
  개발 배포만 계속하고 Platform PR 생성은 건너뜁니다.

CI가 없는 동안에는 **PR 체크리스트의 "로컬에서 빌드와 테스트가 통과했습니다"가 그 자리를
대신합니다.** 체크만 하고 안 돌리면 의미가 없으니 실제로 돌립니다.

---

## 8. 하지 말 것

- **시크릿을 커밋하지 않습니다.** DB 비밀번호, 카카오 앱 키, JWT 시크릿 전부 해당합니다.
  - **로컬은 `application-secret.properties`** — 루트 예시 파일을 복사해 쓰고 커밋하지 않습니다.
  - **Gabia 개발 배포는 GitHub Environment Secret** — Actions가 서버의 `.env`를 생성합니다.
  - **k3s 배포는 K8s Secret** — 운영 값은 클러스터가 소유하고, 애플리케이션은 환경변수로만 받습니다.
  - 경계는 하나입니다. **값이 어디서 오는지는 환경이 정하고, 코드는 이름만 압니다.**
    `application.yml`에는 `${DB_PASSWORD}`처럼 참조만 두고 기본값을 박지 않습니다.
  - 실수로 올렸다면 **키를 재발급하는 게 먼저입니다.** 히스토리에서 지워도 이미 노출된 값입니다.
- **`main`·`develop`에 직접 푸시하지 않습니다.** 저장소 설정으로도 막지만, 습관으로도 막습니다.
- **리뷰 없이 병합하지 않습니다.** 2인이라 급할 때 유혹이 큰데, 그때가 사고가 나는 때입니다.
- **`docs/architecture.md`의 결정을 코드에서 조용히 어기지 않습니다.** 규칙이 불편하면
  코드가 아니라 문서를 고치는 PR을 올립니다. 그게 팀의 결정을 바꾸는 방법입니다.

---

## 9. 코드 규칙

- 문서, 주석, 커밋 메시지는 **한글**로 작성합니다.
- 도메인 네이밍은 [AGENTS.md의 용어집](./AGENTS.md#2-도메인-용어-코드-네이밍-기준)을 따릅니다. 임의 영문 번역을 만들지 않습니다.
- 포맷팅은 **Ktlint**에 위임합니다. 스타일 논쟁 대신 도구를 신뢰합니다.
- 테스트는 **Kotest `DescribeSpec`**, 통합 테스트는 **TestContainers**를 사용합니다.
- **코드를 놓기 전에 [`docs/architecture.md`](./docs/architecture.md)를 봅니다.**
  "이 코드는 어느 도메인의 어느 계층인가"를 먼저 정합니다. 모듈 구조·계층 의존 규칙·
  Command/Query 분리·Bean 조립이 전부 거기에 있고, 그 문서가 코드 위치 판단의 정본입니다.

---

## 10. 문서 지도

어디를 봐야 하는지 헷갈릴 때.

| 알고 싶은 것 | 문서 |
| --- | --- |
| 처음 클론했을 때 무엇부터 하는가 | [README.md](./README.md) |
| 브랜치·커밋·PR·리뷰·저장소 설정 | 이 문서 |
| 코드를 어느 모듈에 놓는가, 계층 규칙 | [`docs/architecture.md`](./docs/architecture.md) |
| 도메인 용어, MVP 범위, 기술 스택 | [AGENTS.md](./AGENTS.md) |
| 서비스 배경, 사용자 리서치 | `docs/context/user-research-insights.md` |
