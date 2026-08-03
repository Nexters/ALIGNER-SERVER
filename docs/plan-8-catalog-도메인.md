# catalog 도메인 구축 — 구현 플랜 (이슈 #8)

브랜치 `feature/8-catalog-도메인-구축`, PR 대상 `develop`.
근거 표기는 `docs/architecture.md`를 §, `docs/domains.md`를 §D로 쓴다.

---

## 0. 먼저 내려야 하는 세 가지 판단

### 0-1. `adapter-ymove` — 이번 범위에서 뺀다. `PoseVideoPort`도 만들지 않는다 (권고)

| 후보 | 판정 |
| --- | --- |
| **A. 모듈·port 둘 다 제외** ✅ | §3 "`contract`와 `adapter-*`는 도메인 간 연결이 실제로 생길 때만 만든다. 미리 만들지 않는다". 구현자도 소비자도 없는 port는 검증 가치가 0 |
| B. `PoseVideoPort`만 정의 | 시그니처는 §D4-3-1에 확정돼 있으나, 아무 `service`도 주입받지 않아야 기동이 성공한다 — "있는데 안 쓰는" 상태를 리뷰가 매번 재확인해야 한다 |
| C. adapter 스텁 | 블로커 4(타임아웃·재시도·메시지)·5(캐시 TTL)가 곧 스텁의 내용이다. 스텁은 결정을 감춘 것 |

**A를 뒷받침하는 사실:**

1. `catalog.target_pose.image_url`이 `catalog` seed 컬럼이다(§D4-3). **핀포즈 그리드(§D4-2)는 YMove 없이 렌더링된다.** 다음 도메인 `screening`이 막히지 않는다.
2. `build-logic/src/main/kotlin/aligner.kotlin-boot.gradle.kts`는 `spring-boot-autoconfigure` + `spring-tx`만 준다. **`spring-web`이 없어 `adapter-ymove`가 `RestClient`를 쓰려면 `libs.versions.toml`에 좌표를 새로 넣어야 한다.** §5 "`build.gradle.kts` 의존성 변경은 최우선 리뷰 대상"에 걸리므로 실제 클라이언트 코드와 함께 리뷰되는 게 맞다.

**대가:** 이번 범위의 `catalog` API는 재생 URL·운동 썸네일을 못 준다. `exercise`에 이미지 컬럼이 없고 썸네일은 YMove 소유(§D4-3-1)이므로 **운동 목록 썸네일은 후속 이슈까지 비어 있다.**

**음성 큐잉 대본은 여기서 빠지지 않는다.** 대본 소유가 YMove에서 `catalog` seed로 바뀌었으므로(§D4-3-1) 외부 연동과 무관해졌다. `exercise_voice_cue` DDL은 **이번 범위**, 번역 seed 값은 후속(0-2와 같은 이유)이다.

**후속 이슈 초안:** "catalog YMove 연동 — PoseVideoPort와 adapter-ymove", blocked-by §D7-4·5·6.

→ 이번 범위는 **7개 모듈**. §D5의 "8개"는 최종 계획이지 이 이슈의 산출물 수가 아니다.

### 0-2. seed 데이터 — DDL만 이번 이슈, 감수 데이터는 후속 changeset (권고)

이슈 완료 조건에 "seed 행이 들어간다"가 없다. 반면 지금 넣으면 되돌리기 비용이 크다:

- **블로커 6(좌우 분리 자세 `*-left`/`*-right`)이 `exercise`·`target_pose`의 행 단위를 직접 결정한다.** 한 행이냐 두 행이냐가 `exercise_id` 값을 바꾼다.
- §6 "이미 적용된 changeset은 수정하지 않는다". `exercise_id`가 `course` seed에 박히면 연쇄된다.
- **`body_part_code` 값 집합의 정본이 저장소에 없다.** `target_pose.body_part_code`·`muscle.body_part_code`는 `screening.body_part`(§D4-2) 값을 받는데 `screening`은 착수 순서상 catalog **다음**이다. 두 도메인 seed가 같은 어휘를 써야 하므로 부위 코드가 먼저 확정돼야 한다 → **확인 필요**

**권고:** `ddl/`만 만들고 `seed/`는 만들지 않는다. 통합 테스트는 seed에 의존하지 않고 `JdbcClient`로 직접 픽스처를 넣는다.

**단, `screening` 착수 전에는 catalog seed가 끝나야 한다** — `screening.cause_rule`이 `target_pose_id`를 참조한다. 후속 순서: `catalog seed` → `screening`.

### 0-3. `ymove_slug`는 NULL 허용 + UNIQUE (권고)

§D4-3은 `ymove_slug(uk)`라고만 적었다. PostgreSQL `UNIQUE`는 NULL을 여러 개 허용하므로 **NULL 허용은 "uk" 표기와 충돌하지 않는다.** 블로커 6 미결 상태에서도 행을 넣을 수 있고, 확정 후 `UPDATE` changeset으로 채운다. `NOT NULL`로 박으면 블로커 6이 풀릴 때까지 어떤 행도 못 넣는다.

