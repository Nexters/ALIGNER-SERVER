# 아키텍처 — 도메인 내부 구조

Aligner 서버의 아키텍처 확정본. 2026-07-25 결정.

이 문서는 **도메인 하나가 어떤 모양인지**를 정의한다. 도메인을 몇 개로 쪼갤지는
이 골격이 확정된 다음 단계이며, 여기서 다루지 않는다.

## 1. 확정된 결정

| 항목 | 결정 |
| --- | --- |
| 아키텍처 | **Hexagonal Modular Monolith** (멀티모듈) |
| 배포 단위 | **단일** — 루트 `application-api` 하나만 실행 가능 |
| DB | **단일 인스턴스**, 도메인별 PostgreSQL schema 격리 |
| 영속성 | **Spring Data JDBC** (JPA·QueryDSL 탈락 — §2) |
| 서비스 로직 | **Command / Query 분리** — service와 out-port 두 계층 모두 |
| 도메인 서브모듈 | **기본 6개** + 필요 시 `contract` / `adapter-*` |
| Bean 등록 | **`@AutoConfiguration`** — ComponentScan 쓰지 않음 |
| 스키마 | **Liquibase** — 도메인별 changelog를 루트 master에서 include |
| 계층 규칙 강제 | **`build-logic` 컨벤션 플러그인** + `implementation`/`api` 규율 |

### 왜 모듈러 모놀리스인가

초기 방안의 "모듈별 배포 단위 + 서버 간 gRPC" 대신
"멀티모듈 기반 모듈러 모놀리스"를 채택한다. 2주 MVP에 분산 트랜잭션과
서비스 간 통신 비용을 얹을 이유가 없다.

k8s 학습 목표는 이 결정과 충돌하지 않는다. 단일 배포 단위여도 Pod 이중화·HA 컨트롤
플레인·ArgoCD 파이프라인은 그대로 구축한다. 학습 대상은 클러스터 운영이지 서비스 분해가
아니다.

그리고 이 구조는 나중에 분해할 때 손해가 아니다. 헥사고날 경계 덕분에 도메인을 떼어낼 때
바뀌는 건 Adapter뿐이고 `model`·`infrastructure`·`service`는 그대로다.

---

## 2. 왜 JPA가 아니라 Spring Data JDBC인가

**JPA의 값어치는 영속성 컨텍스트에 있다.** 더티체킹, 지연로딩, 캐스케이드, 1차 캐시.
그런데 이 구조는 `model`이 순수해야 하고 `service`가 영속성을 몰라야 한다. 그러면
`@Entity`는 `repository` 모듈에 갇히고, 밖으로는 변환해서 내보낸다.

이 순간 JPA의 기능이 전부 무력화된다. 변환해서 내보낸 모델은 detached라 더티체킹이 안 되고,
지연로딩은 경계 밖에서 터지고, 결국 `save()`를 명시적으로 부른다. **JPA를 쓰되 JPA를 안 쓰는
상태**가 되고 복잡도만 남는다.

Spring Data JDBC는 **애그리거트 단위 저장 + 영속성 컨텍스트 없음**이 설계 전제다.
JPA로 억지로 만들려던 상태가 여기서는 기본값이다.

| | JPA | Spring Data JDBC |
| --- | --- | --- |
| 순수 `model` 유지 | 변환 필요 + 기능 상실 | 변환이 자연스러움 |
| `save()` 명시 호출 | 더티체킹을 버리는 것 | 원래 그렇게 동작 |
| Kotlin `data class` | 권장되지 않음 (equals/hashCode, 프록시) | 그대로 씀 |
| 컴파일러 플러그인 | allopen / noarg / kapt | 없음 |
| 참조 구현 | 재해석 필요 | 1:1로 적용 |

**대가는 하나다: JPQL이 없어 복잡한 조회는 SQL을 직접 쓴다.** 다만 우리 도메인의 무거운
조회는 "코스 + 스텝 + 운동 + 체크포인트" 한 덩어리인데, 이건 조인 SQL 한 방이 ORM보다 낫다.
Command/Query를 나눈 이상 읽기 쪽이 SQL인 게 오히려 앞뒤가 맞는다.

> **QueryDSL은 쓰지 않는다.** JPA와 함께 탈락했다. QueryDSL은 `@Entity`에 어노테이션
> 프로세서를 돌려야 해서 Kotlin에서는 kapt가 필요한데, kapt는 유지보수 모드고 QueryDSL은
> KSP를 공식 지원하지 않는다. 동적 조회가 필요하면 `JdbcClient`로 SQL을 조립한다.
> (기존 AGENTS.md의 QueryDSL groupId 주의사항은 이 결정으로 무효가 됐다.)

---

## 3. 모듈 레이아웃

