# 아키텍처 판단 규칙

> **정본은 `docs/architecture.md`다.** 이 파일은 값을 복제하지 않고 *무엇을 확인할지*만 담는다.
> 표·버전·의존성 목록이 필요하면 항상 원문 해당 절을 읽는다. 둘이 어긋나면 원문이 이긴다.

## 코드를 놓기 전에 답해야 하는 3가지

1. **어느 도메인인가?** — 도메인 분할은 아직 미확정이다(`docs/architecture.md` §11).
   기존 도메인에 안 맞는다고 새 도메인을 임의로 만들지 말고 사용자에게 확인한다.
2. **어느 계층인가?** — `model` / `infrastructure` / `service` / `repository-jdbc` / `api` / `schema`.
   §3 계층별 역할 표에서 고른다.
3. **쓰기인가 읽기인가?** — Command/Query는 service와 out-port 양쪽에서 나뉜다(§4).
   한쪽만 필요하면 한쪽만 만든다. 세트로 찍어내지 않는다.

## 위반 체크리스트

작성한 코드와 `build.gradle.kts`를 아래로 훑는다. 하나라도 걸리면 `[필수]`다.

### 의존 방향 (§3 금지 규칙)

- [ ] `api`가 `repository-jdbc`를 직접 참조하지 않는다 (반드시 `service` 경유)
- [ ] `service`가 `CrudRepository`·`JdbcClient`를 직접 참조하지 않는다 (`infrastructure` port 경유)
- [ ] `model`·`infrastructure`에 Spring·JDBC 타입이 없다
- [ ] `service` 시그니처에 `Authentication`·`Principal` 등 Security 타입이 없다 (§9)
- [ ] 도메인 간 직접 참조가 없다 — `contract` / `adapter-*` 경유 (§7)
- [ ] `QueryService`가 `CommandService`를 주입받지 않는다 (§4)
- [ ] `support-core`·`support-web`에 비즈니스 로직이 없다 (§9)

### build.gradle.kts — 리뷰의 최우선 대상

컨벤션 플러그인과 `implementation`/`api` 규율이 못 막는 유일한 위반 지점이다(§8).

- [ ] 추가한 의존성이 §3 "계층별 역할과 허용 의존성" 표에 있는가
- [ ] `api` / `implementation` 선택이 §8 "언제 `api`를 쓰는가" 표와 맞는가
  (판단이 안 서면 `implementation`으로 두고 컴파일이 깨질 때 올린다)
- [ ] `repository-jdbc`의 `schema` 의존이 `testImplementation`인가 (`main`이면 위반)
- [ ] JPA·QueryDSL·Exposed를 추가하지 않았는가 (§2에서 탈락 확정)
- [ ] 버전을 모듈에서 직접 명시하지 않았는가 (플러그인·버전 카탈로그가 결정)

### Bean 조립 (§5) — 빌드는 성공하는데 런타임에 터지는 자리

- [ ] `@AutoConfiguration` 클래스를 `AutoConfiguration.imports`에 FQCN으로 등록했는가
- [ ] **`api` 모듈에서 컨트롤러를 `@Bean`으로 등록했는가** — 빠지면 기동은 되고 호출만 404다
- [ ] `CrudRepository`가 있으면 `@EnableJdbcRepositories(basePackageClasses = ...)`를 붙였는가
- [ ] `@SpringBootApplication`·`@ComponentScan`을 쓰지 않았는가
- [ ] 구현체를 `internal`로 감췄는가

### 스키마 (§6)

- [ ] 엔티티에 `@Table(schema = "{domain}", ...)`가 있는가 — 빠지면 `public`을 친다
- [ ] `JdbcClient` SQL이 schema-qualified인가 (`FROM course.course`)
- [ ] 첫 changeset에 `CREATE SCHEMA IF NOT EXISTS {domain}`이 있는가
- [ ] 루트 `changelog-master.yaml`에 include를 추가했는가
- [ ] 도메인 간 FK가 없는가 (상대 식별자를 값 컬럼으로만)
- [ ] 이미 적용된 changeset을 수정하지 않고 새로 쌓았는가

### 도메인 규칙 (AGENTS.md)

- [ ] 클래스·테이블·API 이름이 §2 용어집을 따르는가 (임의 영문 번역 금지)
- [ ] 이름이 §4 "이름 규칙" 표 형태인가 (`{Aggregate}Identity`, `{Aggregate}QueryService` …)
- [ ] **스크리닝 문항·원인 매핑·자세 포인트가 코드에 하드코딩되지 않았는가** —
      요가 지도자 감수 전 데이터다. `schema/seed/`의 changeset으로 넣는다
- [ ] P1 기능(AGENTS.md §3 하단 목록)을 선제 구현하지 않았는가

## 새 도메인을 추가할 때

`docs/architecture.md` §10의 11단계를 **순서대로 전부** 밟는다. 하나만 빠져도
Bean이나 테이블을 못 찾는다. 이때는 `module-scaffolder` 서브에이전트를 쓴다.

## 아직 정해지지 않은 것 (§11) — 임의로 결정하지 않는다

- 도메인 분할 (`member` / `screening` / `course` / `training`은 후보일 뿐 미확정)
- 코루틴 사용 범위
- 모듈 의존성 검증 Gradle 태스크

이 셋에 걸리는 판단이 필요하면 **사용자에게 묻는다.** 조용히 정하지 않는다.