---

## 1. member에서 가져올 것 / 달라져야 할 것

### 그대로 가져온다

| 항목 | member 원본 |
| --- | --- |
| 모듈별 `build.gradle.kts` 형태와 `api`/`implementation` 판단 | `member/*/build.gradle.kts` |
| `@AutoConfiguration` + `AutoConfiguration.imports` 1줄 | `member/service`, `repository-jdbc`, `api` |
| 공개 interface + `internal ...Impl`, `@Transactional`을 클래스에 부착 | `MemberQueryService.kt` |
| `contract` 모듈은 의존성 0, 인터페이스 + DTO를 한 파일에 | `MemberAuthContract.kt` |
| contract 구현체는 `internal`로 `service`에, Bean도 거기서 | `MemberAuthContractImpl.kt` |
| changeset id에 도메인 접두사 | `db.changelog-member.yaml` |
| DDL은 schema-qualified, `IF NOT EXISTS` 안 씀, `COMMENT ON` 부착 | `001-create-member.sql` |
| `JdbcClient` SQL을 schema-qualified로 | `MemberQueryRepositoryImpl.kt` |
| 통합 테스트는 JUnit5 + `spring-boot-starter-test`, 단언만 kotest | `MemberRepositoryIntegrationTest.kt` |
| 통합 테스트 부트스트랩을 `…jdbc.bootstrap` 하위 패키지에 | `bootstrap/MemberRepositoryTestApplication.kt` |
| 통합 테스트 `application.yml`이 그 도메인 changelog만 가리킴 | `member/repository-jdbc/src/integrationTest/resources/application.yml` |
| `to_regclass('public.…')`이 null임을 단언하는 schema 격리 테스트 | 같은 파일 |
| 단위 테스트는 Kotest `DescribeSpec` + mockk | `MemberCommandServiceTest.kt` |

> `member/repository-jdbc/.../jdbc/MemberRepositoryTestApplication.kt`(bootstrap이 **아닌** 쪽)는 untracked 잔여물이다. 복제하지 않는다.

### 달라져야 하는 것 — Command가 없기 때문

| # | member | catalog | 근거 |
| --- | --- | --- | --- |
| 1 | `MemberRepository`(쓰기 port) | **없음** | §4 "마스터 데이터처럼 조회만 하는 것에 쓰기 port를 만들지 않는다" / §D4-3 |
| 2 | `MemberCommandService` | **없음** | 동일 |
| 3 | `Member.kt` 애그리거트 + 팩토리 | **애그리거트 클래스를 만들지 않는다** | 아래 |
| 4 | `MemberEntity.kt`(`@Table`) | **없음** | 저장 경로가 없어 Spring Data JDBC 매핑이 불필요 |
| 5 | `MemberJdbcRepository`(`CrudRepository`) | **없음** | 동일 |
| 6 | `@EnableJdbcRepositories` | **붙이지 않는다** | `CrudRepository`가 하나도 없다 |
| 7 | `created_at`/`updated_at` | **없음** | §D4-3 컬럼 목록에 없다. 쓰기가 없으니 감사 시각이 생길 자리도 없다 |
| 8 | `@Bean(name = […])` 이름 고정 | **불필요** | 이름 망글링은 `internal fun`일 때만 문제 |
| 9 | AutoConfiguration 4개 | **3개** | adapter 없음 |
| 10 | 도메인 내부 FK 없음(단일 테이블) | **`catalog` 내부 FK는 건다** | §6이 금지한 것은 **도메인 간** FK다 |
| 11 | 애그리거트 1개 | **테이블 6개, 조회 진입점 2개** | QueryRepository·QueryService를 애그리거트별로 분리 |
| 12 | `SecurityConfig` 변경 | **변경하지 않는다** | 기본값 `anyRequest().authenticated()`. `permitAll`을 추가하지 않는다 |

**#3 보충.** §D4-3의 "애그리거트" 행은 **소유 경계**를 적은 것이지 클래스를 찍으라는 뜻이 아니다. 쓰기 port가 없으므로 생성할 코드도 소비할 코드도 없다 — `QueryRepository`는 `model/view/`의 View를 반환하고 `api`는 View를 DTO로 바꾼다. 애그리거트 클래스를 만들면 **컴파일은 되지만 아무도 참조하지 않는 죽은 코드**다. §3 "미리 만들지 않는다", §4 "세트로 찍어내라는 뜻이 아니다"에 걸린다.

대신 `{Aggregate}Identity`는 만든다 — QueryRepository 파라미터 타입으로 실제로 쓰인다.

---

## 2. 단계별 구현

각 단계 끝에서 `./gradlew build` 통과. `settings.gradle.kts`는 **그 단계에서 만든 모듈만** 추가한다.