```text
ALIGNER-SERVER/
├── build-logic/                    ← 계층 타입별 컨벤션 플러그인
├── support-core/                   ← 공통 예외·에러 코드
├── support-web/                    ← 도메인 횡단 웹·보안 관심사 (§9)
├── application-api/                ← 단일 실행·배포 단위 (전체 조립)
└── {domain}/
    ├── model/                      ← 순수 도메인 모델 + 읽기 뷰 모델 + 도메인 예외
    ├── infrastructure/             ← out-port 인터페이스만 (구현 금지)
    ├── service/                    ← Command / Query 서비스
    ├── repository-jdbc/            ← port 구현 (CrudRepository / JdbcClient)
    ├── api/                        ← REST Controller + 요청·응답 DTO
    ├── schema/                     ← Liquibase changelog + 마스터 데이터 시드
    │
    ├── contract/                   ← (필요 시) 외부에 공개하는 발행 계약
    ├── adapter-{대상도메인}/        ← (필요 시) 다른 도메인 데이터 소비
    └── adapter-auth/               ← (member 전용) support-web 인증 port 구현
```

`contract`와 `adapter-*`는 **도메인 간 연결이 실제로 생길 때만** 만든다. 미리 만들지 않는다.

도메인별 `application-api`는 두지 않는다. 단일 배포 단위 결정의 직접적인 결과다.

`support-core`와 `support-web`은 **도메인이 아니다.** `support-core`는 도메인과 웹 계층이 함께
써야 하는 공통 예외·에러 코드만 담고, `support-web`은 도메인 `api` 모듈들이 공유해야 하는
웹·보안 타입만 담는 루트 레벨 공유 모듈이다. 여기에 비즈니스 로직을 넣지 않는다. 상세는 §9.

### 계층별 역할과 허용 의존성

| 모듈 | 역할 | 허용 의존성 | 컨벤션 플러그인 |
| --- | --- | --- | --- |
| `model` | 순수 도메인 모델, 읽기 뷰 모델, 도메인 예외 | `support-core` | `aligner.kotlin-lib` |
| `infrastructure` | out-port 인터페이스만 | `model` | `aligner.kotlin-lib` |
| `schema` | Liquibase changelog | **없음** | `aligner.kotlin-lib` |
| `contract` | 외부 공개 계약 + 발행 DTO | **없음** | `aligner.kotlin-lib` |
| `service` | Command/Query 서비스, Bean 등록, 자기 `contract` 구현체 | `model`, `infrastructure`, 자기 `contract`(필요 시) | `aligner.kotlin-boot` |
| `adapter-*` | 대상 도메인 계약 → 자기 port 구현 | 자기 `infrastructure`·`model`, 대상 `contract` | `aligner.kotlin-boot` |
| `adapter-auth` | `member`의 인증 연동 adapter | `support-web`, `member:contract` | `aligner.kotlin-boot` |
| `repository-jdbc` | port 구현 | `infrastructure`, `model` + **테스트에 한해 자기 `schema`** | `aligner.repository-jdbc` |
| `api` | Controller + DTO | `service`, `model`, `support-web` | `aligner.boot-mvc` |
| `support-core` | 공통 예외·에러 코드 (도메인 아님) | **없음** | `aligner.kotlin-lib` |
| `support-web` | 도메인 횡단 웹·보안 (도메인 아님) | `support-core` | `aligner.boot-mvc` |
| `application-api` | 전체 조립·실행 | 모든 도메인의 `api`·`repository-jdbc`·`schema`·`adapter-*`, `adapter-auth`, `support-web`, `support-core` | `aligner.boot-application` |

`repository-jdbc`의 `schema` 의존은 **`testImplementation`으로만** 허용한다. 통합 테스트가
TestContainers로 PostgreSQL을 띄운 뒤 그 도메인의 changelog를 돌려 테이블을 만들어야 하기
때문이다. `main` 소스셋에서 `schema`를 참조하면 위반이다.

```kotlin
// {domain}/repository-jdbc/build.gradle.kts
dependencies {
    implementation(project(":{domain}:infrastructure"))
    api(project(":{domain}:model"))

    testImplementation(project(":{domain}:schema"))   // 통합 테스트 전용
}
```

### 도메인 예외는 왜 별도 모듈이 아닌가

`exception`을 별도 모듈로 두면 격리를 사는 것 같지만, **그 대가로 얻는 게 없다.** 도메인 예외를
쓰는 건 `service`와 `repository-jdbc`인데 **둘 다 이미 `model`을 의존한다.** 예외만 쓰고 모델은
안 쓰는 모듈이 하나도 없으므로, 분리해도 아무것도 차단되지 않으면서 도메인마다 모듈 하나를 더
쓴다. 그래서 `model/exception/` 패키지로 둔다.

대신 `model`의 허용 의존성이 `support-core` 하나가 된다. `support-core`는 순수 Kotlin이라
**진짜 지켜야 할 규칙 — `model`에 Spring·JDBC 반입 금지 — 은 그대로다.** 컨벤션 플러그인이
`aligner.kotlin-lib`으로 클래스패스에서 Spring을 빼는 것도 변함없다.

### 의존 방향

```text
api ──→ service ──→ infrastructure(port) ←── repository-jdbc
         │                    ↑
         └──→ model ←─────────┴── adapter-*  ──→ {대상}:contract

application-api = api + repository-jdbc + schema + adapter-* + adapter-auth + support-web + support-core 조립
```

### 금지 규칙 — 위반 시 컴파일 단계에서 막혀야 정상

- `api` → `repository-jdbc` 직접 참조 금지. 반드시 `service` 경유.
- `service` → `CrudRepository`·`JdbcClient` 직접 참조 금지. `infrastructure`의 port 경유.
- `model`·`infrastructure`에 Spring·JDBC 타입 반입 금지. 컨벤션 플러그인이 클래스패스에서
  아예 빼므로 `Unresolved reference`로 터진다.
- 도메인 간 직접 참조 금지. `contract` / `adapter-*` 경유 (§7).
- 도메인 간 **DB FK 금지**. 상대 도메인의 식별자만 값으로 저장하고, 존재 검증은 port로 한다.
- 위 규칙 중 **컨벤션 플러그인은 클래스패스를, `implementation`/`api` 구분은 전이 누출을**
  각각 막는다(§8). 남는 건 "금지된 의존을 직접 한 줄 적는 경우"인데, 이건 **리뷰로 잡는다.**
  검증 태스크는 위반이 실제로 나오면 만든다 (§11).

---

## 4. Command / Query 분리

**service와 out-port 두 계층 모두에서 나눈다.** 쓰기는 애그리거트를 통째로 다루고, 읽기는
필요한 컬럼만 조인해서 뷰 모델로 바로 받는다.

```text
{domain}/model/
  Course.kt                       애그리거트 루트 (쓰기 경로)
  Step.kt
  view/CourseDetailView.kt        읽기 전용 뷰 모델
  view/CourseSummaryView.kt

{domain}/infrastructure/
  CourseRepository                쓰기 port  — 도메인 모델 입출력
  CourseQueryRepository           읽기 port  — View 반환

{domain}/service/
  CourseCommandService            공개 interface + internal Impl
  CourseQueryService              공개 interface + internal Impl

{domain}/repository-jdbc/
  CourseEntity.kt                 @Table, 애그리거트 매핑
  CourseJdbcRepository.kt         CrudRepository
  CourseRepositoryImpl.kt         쓰기 port 구현 — Entity ↔ Model 변환
  CourseQueryRepositoryImpl.kt    읽기 port 구현 — JdbcClient + 조인 SQL
```

### 규칙

- **양쪽을 항상 만들지 않는다.** 이건 분리 규칙이지 세트로 찍어내라는 뜻이 아니다. 쓰기가 없는
  애그리거트에 `CommandService`를 만들지 않고, 마스터 데이터처럼 조회만 하는 것에 쓰기 port를
  만들지 않는다. 필요해질 때 반대쪽을 추가한다.
- **읽기 뷰 모델은 `model/view/`에 둔다.** `api`는 `service`와 `model`만 의존하므로 뷰 모델을
  받을 수 있고, 계층 규칙은 그대로 유지된다. `infrastructure`에 두면 `api`가 port 모듈까지
  의존하게 되므로 금지.
- **읽기 경로는 쓰기 유스케이스를 호출하지 않는다.** `QueryService`가 `CommandService`를
  주입받는 코드가 보이면 잘못 나눈 것이다.
- **트랜잭션**: Command는 `@Transactional`, Query는 `@Transactional(readOnly = true)`.
- **쓰기 port는 애그리거트 단위**로만 오간다. `save(course: Course): Course`,
  `findByCourseIdentity(...): Course?`. 부분 갱신용 port를 만들지 않는다.
- **읽기 port는 화면 단위**로 만든다. 재사용을 노려 범용 조회 메서드를 만들지 말고, 필요한
  응답에 맞는 메서드를 그때그때 추가한다.
- Spring Data JDBC에는 더티체킹이 없다. **변경 후에는 항상 `save()`를 명시적으로 호출**한다.

### 이름 규칙

| 대상 | 형태 | 예 |
| --- | --- | --- |
| 애그리거트 루트 | `{Aggregate}` | `Course` |
| 식별자 | `{Aggregate}Identity` | `CourseIdentity` |
| 읽기 뷰 모델 | `{Aggregate}{용도}View` | `CourseDetailView` |
| 쓰기 port | `{Aggregate}Repository` | `CourseRepository` |
| 읽기 port | `{Aggregate}QueryRepository` | `CourseQueryRepository` |
| 쓰기 서비스 | `{Aggregate}CommandService` | `CourseCommandService` |
| 읽기 서비스 | `{Aggregate}QueryService` | `CourseQueryService` |
| JDBC 엔티티 | `{Aggregate}Entity` | `CourseEntity` |