### 1단계 — `model`

```
catalog/model/build.gradle.kts
catalog/model/src/main/kotlin/team/aligner/catalog/model/ExerciseIdentity.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/TargetPoseIdentity.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/MuscleRole.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/view/ExerciseDetailView.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/view/ExerciseSummaryView.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/view/TargetPoseDetailView.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/view/TargetPoseSummaryView.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/view/MuscleView.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/view/ExerciseVoiceCueView.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/exception/CatalogErrorCode.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/exception/ExerciseNotFoundException.kt
catalog/model/src/main/kotlin/team/aligner/catalog/model/exception/TargetPoseNotFoundException.kt
```

`build.gradle.kts`: `aligner.kotlin-lib` + `api(project(":support-core"))`. 근거 §3 표, §8.

**판단**
- `MuscleRole`은 `STRETCH | STRENGTHEN` enum. **감수 전 데이터가 아니다** — §D4-3이 값 집합을 확정했다. 닫힌 구조 어휘라 seed 하드코딩 금지(§6)에 안 걸린다. 반면 운동명·MET·난이도·금기·근육 이름은 전부 seed다.
- View는 `model/view/`. §4 "`infrastructure`에 두면 `api`가 port 모듈까지 의존하게 되므로 금지".
- `MuscleView(muscleCode, name, bodyPartCode, highlightAssetKey, role, displayOrder)`를 Detail View가 `List<MuscleView>`로 품는다.
- `MuscleIdentity`는 만들지 않는다 — 근육 코드로 조회하는 화면이 없다.
- **음성 큐는 `ExerciseDetailView`에만 싣는다.** `ExerciseVoiceCueView(displayOrder, offsetSeconds, content)`를 `List`로 품는다.
  - **`ExerciseSummaryView`에는 넣지 않는다.** 대본이 필요한 곳은 세션 플레이어 하나이고, 코스 상세·홈 목록은 스텝을 개수만큼 그린다(§D4-3-1 "장애와 성능"). 요약에 대본을 실으면 목록 1회 조회에 큐 테이블 조인이 스텝 수만큼 붙고 응답 크기도 스텝 수에 비례해 커진다 — YMove를 스텝마다 치지 않기로 한 것과 같은 이유다.
  - `offsetSeconds: Int?` — 타임코드 미확정이므로 nullable이다(§D4-3). 클라이언트는 `null`이면 `displayOrder` 순차 재생으로 읽는다.
  - **`ExerciseVoiceCueIdentity`를 만들지 않는다** — `cue_id`로 단건 조회하는 화면이 없다. 큐는 항상 운동 상세에 딸려 나온다.
  - `contract`에는 넣지 않는다(3단계와 같은 판단). 세션 플레이어는 `catalog` API의 운동 상세를 직접 부르고, `training`이 대본을 필요로 한다는 근거가 §D4-5에 없다. → **확인 필요**: 세션 재생 데이터를 `training` 응답에 합쳐 내릴 계획이면 그때 `ExerciseResponse`에 추가한다
- **칼로리 필드를 View에 두지 않는다.** §D4-3 + 계산 입력인 몸무게는 `member` 소유이고 `catalog`는 `member`를 의존할 수 없다(§D1). `metValue`만 싣는다.
- `metValue: BigDecimal` (DB `NUMERIC(4,2)`). → **확인 필요**: `Double`로 낮출지. 정본에 명시 없음.

**검증**
- `./gradlew :catalog:model:build`
- 계층 규칙 1회성 확인: `import org.springframework.stereotype.Component`를 넣고 컴파일 → `Unresolved reference`로 깨져야 정상(§3). 확인 후 되돌린다.

### 2단계 — `infrastructure`

```
catalog/infrastructure/build.gradle.kts
catalog/infrastructure/src/main/kotlin/team/aligner/catalog/infrastructure/ExerciseQueryRepository.kt
catalog/infrastructure/src/main/kotlin/team/aligner/catalog/infrastructure/TargetPoseQueryRepository.kt
```

`build.gradle.kts`: `aligner.kotlin-lib` + `api(project(":catalog:model"))` (§8 표 1행).

**port 시그니처 (읽기 전용, 화면 단위)**
```
ExerciseQueryRepository
  findDetail(exerciseIdentity): ExerciseDetailView?
  findAllByIdentities(List<ExerciseIdentity>): List<ExerciseSummaryView>   // ExerciseContract 전용

TargetPoseQueryRepository
  findDetail(targetPoseIdentity): TargetPoseDetailView?
  findAllByBodyPartCode(bodyPartCode: String): List<TargetPoseSummaryView> // 핀포즈 그리드
```

**판단**
- 쓰기 port를 만들지 않는다(§4, §D4-3).
- `PoseVideoPort`도 이번엔 만들지 않는다(0-1).
- `findAllByBodyPartCode`의 근거는 §D4-2 "자세 그리드는 클라이언트가 `catalog` API로 직접 그린다". 범용 `findAll()`을 만들지 않는다(§4).
- `bodyPartCode`를 `String` 원시값으로 받는다. `screening` 소유 어휘라 `catalog`에 타입을 만들면 경계가 흐려진다(§6).