도메인 용어는 `AGENTS.md` §2 용어집을 따른다. 임의 영문 번역을 만들지 않는다.

---

## 5. Bean 조립 — ComponentScan을 쓰지 않는다

각 모듈이 `@AutoConfiguration` 클래스로 자기 Bean을 명시 등록하고, `AutoConfiguration.imports`에
FQCN을 올린다. `application-api`가 Gradle 의존성으로 넣지 않은 모듈은 아예 로딩되지 않는다.
**모듈 조립을 클래스패스 우연이 아니라 빌드 선언이 결정하게 만드는 장치다.**

실행 모듈도 `@SpringBootApplication`을 쓰지 않는다. `@SpringBootApplication`은 기본
ComponentScan을 포함하므로, 패키지 위치에 따라 `@Component`·`@RestController`가 우연히 잡힐 수
있다. `application-api`는 `@SpringBootConfiguration` + `@EnableAutoConfiguration`만 사용한다.

```kotlin
// application-api/AlignerApplication.kt
@SpringBootConfiguration
@EnableAutoConfiguration
class AlignerApplication

fun main(args: Array<String>) {
    runApplication<AlignerApplication>(*args)
}
```

```kotlin
// {domain}/service/CourseService.kt
interface CourseCommandService {
    fun prescribe(command: PrescribeCourseCommand): Course
}

internal class CourseCommandServiceImpl(
    private val courseRepository: CourseRepository,
) : CourseCommandService { /* ... */ }
```

```kotlin
// {domain}/service/CourseServiceAutoConfiguration.kt
@AutoConfiguration
class CourseServiceAutoConfiguration {
    @Bean
    fun courseCommandService(courseRepository: CourseRepository): CourseCommandService =
        CourseCommandServiceImpl(courseRepository)
}
```

```text
{domain}/service/src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
  └ team.aligner.{domain}.service.CourseServiceAutoConfiguration
```

구현체는 `internal`로 감춘다. 다른 모듈은 인터페이스만 본다.

### 컨트롤러도 직접 등록해야 한다

**ComponentScan을 뺐으므로 `@RestController`도 스캔되지 않는다.** `api` 모듈의
`@AutoConfiguration`에서 `@Bean`으로 등록하지 않으면 컨트롤러가 존재하지 않는 것과 같다.
빌드도 기동도 성공하고 **호출만 404**가 되므로, 이 구조에서 원인 찾기가 가장 어려운 실수다.

```kotlin
// {domain}/api/CourseApiAutoConfiguration.kt
@AutoConfiguration
class CourseApiAutoConfiguration {
    @Bean
    fun courseController(
        courseCommandService: CourseCommandService,
        courseQueryService: CourseQueryService,
    ): CourseController = CourseController(courseCommandService, courseQueryService)
}
```

컨트롤러 클래스에는 `@RestController`를 그대로 붙인다. `RequestMappingHandlerMapping`은
Bean이 어떻게 등록됐는지와 무관하게 타입의 `@Controller`·`@RequestMapping`을 보고 핸들러를
찾으므로, 어노테이션은 남기고 등록 방식만 바꾸는 것이다.

### repository-jdbc에는 @EnableJdbcRepositories가 필요하다

`CrudRepository`를 상속한 인터페이스는 리포지토리 스캔이 있어야 Bean이 만들어진다.
`@AutoConfiguration`에 `@EnableJdbcRepositories`를 함께 붙이고 스캔 범위를 그 도메인으로 좁힌다.

```kotlin
// {domain}/repository-jdbc/CourseRepositoryAutoConfiguration.kt
@AutoConfiguration
@EnableJdbcRepositories(basePackageClasses = [CourseJdbcRepository::class])
class CourseRepositoryAutoConfiguration {
    @Bean
    fun courseRepository(jdbcRepository: CourseJdbcRepository): CourseRepository =
        CourseRepositoryImpl(jdbcRepository)

    @Bean
    fun courseQueryRepository(jdbcClient: JdbcClient): CourseQueryRepository =
        CourseQueryRepositoryImpl(jdbcClient)
}
```

`basePackages`에 문자열을 쓰지 않고 `basePackageClasses`를 쓴다. 패키지명을 바꿔도 깨지지 않는다.

### 안 보일 때 체크리스트

이 방식의 실수 지점은 등록 누락 하나로 모인다. 증상별로 확인 순서가 다르다.

**"Bean이 없다"**

1. 모듈의 `AutoConfiguration.imports`에 FQCN을 등록했는가
2. `application-api/build.gradle.kts`에 그 모듈 의존성을 추가했는가
3. 클래스에 `@AutoConfiguration` + `@Bean`이 붙어 있는가
4. `CrudRepository` Bean이면 `@EnableJdbcRepositories`가 붙어 있는가

**엔드포인트가 404다** — 기동은 정상인데 호출이 안 될 때

1. `api` 모듈의 `@AutoConfiguration`에 컨트롤러를 `@Bean`으로 등록했는가
2. 그 `@AutoConfiguration`이 `AutoConfiguration.imports`에 있는가

`@SpringBootApplication`을 쓰지 않으므로 "패키지 아래 있으니 잡히겠지"는 성립하지 않는다.

**테이블이 없다**

1. 엔티티에 `@Table(schema = "{domain}", name = ...)`를 붙였는가 — 빠지면 `public`을 친다 (§6)
2. `{domain}/schema`의 changelog에 해당 SQL이 포함됐는가
3. 루트 `changelog-master.yaml`에 도메인 changelog를 include 했는가
4. 통합 테스트라면 `testImplementation(project(":{domain}:schema"))`를 걸었는가

---

## 6. 스키마 — Liquibase

단일 DB를 쓰되 도메인별 PostgreSQL schema와 changelog를 분리한다. 두 명이 서로 다른 도메인을
동시에 작업해도 파일이 충돌하지 않고, 테이블 이름도 도메인 경계 안에서만 충돌한다.

```text
application-api/src/main/resources/db/changelog-master.yaml
  ├ include: classpath:db/{domain-a}/db.changelog-{domain-a}.yaml
  └ include: classpath:db/{domain-b}/db.changelog-{domain-b}.yaml

{domain}/schema/src/main/resources/db/{domain}/
  ├ db.changelog-{domain}.yaml
  ├ ddl/001-create-{table}.sql
  └ seed/001-{마스터데이터}.sql
```

### 규칙

- 새 도메인을 추가하면 `changelog-master.yaml`에 include를 반드시 추가한다.
- PostgreSQL schema 이름은 도메인명과 동일하게 둔다. 예: `course.course`,
  `training.session`.
- 각 도메인 changelog의 첫 changeset은 `CREATE SCHEMA IF NOT EXISTS {domain}`를 포함한다.
- 모든 DDL은 schema-qualified table name을 사용한다. 예: `CREATE TABLE course.course (...)`.
- **Liquibase 자기 추적 테이블은 `public`에 둔다.** `DATABASECHANGELOG`·`DATABASECHANGELOGLOCK`은
  도메인 데이터가 아니라 마이그레이션 메타데이터이므로 도메인 schema에 섞지 않는다.
  기본 동작이 이미 그렇지만, 나중에 `defaultSchema`를 건드릴 때 딸려가지 않도록 고정해둔다.

  ```yaml
  spring:
    liquibase:
      liquibase-schema: public
  ```
- **도메인 간 FK를 걸지 않는다.** 상대 도메인 식별자는 값 컬럼으로만 저장한다
  (예: `session` 테이블은 `member` 테이블에 FK를 걸지 않고 `member_id`만 가진다).
  경계를 DB 레벨에서 묶으면 모듈 독립성과 삭제 정책이 엉킨다.
- **스크리닝 문항·원인 매핑·자세 포인트는 요가 지도자 감수 전 데이터다.** 코드에 하드코딩하지
  말고 `schema/seed/`의 changeset으로 넣는다. 감수 결과가 나오면 새 changeset을 추가한다.
- 이미 적용된 changeset은 수정하지 않는다. 항상 새 changeset을 쌓는다.

### 코드에서도 schema를 한정한다

DDL만 schema-qualified로 쓰면 런타임에 테이블을 못 찾는다. **엔티티와 쿼리 양쪽 다** 명시한다.

```kotlin
// {domain}/repository-jdbc
@Table(schema = "course", name = "course")
class CourseEntity(...)
```

```kotlin
// 읽기 경로의 JdbcClient SQL도 마찬가지
jdbcClient.sql(
    """
    SELECT c.course_id, c.cause_code, s.step_order, s.exercise_id
    FROM course.course c
    JOIN course.step s ON s.course_id = c.course_id
    WHERE c.course_id = :courseId
    """,
)
```

**`NamingStrategy`로 도메인별 기본 schema를 주는 우회는 쓸 수 없다.** 단일 애플리케이션
컨텍스트에 `JdbcMappingContext`가 하나뿐이라 `NamingStrategy`도 전역 하나다. 도메인마다 다른
값을 줄 수 없으므로 **엔티티마다 `@Table(schema = ...)`를 붙이는 것이 유일한 방법**이다.

`search_path`를 손대는 방법도 쓰지 않는다. 연결 설정에 도메인 경계를 숨기면 SQL만 보고는 어느
schema를 치는지 알 수 없게 되고, 커넥션 풀 재사용 시 상태가 새는 위험도 생긴다.

---

## 7. 도메인 간 통신