### 3단계 — `contract`

```
catalog/contract/build.gradle.kts
catalog/contract/src/main/kotlin/team/aligner/catalog/contract/ExerciseContract.kt
catalog/contract/src/main/kotlin/team/aligner/catalog/contract/TargetPoseContract.kt
```

`build.gradle.kts`: `aligner.kotlin-lib`, **의존성 0** (§3 표).

**판단**
- 인터페이스와 발행 DTO를 같은 파일에. 식별자는 원시 `Long`(§7).
- `ExerciseResponse`: `exerciseId, name, defaultSetCount, defaultRepCount, defaultDurationSeconds, metValue, difficulty, contraindications`
  - `default*`는 §D4-4 "비어 있으면 `catalog.exercise`의 기본값을 쓴다"에 필요
  - `metValue`는 코스 칼로리를 "스텝 합으로 계산"(§D4-3)하는 `course`에 필요
  - **근육을 contract에 넣지 않는다.** §7 "`contract`는 외부 통합 전용으로 좁게". `course`·`training`이 근육을 요구한다는 근거가 §D4-4·4-5에 없다
- `TargetPoseResponse`: `targetPoseId, name, imageUrl, bodyPartCode, level`
- `catalog:model`을 의존하지 않는다 — 계약이 도메인 모델을 노출하면 좁게 유지되지 않는다

### 4단계 — `schema` (DDL만)

```
catalog/schema/build.gradle.kts
catalog/schema/src/main/resources/db/catalog/db.changelog-catalog.yaml
catalog/schema/src/main/resources/db/catalog/ddl/001-create-exercise.sql
catalog/schema/src/main/resources/db/catalog/ddl/002-create-target-pose.sql
catalog/schema/src/main/resources/db/catalog/ddl/003-create-muscle.sql
catalog/schema/src/main/resources/db/catalog/ddl/004-create-pose-muscle.sql
catalog/schema/src/main/resources/db/catalog/ddl/005-create-exercise-muscle.sql
catalog/schema/src/main/resources/db/catalog/ddl/006-create-exercise-voice-cue.sql
```

**변경**: `application-api/src/main/resources/db/changelog-master.yaml`에 include 1줄. **`classpath:` 접두사를 붙이지 않는다** — 그 파일 주석이 이유를 적어뒀고 member가 그 형태로 동작 중이다.

| changeset id | 내용 |
| --- | --- |
| `catalog-0001-create-schema` | `CREATE SCHEMA IF NOT EXISTS catalog` |
| `catalog-0002-create-exercise` | `ddl/001` |
| `catalog-0003-create-target-pose` | `ddl/002` |
| `catalog-0004-create-muscle` | `ddl/003` |
| `catalog-0005-create-pose-muscle` | `ddl/004` (FK 때문에 0003·0004 뒤) |
| `catalog-0006-create-exercise-muscle` | `ddl/005` (0002·0004 뒤) |
| `catalog-0007-create-exercise-voice-cue` | `ddl/006` (FK 때문에 `exercise`를 만드는 0002 뒤) |

**판단**
- 컬럼은 §D4-3 정본 그대로. **`cause_code` 없음**, **`pose_checkpoint` 없음**, **칼로리 컬럼 없음**
- **`exercise.voice_guide_script` 컬럼을 만들지 않는다.** 대본은 `catalog` 소유로 돌아왔지만 컬럼이 아니라 `exercise_voice_cue` 테이블이다(§D4-3). 컬럼 → 테이블 전환은 데이터 변환 changeset을 요구하는데 §6이 적용된 changeset 수정을 금지한다
- `exercise_voice_cue`: `cue_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY`, `exercise_id BIGINT NOT NULL` (**FK → `catalog.exercise`**), `display_order SMALLINT NOT NULL`, `offset_seconds INT` (**NULL 허용 — 타임코드 미확정**), `content TEXT NOT NULL`
  - `UNIQUE (exercise_id, display_order)` — 한 운동 안에서 순서가 겹치지 않게 DB가 막는다. `(exercise_id, display_order)`를 PK로 쓰지 않는 이유는 §D4-3에 있다
  - 인덱스는 위 UNIQUE가 겸한다. **`ix_exercise_voice_cue_exercise_id`를 따로 만들지 않는다** — 선두 컬럼이 `exercise_id`라 조회 경로가 그대로 탄다
  - `offset_seconds`에 `CHECK (offset_seconds >= 0)`. 값 집합이 닫혀 있어 감수 대기 사유가 없다
  - **`audio_asset_key`·`source_content`를 지금 넣지 않는다** — §D7-14·13. 둘 다 필요해지면 `ADD COLUMN` changeset 한 줄이라 지금 넣을 이유가 없다(§3 "미리 만들지 않는다")