도메인 간 직접 참조는 금지다. 요청 도메인이 **자기 `infrastructure`에 port를 정의**하고,
연결 Adapter는 별도 `adapter-*` 모듈에 둔다.

```text
요청 도메인 service
  → 요청 도메인 infrastructure 의 Port          (도메인 언어로 최소한만)
  → 요청 도메인 adapter-{대상} 의 Adapter 구현체
  → {대상}:contract 의 발행 계약
```

### 규칙

- 요청 도메인의 `service`는 대상 도메인 타입을 **import하지 않는다.** 컴파일 의존이 없어야 정상.
- Adapter는 대상 도메인의 **`contract`만** 의존한다. 대상의 `service`·`model`·`repository-jdbc`를
  직접 쓰지 않는다. 대상의 `service`는 그 도메인 자기 Controller를 위한 in-port이지 통합
  계약이 아니다.
- `contract`는 외부 통합 전용으로 **좁게** 만든다. 식별자는 원시 타입으로 받고, 반환은 발행
  DTO로 한다. 구현체는 `internal`로 대상 도메인 `service`에 두고 Bean도 거기서 등록한다.
- `application-api`는 Adapter를 구현하지 않는다. 조립만 한다.

---

## 8. build-logic — 규칙을 빌드가 강제한다

계층 타입별 컨벤션 플러그인을 직접 작성해 모듈 타입별 클래스패스와 빌드 동작을 고정한다.

```text
build-logic/src/main/kotlin/
  aligner.kotlin-lib.gradle.kts        ← model, infrastructure, schema, contract, support-core
  aligner.kotlin-boot.gradle.kts       ← service, adapter-*, adapter-auth
  aligner.repository-jdbc.gradle.kts   ← repository-jdbc
  aligner.boot-mvc.gradle.kts          ← api, support-web
  aligner.boot-application.gradle.kts  ← application-api
```

| 플러그인 | 제공하는 것 |
| --- | --- |
| `aligner.kotlin-lib` | Kotlin JVM, Kotest, ktlint. **Spring 없음** |
| `aligner.kotlin-boot` | 위 + `spring-boot-autoconfigure`, `spring-tx`. `bootJar` 비활성 |
| `aligner.repository-jdbc` | 위 + `spring-boot-starter-data-jdbc`, PostgreSQL 드라이버, TestContainers |
| `aligner.boot-mvc` | 위 + `spring-boot-starter-web`, Spring Security, springdoc |
| `aligner.boot-application` | 실행 가능 boot jar, Liquibase |

### `api` / `implementation` — 무엇을 막고 무엇을 못 막는가

`implementation`은 의존 방향 제어를 **절반만** 담당한다. 셋을 구분해야 한다.

| | `implementation(project(":x"))` | `api(project(":x"))` |
| --- | --- | --- |
| 내 컴파일 클래스패스 | x 있음 | x 있음 |
| **소비자의 컴파일 클래스패스** | **x 없음** | x 있음 |
| 소비자의 런타임 클래스패스 | x 있음 | x 있음 |

**막아주는 것 — 전이 누출.** `api` 모듈이 `implementation(project(":{d}:service"))`로 걸면,
`application-api`는 `api`에 의존해도 `service` 타입으로 컴파일할 수 없다. 계층을 건너뛴 참조가
의존성을 타고 조용히 번지는 걸 막는다.

**못 막는 것 — 직접 선언한 위반.** `api/build.gradle.kts`에 누가
`implementation(project(":{d}:repository-jdbc"))`를 한 줄 적으면 그냥 컴파일된다.
§3의 금지 규칙은 이 경우를 못 잡는다. **여기가 리뷰의 몫이다** — 빌드 파일에 의존성을 추가하는
PR은 이 표와 §3을 대조해서 본다. 이 위반이 실제로 나오면 그때 검증 태스크를 만든다 (§11).

**의도적으로 남겨둔 것 — 런타임 전이.** `implementation`이어도 런타임 클래스패스에는 올라간다.
`api` → `service`가 `implementation`이지만 `service`의 `AutoConfiguration.imports`는
런타임에 발견되므로 Bean 등록은 정상 동작한다. §5의 조립이 이것에 의존한다.

### 언제 `api`를 쓰는가

**내 공개 시그니처에 그 모듈의 타입이 등장할 때만** `api`다. 나머지는 전부 `implementation`.

| 선언 위치 | 대상 | 설정 | 이유 |
| --- | --- | --- | --- |
| `infrastructure` | `model` | **`api`** | port 시그니처가 도메인 모델을 노출 |
| `service` | `model` | **`api`** | 공개 서비스 인터페이스가 모델·뷰를 반환 |
| `service` | `infrastructure` | `implementation` | port는 internal 구현체의 생성자 인자일 뿐 |
| `service` | `support-core` | `implementation` | 예외를 던질 뿐 시그니처에 없음 |
| `service` | 자기 `contract` | **`api`** | contract 구현체를 Bean으로 노출 |
| `repository-jdbc` | `model` | **`api`** | port 구현이 모델을 반환 |
| `repository-jdbc` | 나머지 | `implementation` | 구현체는 전부 internal |
| `api` | `service`·`model`·`support-web` | `implementation` | 컨트롤러 밖으로 아무것도 안 나감 |
| `adapter-*` | 전부 | `implementation` | 어댑터 구현체는 internal |
| `adapter-auth` | `support-web`·`member:contract` | `implementation` | 인증 adapter 구현체는 internal |
| `application-api` | 전부 | `implementation` | 조립 종착점, 소비자 없음 |

판단이 안 서면 `implementation`으로 두고 컴파일이 깨질 때 `api`로 올린다. 반대 방향보다 안전하다.

각 모듈의 `build.gradle.kts`는 이 정도면 끝난다.

```kotlin
// {domain}/model/build.gradle.kts
plugins { id("aligner.kotlin-lib") }

dependencies {
    api(project(":support-core"))   // ErrorCode를 도메인 예외가 들고 있음
}
```

```kotlin
// {domain}/service/build.gradle.kts
plugins { id("aligner.kotlin-boot") }

dependencies {
    api(project(":{domain}:model"))
    implementation(project(":{domain}:infrastructure"))
}
```

`contract`가 있는 도메인은 공개 계약 구현체를 `service`에 두므로 자기 `contract`도 공개 의존으로
추가한다.

```kotlin
dependencies {
    api(project(":{domain}:contract"))
}
```

버전은 개별 모듈에서 명시하지 않는다. 플러그인과 버전 카탈로그가 결정한다.

`buildSrc`가 아니라 `build-logic` + `includeBuild`인 이유: `buildSrc`는 한 줄만 고쳐도 전체
모듈이 재컴파일된다. 초기에 빌드 설정을 자주 고치게 되는데 그때마다 비용을 낸다.

---

## 9. support-web — 도메인 횡단 웹·보안

인증·예외 응답·에러 포맷은 어느 한 도메인의 것이 아니면서 모든 도메인 `api`가 필요로 한다.
6개 도메인 모듈 중에는 이걸 담을 자리가 없다. `model`은 Spring이 금지고, `service`도 Security를
몰라야 하며, `application-api`에 두면 도메인 `api`가 조립 모듈을 역방향 참조하게 된다.

그래서 **루트 레벨 공유 모듈 `support-core`와 `support-web`**을 둔다.

```text
support-core/
├── BaseException.kt              공통 예외 부모
└── ErrorCode.kt                  HTTP 상태와 응답 코드를 담는 공통 계약

support-web/
├── AlignerPrincipal.kt          인증된 회원 표현
├── SecurityConfig.kt            SecurityFilterChain, OAuth2 공통 설정
├── ApiErrorResponse.kt          공통 에러 응답 포맷 (Spring 의 ErrorResponse 와 이름 충돌 회피)
└── GlobalExceptionHandler.kt    @RestControllerAdvice
```

### 규칙

- `support-core`는 Spring을 모른다. 도메인 `model`과 `support-web`이 함께 의존할 수 있는
  최소 공통 타입만 둔다.
- `support-web`은 **도메인이 아니다.** 비즈니스 로직·도메인 모델·DB 접근을 넣지 않는다.
  들어가도 되는 건 "모든 도메인 `api`가 똑같이 필요로 하는 웹·보안 타입"뿐이다.
- 어느 도메인 구현 모듈에도 의존하지 않는다. 의존하고 싶어지면 그건 그 도메인의 것이다.
- `api` 모듈과 `member:adapter-auth`만 `support-web`을 의존한다. `service` 이하는 참조하지 않는다.
- **예외 → HTTP 상태 매핑**은 `GlobalExceptionHandler`가 `support-core`의 `BaseException`과
  `ErrorCode`만 보고 처리한다. 각 도메인의 `model/exception/`은 자기 예외가 적절한 `ErrorCode`를
  들고 있게 만들고, HTTP 응답 포맷은 모른다.

```kotlin
// support-core
interface ErrorCode {
    val status: Int
    val code: String
    val message: String
}

abstract class BaseException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
```

```kotlin
// support-web
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BaseException::class)
    fun handle(exception: BaseException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(exception.errorCode.status)
            .body(ApiErrorResponse.from(exception.errorCode))
}
```

### 카카오 로그인 책임 분리

`support-web`은 OAuth2 필터와 인증 성공 흐름의 웹 계층만 담당한다. 카카오 식별자로 회원을
조회하거나 가입시키는 로직은 `member` 도메인의 책임이다.

```text
support-web
  → AuthMemberPort                 웹 계층 언어의 최소 port
  → member/adapter-auth            AuthMemberPort 구현
  → member/contract                회원 조회·가입 계약
  → member/service                 실제 회원 도메인 로직
```