- `exercise_id`/`target_pose_id`는 `BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY`. `muscle_code`는 자연키 `VARCHAR PRIMARY KEY`
- `ymove_slug`는 **NULL 허용 + UNIQUE**(0-3)
- `role`에 `CHECK (role IN ('STRETCH','STRENGTHEN'))`. PostgreSQL enum 타입을 만들지 않는다 — 값 추가 마이그레이션 비용이 크다
- **`catalog` 내부 FK는 건다** — `pose_muscle → target_pose`, `pose_muscle → muscle`, `exercise_muscle → exercise`, `exercise_muscle → muscle`, `exercise_voice_cue → exercise`. seed 오타를 DB가 잡아준다
- **`body_part_code`에는 FK를 걸지 않는다** — 다른 도메인이다(§6). 값 컬럼 + 인덱스만
- 인덱스: `ix_target_pose_body_part_code`, `ix_pose_muscle_target_pose_id`, `ix_exercise_muscle_exercise_id`
- `created_at`/`updated_at` 없음

**미결 — 이 단계 전에 정할 것 (적용된 changeset은 수정 불가, §6)**

| 항목 | 상태 | 권고 |
| --- | --- | --- |
| `exercise.difficulty` 타입·값 집합 | 정본 미기재 | `VARCHAR(20)`. **값 집합 확인 필요** — 정해지기 전엔 `CHECK` 없이 두고 확정 시 새 changeset |
| `target_pose.level` 타입 | §D7-2 해소됨 (선형 1→2→3) | `SMALLINT NOT NULL` |
| `contraindications` 타입 | §D4-3은 컬럼 하나로 적었으나 "주의사항 **탭**"은 목록으로 읽힘 | 목록이면 `TEXT[]`, 한 문단이면 `TEXT`. **확인 필요** — 나중에 바꾸면 데이터 변환 changeset이 필요 |
| `pose_muscle`/`exercise_muscle` PK 단위 | 같은 근육이 한 자세에서 두 역할을 가질 수 있나 | 권고 `PRIMARY KEY (target_pose_id, muscle_code)`. 양쪽 가능해야 하면 `role`을 PK에 포함. **확인 필요** |
| `body_part_code` 값 어휘 | 저장소에 정본 없음 | **확인 필요.** seed 착수 전 필수 |
| 음성 큐가 매달릴 곳 — 핀포즈가 `exercise` 행으로 존재하는가 | §D7-10이 "준비 동작과 핀포즈에 각각" MET을 준다고 적었고 `met_value`는 `exercise`에만 있다 | `exercise_voice_cue.exercise_id` 하나로 충분. **확인 필요** — 핀포즈가 `exercise` 행이 아니면 `target_pose`용 테이블이 따로 필요해지고, 이건 `ADD COLUMN`으로 못 메운다 |
| `content` 길이 제한 | 정본 미기재 | `TEXT`. 큐 문장 길이 상한을 감수 전에 정할 근거가 없다 |

**검증**: 컴파일 대상이 아니라 **빌드로는 검증되지 않는다.** 6단계 통합 테스트가 이 changelog를 실제로 돌리는 첫 지점이다.

### 5단계 — `service` + 단위 테스트

```
catalog/service/build.gradle.kts
catalog/service/src/main/kotlin/team/aligner/catalog/service/ExerciseQueryService.kt
catalog/service/src/main/kotlin/team/aligner/catalog/service/TargetPoseQueryService.kt
catalog/service/src/main/kotlin/team/aligner/catalog/service/ExerciseContractImpl.kt
catalog/service/src/main/kotlin/team/aligner/catalog/service/TargetPoseContractImpl.kt
catalog/service/src/main/kotlin/team/aligner/catalog/service/CatalogServiceAutoConfiguration.kt
catalog/service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
catalog/service/src/test/kotlin/team/aligner/catalog/service/ExerciseQueryServiceTest.kt
catalog/service/src/test/kotlin/team/aligner/catalog/service/TargetPoseQueryServiceTest.kt
catalog/service/src/test/kotlin/team/aligner/catalog/service/ExerciseContractImplTest.kt
```

`build.gradle.kts`: `aligner.kotlin-boot` + `api(":catalog:model")` + `api(":catalog:contract")` + `implementation(":catalog:infrastructure")`.

**판단**
- **`*CommandService.kt`가 생기면 §D4-3 위반이다**
- `@Transactional(readOnly = true)`를 **클래스에** 붙인다 (allopen/CGLIB, §10 5단계)
- 없는 식별자 → `ExerciseNotFoundException` / `TargetPoseNotFoundException`
- `findAllByBodyPartCode`는 빈 리스트를 정상으로 반환(예외 아님)
- contract 구현체는 `internal`, 위임만 하고 판단을 두지 않는다(§7)
- **`ExerciseContractImpl.findAllByIds(emptyList())`는 DB를 치지 않고 `emptyList()`를 반환한다.** `IN (:ids)`에 빈 리스트를 넘기면 SQL이 깨진다. member에 없던 새 위험
- AutoConfiguration이 Bean 4개 등록

**검증**: `./gradlew :catalog:service:test`
- 상세 조회 정상 / 없는 식별자 → 예외 / 부위 코드 조회 / 빈 목록도 정상
- `ExerciseContractImpl` 필드 매핑 회귀 방지
- **`findAllByIds(emptyList())` → port 미호출** (`verify(exactly = 0)`)

### 6단계 — `repository-jdbc` + 통합 테스트

```
catalog/repository-jdbc/build.gradle.kts
catalog/repository-jdbc/src/main/kotlin/team/aligner/catalog/repository/jdbc/ExerciseQueryRepositoryImpl.kt
catalog/repository-jdbc/src/main/kotlin/team/aligner/catalog/repository/jdbc/TargetPoseQueryRepositoryImpl.kt
catalog/repository-jdbc/src/main/kotlin/team/aligner/catalog/repository/jdbc/CatalogRepositoryAutoConfiguration.kt
catalog/repository-jdbc/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
catalog/repository-jdbc/src/integrationTest/kotlin/team/aligner/catalog/repository/jdbc/bootstrap/CatalogRepositoryTestApplication.kt
catalog/repository-jdbc/src/integrationTest/kotlin/team/aligner/catalog/repository/jdbc/ExerciseQueryRepositoryIntegrationTest.kt
catalog/repository-jdbc/src/integrationTest/kotlin/team/aligner/catalog/repository/jdbc/TargetPoseQueryRepositoryIntegrationTest.kt
catalog/repository-jdbc/src/integrationTest/resources/application.yml
```

`build.gradle.kts`: `aligner.repository-jdbc` + `api(":catalog:model")` + `implementation(":catalog:infrastructure")` + `testImplementation(":catalog:schema")`. §3 — schema는 `testImplementation`으로만. `aligner.kotlin-lib`이 `integrationTestImplementation.extendsFrom(testImplementation)`을 걸어둔다.

**판단 — member와 가장 크게 갈리는 지점**
- **`*Entity.kt`도, `CrudRepository`도, `@EnableJdbcRepositories`도 없다.** 전부 `JdbcClient` 직결(§4)
  - **복합키 문제가 사라진다** — `pose_muscle`·`exercise_muscle`은 복합 PK라 Spring Data JDBC로는 하위 엔티티로 감싸야 하는데, 읽기만 하므로 조인 SQL로 끝난다
  - **member가 겪은 `BeanDefinitionOverrideException`이 원천적으로 안 생긴다.** `bootstrap/` 관례는 일관성 때문에 유지
- SQL은 전부 schema-qualified (`FROM catalog.exercise`). 빠지면 `public`을 친다(§6)
- **N+1 회피**: 운동 상세는 "본체 1쿼리 + 근육 1쿼리 + 음성 큐 1쿼리" **3회**. 근육과 큐를 한 SQL에 조인하면 카티션 곱이 되므로 나눈다. 목록 조회는 근육도 큐도 안 실으므로 1쿼리. 근육·큐 둘 다 `ORDER BY display_order`
- `role` 문자열 → `MuscleRole` 변환은 이 어댑터에서만
- AutoConfiguration은 `JdbcClient`만 받아 Bean 2개 등록

**통합 테스트**
- `application.yml`은 `classpath:db/catalog/db.changelog-catalog.yaml` + `liquibase-schema: public`
- **픽스처는 seed가 아니라 테스트가 `JdbcClient`로 직접 넣는다**
- 검증 항목:
  1. schema 격리 — `to_regclass('public.exercise'|'public.target_pose'|'public.muscle'|'public.exercise_voice_cue')`이 전부 null
  2. changelog 적용 — `public.databasechangelog`에 `catalog-0001` … `catalog-0007` 7행
  3. `findDetail` — 근육이 `display_order` 순, `role`이 `MuscleRole`로 매핑
  4. `findDetail` — 없는 식별자 → null
  5. `findAllByIdentities` — 존재하지 않는 id가 섞여도 예외 없이 나머지 반환
  6. `findAllByBodyPartCode` — 다른 부위가 섞여 나오지 않음
  7. `role` CHECK — `'INVALID'` insert → `DataIntegrityViolationException`
  8. `ymove_slug` UNIQUE — 중복 → 예외, **NULL 두 개는 허용**(0-3 근거를 테스트로 고정)
  9. `catalog` 내부 FK — 없는 `muscle_code`로 insert → 예외
  10. **음성 큐 정렬** — 큐를 `display_order` 역순으로 insert해도 `findDetail`이 `display_order` 오름차순으로 돌려준다. insert 순서가 아니라 컬럼이 순서를 정한다는 것을 고정한다
  11. **`offset_seconds` NULL 허용** — 타임코드 없이 insert되고, View의 `offsetSeconds`가 `null`로 매핑된다. 값이 있는 큐와 없는 큐가 한 운동에 섞여도 통과한다(§D4-3의 확장 경로를 테스트로 고정)
  12. **음성 큐 FK** — 없는 `exercise_id`로 insert → 예외
  13. **`(exercise_id, display_order)` UNIQUE** — 같은 운동에 같은 순서 → 예외, **다른 운동이면 같은 `display_order`가 허용**된다
  14. **큐가 없는 운동** — `findDetail`이 빈 리스트를 돌려준다(예외 아님). seed가 아직 안 들어간 상태가 정상 경로다(0-2)