`AuthMemberPort`는 `support-web`에 두고, 구현체는 `member/adapter-auth`에 둔다.
`adapter-auth`는 일반 도메인 간 adapter가 아니라 웹 인증 port를 회원 도메인 계약에 연결하는
인증 전용 adapter다. `support-web`이 `member:service`나 `member:model`을 직접 의존하지 않기 위한
장치다.

```kotlin
// support-web
interface AuthMemberPort {
    fun findOrRegisterByKakao(command: KakaoLoginCommand): AuthenticatedMember
}
```

```text
member/
  contract/                         support-web에서 호출 가능한 좁은 회원 인증 계약
  adapter-auth/                      support-web AuthMemberPort 구현
```

`application-api`는 `support-web`, `member:contract`, `member:adapter-auth`를 함께 조립한다.
`member:adapter-auth`가 빠지면 OAuth2 인증 성공 시 필요한 `AuthMemberPort` Bean을 찾지 못해 기동이
실패해야 정상이다.

### 인증 정보를 service에 전달하는 법

`service`는 Spring Security를 모른다. `api`가 `SecurityContext`에서 인증된 회원 식별자를
꺼내 **파라미터로 넘긴다.**

```kotlin
// {domain}/api
@PostMapping("/courses/{courseId}/sessions")
fun start(
    @AuthenticationPrincipal principal: AlignerPrincipal,   // support-web
    @PathVariable courseId: Long,
): SessionResponse =
    sessionCommandService
        .start(MemberIdentity.of(principal.memberId), CourseIdentity.of(courseId))
        .toResponse()
```

`service` 시그니처에 `Authentication`·`Principal` 같은 Spring Security 타입이 등장하면
잘못된 것이다.

> `member` 도메인과 혼동하지 않는다. `support-web`은 "이 요청을 누가 보냈나"를 **웹 계층에서
> 표현**할 뿐이고, 회원의 실제 정보·가입·프로필은 `member` 도메인이 소유한다.
> `AlignerPrincipal`은 식별자와 인증에 필요한 최소한만 담는다.

---

## 10. 새 도메인 추가 절차

하나라도 빠지면 Bean이나 테이블을 못 찾는다.

1. `{domain}/` 아래 기본 6개 모듈 디렉터리 생성
2. 각 모듈에 `build.gradle.kts` 작성 — 컨벤션 플러그인 1줄 + 허용된 의존성만.
   `api`/`implementation` 구분은 §8 표를 따른다
3. 루트 `settings.gradle.kts`에 6개 모듈 `include`
4. 패키지 루트는 `team.aligner.{domain}`
5. `@AutoConfiguration` 클래스와 `AutoConfiguration.imports` 작성 — 세 모듈 모두
   - `service` — 서비스 Bean
   - `repository-jdbc` — `@EnableJdbcRepositories` + port 구현 Bean
   - `api` — **컨트롤러를 `@Bean`으로 등록** (안 하면 404, §5)
6. `schema` changelog 작성 — 첫 changeset에 `CREATE SCHEMA IF NOT EXISTS {domain}`,
   이후 DDL은 schema-qualified (§6)
7. 루트 `changelog-master.yaml`에 도메인 changelog include 추가
8. `application-api/build.gradle.kts`에 `api`·`repository-jdbc`·`schema` 의존성 추가
9. `repository-jdbc`에 `testImplementation(project(":{domain}:schema"))` 추가 —
   통합 테스트가 테이블을 만들 수 있어야 한다
10. 다른 도메인 데이터가 필요하면 `contract` / `adapter-*` 추가 (§7)
11. `member`에서 카카오 로그인을 구현하면 `contract` / `adapter-auth` 추가 (§9)

---

## 11. 아직 정하지 않은 것

- **도메인 분할** — 이 골격 위에 어떤 도메인을 놓을지. `member` / `screening` / `course` /
  `training`이 후보였으나 확정 전이다.
- **코루틴 사용 범위** — 기술 스택에 kotlinx-coroutines가 있으나 Spring Data JDBC는 블로킹이다.
  JDK 25 + Spring Boot 4 조합이면 가상 스레드가 더 자연스럽다. 실제 비동기가 필요한 지점이
  나올 때 다시 판단한다.
- **모듈 의존성 검증 태스크** — 만들지 여부부터 미정이다. 커스텀 Gradle 태스크는 반나절 이상
  걸리는 실제 작업이고, MVP 전에 만들면 그만큼 기능이 밀린다. 컨벤션 플러그인과
  `implementation`/`api` 규율이 이미 대부분을 막으므로, **2인 팀에서는 리뷰로 시작한다.**
  금지된 의존을 직접 선언한 위반이 실제로 한 번이라도 나오면 그때 만든다.