**검증**: `./gradlew :catalog:repository-jdbc:integrationTest` (**Docker 필요**). changelog와 DDL이 실제로 도는 유일한 자동 검증 지점.

### 7단계 — `api`

```
catalog/api/build.gradle.kts
catalog/api/src/main/kotlin/team/aligner/catalog/api/ExerciseController.kt
catalog/api/src/main/kotlin/team/aligner/catalog/api/TargetPoseController.kt
catalog/api/src/main/kotlin/team/aligner/catalog/api/CatalogApiAutoConfiguration.kt
catalog/api/src/main/kotlin/team/aligner/catalog/api/dto/ExerciseDetailResponse.kt
catalog/api/src/main/kotlin/team/aligner/catalog/api/dto/TargetPoseDetailResponse.kt
catalog/api/src/main/kotlin/team/aligner/catalog/api/dto/TargetPoseSummaryResponse.kt
catalog/api/src/main/kotlin/team/aligner/catalog/api/dto/MuscleResponse.kt
catalog/api/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

`build.gradle.kts`: `aligner.boot-mvc` + `implementation(":catalog:service")` + `implementation(":catalog:model")`. **`repository-jdbc`를 넣지 않는다** — §8이 "못 막는다"고 명시한 자리.

- **`support-web` 의존을 넣지 않는다** (member와 다른 점). catalog 엔드포인트는 회원 식별자를 안 쓰므로 `@AuthenticationPrincipal`이 등장하지 않는다. → **확인 필요**: 개인화가 필요해지면 그때 추가

**엔드포인트 (정본에 경로 명시가 없어 여기서 정함)**

| 메서드·경로 | 근거 |
| --- | --- |
| `GET /catalog/target-poses?bodyPartCode={code}` | §D4-2 "자세 그리드는 클라이언트가 `catalog` API로 직접 그린다" |
| `GET /catalog/target-poses/{targetPoseId}` | §D4-3 Query |
| `GET /catalog/exercises/{exerciseId}` | §D4-3 Query |

- **범용 목록(`GET /catalog/exercises`)을 만들지 않는다** — 화면 근거 없음(§4)
- **근육 단독 엔드포인트를 만들지 않는다** — §D4-3 Query는 "자세·운동별 근육"이지 근육 마스터 조회가 아니다
- 응답에 `metValue`를 싣고 kcal은 계산하지 않는다. → **확인 필요**: 칼로리를 클라이언트가 계산할지 `course`가 계산할지. `catalog`가 못 하는 것은 확정(몸무게는 `member` 소유)

**Bean 조립**: `CatalogApiAutoConfiguration`에서 컨트롤러 2개를 `@Bean` 등록. **빠지면 기동은 성공하고 호출만 404**다(§5 — 원인 찾기가 가장 어려운 실수).

### 8단계 — `application-api` 조립 + 기동 검증

`application-api/build.gradle.kts`에 3줄:
```
implementation(project(":catalog:api"))
implementation(project(":catalog:repository-jdbc"))
implementation(project(":catalog:schema"))
```

**`catalog:contract`는 추가하지 않는다** (member와 다른 점). member가 명시한 것은 §9가 `support-web` + `member:contract` + `adapter-auth`를 함께 조립하라고 못박았기 때문이다. `catalog:contract`의 소비자(`course/adapter-catalog`)는 아직 없고, 구현체 Bean은 `catalog:api → catalog:service` 런타임 전이로 등록된다. §10 8단계가 요구하는 것도 `api`·`repository-jdbc`·`schema` 셋이다.

**기동 검증** (`JWT_SECRET=… DB_PASSWORD=… ./gradlew :application-api:bootRun`)
1. `\dn` → `catalog` 존재, `\dt catalog.*` → 테이블 6개, `\dt public.*`에 도메인 테이블이 **없어야** 한다
2. 토큰 없이 `GET /catalog/exercises/1` → **401**
3. JWT로 `GET /catalog/exercises/{id}` → 200 또는 404, 바디가 `ApiErrorResponse` 포맷. **500이나 "No mapping" 404면 AutoConfiguration `@Bean` 누락**
4. `GET /catalog/target-poses?bodyPartCode=…` → 목록

> 3·4는 JWT가 필요하다. 카카오 토큰이 없으면 `JWT_SECRET`으로 HS256 토큰을 직접 만든다(코드 변경 없음). 그것도 불가하면 **"미검증"으로 PR 본문에 명시한다** — `SecurityConfig`에 `permitAll`을 추가해 우회하지 않는다.

**CI 명령**: `./gradlew ktlintCheck build integrationTest`

---

## 3. 커밋 분할

| # | 메시지 | 범위 |
| --- | --- | --- |
| 1 | `feat: catalog 도메인 모델과 조회 out-port 추가` | `model`, `infrastructure`, include 2줄 |
| 2 | `feat: catalog 운동·자세 조회 계약 모듈 추가` | `contract`, include 1줄 |
| 3 | `feat: catalog 스키마 changelog 추가` | `schema`, `changelog-master.yaml`, include 1줄 |
| 4 | `feat: catalog 운동·자세 조회 서비스 추가` | `service` + AutoConfiguration, include 1줄 |
| 5 | `test: catalog 조회 서비스 단위 테스트 추가` | `service/src/test` |
| 6 | `feat: catalog JDBC 조회 리포지토리 구현 추가` | `repository-jdbc` + AutoConfiguration, include 1줄 |
| 7 | `test: catalog 리포지토리 통합 테스트 추가` | `repository-jdbc/src/integrationTest` |
| 8 | `feat: catalog 운동·자세 조회 API 추가` | `api` + AutoConfiguration, include 1줄 |
| 9 | `chore: application-api 에 catalog 모듈 조립` | `application-api/build.gradle.kts` |

`gradle/libs.versions.toml`과 `build-logic`은 **손대지 않는다.** 0-1로 `spring-web`이 빠져 새 라이브러리가 없다. member가 카탈로그를 3곳 고쳐야 했던 것과 대비된다.

---

## 4. 확인 필요 목록

### 착수를 막는 것

| # | 항목 | 언제까지 |
| --- | --- | --- |
| 1 | `contraindications` — `TEXT`인가 `TEXT[]`인가 | **4단계 전.** 적용된 changeset은 수정 불가(§6) |
| 2 | `exercise.difficulty` 값 집합 | 4단계 전 (확정 전엔 `CHECK` 없이) |
| 3 | `pose_muscle`/`exercise_muscle` PK — 한 근육이 두 역할 가능? | 4단계 전 |
| 4 | `body_part_code` 값 어휘의 정본 | seed 이슈 전 (이번 범위는 안 막힘) |
| 5 | §D7-4·5·6 (YMove 장애·캐시 TTL·slug/좌우 분리) | `adapter-ymove` 후속 이슈 전 |
| 6 | 칼로리를 누가 계산하나 | `course` 착수 전 (이번 범위는 안 막힘) |
| 7 | seed 감수 데이터 출처 | seed 이슈 전 |
| 8 | **음성 큐가 매달릴 곳 — 핀포즈가 `exercise` 행인가** | **4단계 전.** 아니면 `target_pose`용 테이블이 하나 더 필요하고, 이건 컬럼 추가로 못 메운다 |
| 9 | §D7-12·13 (번역 전처리 위치·원문 변경 감지) | seed 이슈 전 (4단계는 안 막힘 — DDL이 산출 방식을 모른다) |
| 10 | §D7-15 (타임코드 확정) | seed 이슈 전 **권장**. 4단계는 안 막힘 — `offset_seconds`가 nullable이고 확정 시 `UPDATE` changeset이다. seed를 두 번 만들지 않으려는 것뿐 |

**§D7-14(TTS 산출물이 텍스트인가 오디오인가)는 이 목록에 없다.** 오디오로 정해져도 `audio_asset_key` `ADD COLUMN` changeset 한 줄이라 4단계를 막지 않는다. 8·9·10과 갈리는 기준은 "나중에 답이 바뀌면 데이터 변환이 필요한가"다(§6).

### 구현 중 즉시 드러나는 것 (미리 답할 필요 없음)

| 항목 | 드러나는 지점 |
| --- | --- |
| `@EnableJdbcRepositories` 없이 `JdbcClient` Bean 주입 | 6단계. member 통합 테스트가 이미 autowire 중이라 사실상 확인됨 |
| `catalog` changelog가 실제로 도는가 | 6단계 |
| ktlint 라인 길이 위반 | `./gradlew ktlintCheck` |

### 정본 문서와의 표기 차이 (문서 갱신은 별도 승인, §D7-11)

- §D4-3 "애그리거트 `Exercise` `TargetPose` `Muscle`" — 이번 구현은 애그리거트 클래스를 만들지 않는다(1절 #3). 문서를 고치지 않고 PR 본문에 근거를 적는다
- §D5 "catalog: … adapter-ymove" — 이번 include는 7개다. 최종 계획과의 차이를 PR 본문에 명시한다
