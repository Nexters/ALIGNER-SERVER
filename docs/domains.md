# 도메인 분할 — 확정본

Aligner 서버의 도메인 경계 확정본. 2026-07-27 결정.

`docs/architecture.md`가 **도메인 하나가 어떤 모양인지**를 정의하고, 이 문서는 **그 골격 위에
어떤 도메인을 몇 개 놓을지**를 정의한다. 계층 규칙·Bean 조립·`api`/`implementation` 판단이
필요하면 항상 `docs/architecture.md`를 읽는다. 여기서는 그 값을 복제하지 않는다.

## 1. 확정된 결정

| 항목 | 결정 |
| --- | --- |
| 도메인 수 | **5개** — `member` `screening` `catalog` `course` `training` |
| PostgreSQL schema | 도메인명과 동일 (`member` `screening` `catalog` `course` `training`) |
| 도메인 간 의존 | **단방향** — `catalog` ← `course` ← `training`, `screening` ← `course` |
| 마스터 데이터 소유 | 운동·자세·자세 포인트는 `catalog`, 스크리닝 문항·분기 규칙은 `screening`, 처방 규칙은 `course` |
| 도장(`Stamp`) | `course` 소유 (달성 판단은 `course`, 수행 기록은 `training`) |
| 총 모듈 수 | **44개** — 도메인 40 + 루트 4 |
| 외부 시스템 | **YMove** — 영상·음성 대본. `catalog`만 접근한다 (§4-3-1) |

### 도메인 지도

| 도메인 | 소유하는 것 | MVP(`AGENTS.md` §3) |
| --- | --- | --- |
| `member` | 회원, 카카오 식별자, 프로필 | 1, 8 |
| `screening` | 부위, 스크리닝 문항·선택지, 응답→원인 분기 규칙, 회원 응답과 판별된 원인 | 2, 3(앞) |
| `catalog` | 보강 운동·목표 자세·자세 포인트, YMove 연동(영상·음성 대본) | 5, 7(정의) |
| `course` | 원인별 코스 템플릿, 회원별 처방 코스·스텝·진행 상태, 보강 편성 규칙, 도장·해금 | 3(뒤), 4, 7 |
| `training` | 세션 시작·완료·수행 기록, 자세 포인트 체크 응답 기록 | 6 |

### 핵심 도메인 루프(`AGENTS.md` §2)와의 대응

```text
① 부위 선택                     screening
② 자가 스크리닝                 screening
③ 응답 분기 → ④ 원인 판별       screening
⑤ 원인별 코스 처방              course  (원인은 screening:contract로 읽음)
⑥ 세션 수행 → 완료 기록         training
⑦ 자세 포인트 체크              training 이 기록 → course 로 push
   ├ 전 포인트 통과 → 도장·해금  course
   └ 미달 → 보강 자동 편성       course
```

---

## 2. 왜 이렇게 나눴는가

### `catalog`를 따로 두는 이유

운동·자세·자세 포인트는 **요가 지도자 감수 전 마스터 데이터**이고 쓰기가 없다
(`docs/architecture.md` §6). `course`와 `training` 두 도메인이 모두 읽는다.
YMove 연동이 붙은 뒤에도 이 성질은 유지된다 — `catalog`는 YMove를 **읽기만** 한다(§4-3-1).
덕분에 외부 시스템을 아는 도메인이 하나로 묶인다는 이점이 하나 더 생겼다.
한 도메인에 얹으면 그 도메인의 `contract`가 "처방 계약 + 운동 카탈로그 조회"로 넓어지는데,
§7은 `contract`를 좁게 만들라고 한다. Command가 없어 모듈 수 대비 실제 코드량은 작다.

### `course`가 `training`을 의존하지 않는 이유 — 기록은 training, 판단은 course

⑦의 "미달 포인트 → 보강 동작 자동 편성"은 이 서비스의 리텐션 엔진이다. 이걸 `course`가
`training`을 조회하는 pull로 짜면 `course ↔ training` 양방향이 된다.

그래서 **세션 완료·체크 결과를 `training`이 `course`로 push**한다. `training`은 "무슨 일이
있었나"만 기록하고, 도장·해금·재편성 **판단은 전부 `course`**가 한다. 재편성 타이밍도
"다음날 조회 시"보다 "세션 완료 직후"가 자연스럽다.

### `screening`이 원인까지, `course`가 처방부터

MVP 기능 3("스크리닝 응답에 따른 보강 영역과 목표 자세 매핑")이 두 도메인에 걸친다.
경계는 **원인(`Cause`)**이다. `screening`은 응답을 원인 코드로 바꾸는 데까지, `course`는
원인 코드를 코스로 바꾸는 데부터 책임진다. 원인 코드는 값으로만 넘어가고 FK는 걸지 않는다.

처방 요청은 `course`가 받고, 그 회원의 최신 원인을 `screening:contract`로 읽는다.
클라이언트가 원인 코드를 들고 `course`를 다시 호출하는 방식은 원인 위조가 가능해서 쓰지 않는다.

---

## 3. 도메인 간 통신

`docs/architecture.md` §7 적용. 요청 도메인이 자기 `infrastructure`에 port를 정의하고,
연결은 `adapter-*` 모듈이 대상 도메인의 `contract`만 보고 구현한다.

```text
support-web ──→ member:contract           (adapter-auth, §9)

course   ──→ screening:contract           최신 원인 조회
course   ──→ catalog:contract             운동·자세 조회, 존재 검증
catalog  ──→ YMove (외부 HTTP)            영상 URL·음성 대본, 48시간 만료 (§4-3-1)
training ──→ course:contract              스텝 구성 조회 / 완료·체크 결과 push
training ──→ catalog:contract             세션 중 운동 상세·체크포인트 조회
```

순환이 없다. `training` → `course` → `screening`·`catalog` 한 방향으로만 흐른다.

### 계약 시그니처 초안

`contract`는 통합 전용으로 좁게 만든다. 식별자는 원시 타입으로 받고 발행 DTO로 반환한다.
구현체는 `internal`로 대상 도메인 `service`에 두고 Bean도 거기서 등록한다(§7).

```kotlin
// member/contract
interface MemberAuthContract {
    fun findOrRegisterByKakao(command: KakaoMemberCommand): AuthenticatedMemberResponse
}

// screening/contract
interface ScreeningResultContract {
    fun findLatestCause(memberId: Long, bodyPartCode: String): LatestCauseResponse?
}

// catalog/contract
interface ExerciseContract {
    fun findAllByIds(exerciseIds: List<Long>): List<ExerciseResponse>
}
interface TargetPoseContract {
    fun findById(targetPoseId: Long): TargetPoseResponse?
    fun findCheckpoints(targetPoseId: Long): List<PoseCheckpointResponse>
}

// course/contract
interface CourseStepContract {
    fun findStep(courseId: Long, stepOrder: Int): CourseStepResponse?
}
interface CourseProgressContract {
    fun completeStep(command: CompleteStepCommand)
    fun applyCheckpointResult(command: CheckpointResultCommand)
}
```

### port ↔ adapter 대응

| 요청 도메인 port (`infrastructure`) | adapter 모듈 | 대상 contract |
| --- | --- | --- |
| `course` — `CauseLookupPort` | `course/adapter-screening` | `screening:contract` |
| `course` — `ExerciseCatalogPort` `TargetPoseCatalogPort` | `course/adapter-catalog` | `catalog:contract` |
| `training` — `CourseStepPort` `CourseProgressPort` | `training/adapter-course` | `course:contract` |
| `training` — `ExerciseDetailPort` `PoseCheckpointPort` | `training/adapter-catalog` | `catalog:contract` |
| `support-web` — `AuthMemberPort` | `member/adapter-auth` | `member:contract` |

`training`은 아무도 읽지 않으므로 **`contract`를 만들지 않는다.** 미리 만들지 않는 원칙(§3)이다.

---

## 4. 도메인별 상세

### 4-1. `member`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Member` (루트) |
| Command | 카카오 가입, 프로필 수정 |
| Query | 프로필 조회 |
| 모듈 | 기본 6 + `contract` + `adapter-auth` = **8** |

```text
member.member    member_id, kakao_id(uk), nickname, profile_image_url,
                 created_at, updated_at
```

카카오 로그인의 웹 계층은 `support-web`, 회원 조회·가입은 `member`가 갖는다
(`docs/architecture.md` §9). `adapter-auth`가 빠지면 기동이 실패해야 정상이다.

### 4-2. `screening`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `ScreeningResult` (루트) + `ScreeningAnswer` (자식) |
| 마스터 | `BodyPart` `ScreeningQuestion` `ScreeningOption` `CauseRule` `Cause` — 전부 seed |
| Command | 스크리닝 응답 제출 → 원인 판별 → 결과 저장 |
| Query | 부위 목록, 부위별 문항, 회원의 최신 결과 |
| 모듈 | 기본 6 + `contract` = **7** |

```text
screening.body_part            body_part_code(pk), name, display_order          [seed]
screening.cause                cause_code(pk), name, body_part_code, description [seed]
screening.screening_question   question_id(pk), body_part_code, content, display_order [seed]
screening.screening_option     option_id(pk), question_id, content, display_order [seed]
screening.cause_rule           rule_id(pk), question_id, option_id, cause_code   [seed]
screening.screening_result     result_id(pk), member_id, body_part_code, cause_code, created_at
screening.screening_answer     answer_id(pk), result_id, question_id, option_id
```

`cause_rule`이 **응답 조합 → 원인** 분기표다. `AGENTS.md` §2의 매핑 예시가 여기 들어간다.
감수 결과가 바뀌면 이 seed changeset만 새로 쌓으면 되고 코드는 손대지 않는다.

`member_id`는 값 컬럼이며 FK를 걸지 않는다(§6).

### 4-3. `catalog`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Exercise` `TargetPose`(+ `PoseCheckpoint` 자식) |
| Command | **없음** — `CommandService`·쓰기 port를 만들지 않는다(§4) |
| Query | 운동 상세, 자세 상세, 자세별 체크포인트 |
| 외부 의존 | **YMove** — 영상·음성 대본의 정본 (§4-3-1) |
| 모듈 | 기본 6 + `contract` + `adapter-ymove` = **8** |

```text
catalog.exercise         exercise_id(pk), ymove_slug(uk), name,
                         default_set_count, default_rep_count                      [seed]
catalog.target_pose      target_pose_id(pk), ymove_slug(uk), name, image_url       [seed]
catalog.pose_checkpoint  checkpoint_id(pk), target_pose_id, content, display_order [seed]
```

`catalog`는 순수 카탈로그다. "어떤 원인에 어떤 운동을 쓰는가"는 처방 규칙이므로 `course`가 갖는다.
여기에 `cause_code`를 두지 않는다.

`infrastructure`에는 `QueryRepository`와 **`PoseVideoPort`**(아래)만 둔다. 쓰기 port를 세트로
찍지 않는다.

#### 4-3-1. YMove 연동 — `catalog`가 순수 seed 도메인이 아닌 이유

영상과 음성은 YMove를 쓴다. **`videos[].videoUrl`이 48시간 만료라 DB에 넣을 수 없다.**
seed changeset에 박으면 이틀 뒤 전부 죽는다. 그래서 `catalog`는 "어떤 YMove 자세를 쓰는가"와
감수로 덧붙인 것만 소유하고, 재생에 필요한 값은 요청 시점에 YMove에서 읽는다.

경계는 **`ymove_slug`**다. 우리 seed는 slug만 들고, 나머지는 YMove가 정본이다.

| 값 | 소유 | 이유 |
| --- | --- | --- |
| `ymove_slug` | `catalog` seed | 어떤 자세를 쓸지는 우리 감수 결정이다 |
| `videoUrl` `thumbnailUrl` | YMove (매 요청) | 48시간 만료. 캐시 TTL을 그보다 짧게 둔다 |
| `instructions` | YMove | 음성 큐잉 대본. `voice_guide_script` 컬럼을 없앤 이유다 |
| `title` `description` `difficulty` `videoDurationSecs` `muscleGroup` | YMove | 중복 저장하지 않는다 |
| `name` | `catalog` seed | YMove `title`을 우리 표현으로 덮어야 할 때만 쓰는 override |
| `default_set_count` `default_rep_count` | `catalog` seed | YMove에 없다. 감수 대상이다 |
| `PoseCheckpoint` | `catalog` seed | **YMove `importantPoints`와 다르다.** 아래 참고 |

**`importantPoints`를 `PoseCheckpoint`로 쓰지 않는다.** YMove의 것은 수행 중 안내(주의사항·정렬)고,
`PoseCheckpoint`는 완수 판정 대상이다. `course.reinforcement_rule`이 `checkpoint_id`로 보강 운동을
매핑하므로 식별자가 안정적이어야 하고, 감수 결과에 따라 우리가 통제해야 한다. 감수 시 YMove의
`importantPoints`를 출발점으로 삼는 것은 자유다.

**`exerciseType`에 `yoga` 필터가 필요하다.** YMove는 요가 외 콘텐츠도 담고 있다. 필터는 adapter가 건다.

##### port와 모듈

`docs/architecture.md` §7의 `adapter-*`는 *다른 도메인* 소비용으로 정의돼 있다. YMove는 외부
시스템이라 새 범주지만, 방향(요청 도메인이 자기 `infrastructure`에 port를 정의하고 adapter가
구현)은 같으므로 같은 규칙을 따른다.

```text
catalog/infrastructure   PoseVideoPort            (out-port, 순수 Kotlin)
catalog/adapter-ymove    PoseVideoPort 구현체      (RestClient, internal)
```

```kotlin
// catalog/infrastructure
interface PoseVideoPort {
    fun findPlayback(ymoveSlugs: List<String>): Map<String, PoseVideoPlayback>
}
```

목록 조회에서 N번 호출하지 않도록 **slug 리스트를 한 번에 받는다.**

##### 장애와 성능

- **YMove가 죽으면 세션이 안 돈다.** 영상 없이 진행할 수 없으므로 fallback이 없다. 타임아웃과
  실패 시 사용자에게 보일 메시지를 `course`·`training` 착수 전에 정해야 한다(§7-5).
- `training`이 세션 중 운동 상세를 `catalog:contract`로 읽는데, 그 응답에 재생 URL이 실리면
  스텝마다 YMove를 친다. 캐시 위치와 TTL을 정해야 한다(§7-6).

### 4-4. `course`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Course`(루트) + `CourseStep` + `CourseStepExercise`, `Stamp`(루트) |
| 마스터 | `CourseTemplate` + `TemplateStep`, `ReinforcementRule`, 자세 사다리 — seed |
| Command | 코스 처방, 스텝 완료, 체크 결과 반영(도장·해금·보강 편성) |
| Query | 코스 상세(스텝+운동), 진행도, 획득한 도장 |
| 모듈 | 기본 6 + `contract` + `adapter-screening` + `adapter-catalog` = **9** |

```text
course.course_template          template_id(pk), cause_code, target_pose_id, name,
                                unlock_required_target_pose_id(null)                  [seed]
course.template_step            template_step_id(pk), template_id, step_order      [seed]
course.template_step_exercise   template_step_id, exercise_id, display_order       [seed]
course.reinforcement_rule       rule_id(pk), checkpoint_id, exercise_id            [seed]

course.course                   course_id(pk), member_id, template_id, cause_code,
                                target_pose_id, current_step_order, status, created_at
course.course_step              course_step_id(pk), course_id, step_order, status
course.course_step_exercise     course_step_exercise_id(pk), course_step_id, exercise_id,
                                display_order, source, duration_seconds, set_count
course.stamp                    stamp_id(pk), member_id, target_pose_id, acquired_at
```

- `course_step_exercise.source`가 `TEMPLATE` / `REINFORCEMENT`를 구분한다. ⑦로 자동 편성된
  보강 동작을 이걸로 식별한다.
- `duration_seconds` `set_count`는 nullable이며 비어 있으면 `catalog.exercise`의 기본값을 쓴다
  (§7 열린 질문 참고).
- `reinforcement_rule`이 **미달 체크포인트 → 보강 운동** 매핑이다. 감수 대상 seed다.
- `exercise_id` `target_pose_id` `checkpoint_id` `member_id`는 전부 값 컬럼이고 FK가 없다.

### 4-5. `training`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Session`(루트) + `SessionExerciseRecord`, `CheckpointResult`(루트) + 항목 |
| Command | 세션 시작, 세션 완료, 자세 포인트 체크 결과 기록 |
| Query | 세션 기록 조회 |
| 모듈 | 기본 6 + `adapter-course` + `adapter-catalog` = **8** |

```text
training.session                 session_id(pk), member_id, course_id, step_order,
                                 started_at, completed_at, status
training.session_exercise_record  record_id(pk), session_id, exercise_id,
                                 completed, performed_duration_seconds
training.checkpoint_result       result_id(pk), session_id, member_id, target_pose_id, created_at
training.checkpoint_result_item  item_id(pk), result_id, checkpoint_id, passed
```

세션 완료와 체크 결과 저장 이후 `CourseProgressPort`로 `course`에 밀어넣는다.
**`training`에는 도장·해금·재편성 판단이 없다.** 그 로직이 여기 생기면 잘못 나눈 것이다.

---

## 5. 예정 모듈 구성 — 향후 `settings.gradle.kts` include 대상

```text
build-logic (includeBuild)
support-core
support-web
application-api

member:      model infrastructure service repository-jdbc api schema contract adapter-auth
screening:   model infrastructure service repository-jdbc api schema contract
catalog:     model infrastructure service repository-jdbc api schema contract adapter-ymove
course:      model infrastructure service repository-jdbc api schema contract
             adapter-screening adapter-catalog
training:    model infrastructure service repository-jdbc api schema
             adapter-course adapter-catalog
```

패키지 루트는 `team.aligner.{domain}`이다(§10).

현재 `settings.gradle.kts`가 include 하는 것은 `support-core` · `support-web` ·
`application-api` 뿐이고 `build-logic`은 `includeBuild` 대상이다. 위 목록의 도메인 모듈은
아직 없다. 도메인 구현 이후 `application-api`는 5개 도메인의 `api` · `repository-jdbc` ·
`schema` · `adapter-*`와 `member:contract` · `support-web` · `support-core`를 조립한다.

### 착수 순서

의존 방향의 말단부터 만든다. 앞 도메인이 없으면 뒤 도메인의 adapter를 컴파일할 수 없다.

```text
1. build-logic + support-core + support-web + application-api  (골격)
2. member  (+ contract, adapter-auth)   — 인증이 없으면 나머지 API를 붙일 수 없다
3. catalog                              — 의존이 없고 seed만 있어 가장 가볍다
4. screening
5. course    (adapter-screening, adapter-catalog)
6. training  (adapter-course, adapter-catalog)
```

---

## 6. 스키마 계획

`docs/architecture.md` §6 적용.

- schema 5개. 각 도메인 changelog 첫 changeset에 `CREATE SCHEMA IF NOT EXISTS {domain}`.
- 모든 DDL은 schema-qualified. 엔티티에 `@Table(schema = "{domain}", ...)`, `JdbcClient` SQL도 동일.
- 루트 `changelog-master.yaml`에 5개 include를 추가한다.
- **도메인 간 FK 없음.** `member_id` `exercise_id` `target_pose_id` `checkpoint_id` `cause_code`는
  전부 값 컬럼이다. 존재 검증이 필요하면 port로 한다.
- seed는 `schema/seed/`의 changeset으로 넣는다. 감수 전 데이터가 들어가는 곳은 넷이다.

| seed | 위치 | 감수 대상 |
| --- | --- | --- |
| 부위·원인·스크리닝 문항·분기 규칙 | `screening/schema/seed/` | ✅ |
| 운동·자세·자세 포인트 | `catalog/schema/seed/` | ✅ |
| 원인별 코스 템플릿·스텝 구성 | `course/schema/seed/` | ✅ |
| 미달 체크포인트 → 보강 운동 규칙 | `course/schema/seed/` | ✅ |

이 넷 중 어느 것도 코드에 하드코딩하지 않는다. 하드코딩은 그 자체로 `[필수]` 지적이다.

---

## 7. 열린 질문 — 구현 전에 답해야 한다

1. **운동의 수행 시간·세트를 누가 정하는가.** 위 설계는 `catalog.exercise`에 기본값을 두고
   `course.course_step_exercise`가 필요할 때만 덮어쓰는 방식이다. 같은 운동이 코스마다 다른
   세트를 갖는 경우가 실제로 있는지 기획·감수 확인이 필요하다. 없으면 override 컬럼을 뺀다.
2. **자세 해금 사다리의 표현.** 위 설계는 `course_template.unlock_required_target_pose_id` 한 컬럼으로
   선행 자세를 표현한다. 사다리가 분기하거나 선행 조건이 복수면 별도 테이블이 필요하다.
3. **`Course` 용어.** `AGENTS.md` §2 용어집은 코스를 "원인별 처방"으로 적었으나, ⑦ 때문에 실제로는
   회원별로 재편성되는 인스턴스다. 이 문서는 마스터를 `CourseTemplate`, 회원 인스턴스를 `Course`로
   구분한다. 용어집에 `CourseTemplate` 행을 추가해야 한다.
4. **YMove 장애 시 무엇을 보여주는가.** 영상 없이는 세션을 진행할 수 없어 fallback이 없다.
   타임아웃 값, 재시도 여부, 사용자에게 보일 메시지를 `catalog` 착수 전에 정해야 한다.
5. **재생 URL 캐시 위치와 TTL.** 48시간 만료이므로 그보다 짧아야 한다. 코스 상세 조회처럼
   여러 자세를 한 번에 그리는 화면에서 매번 YMove를 치지 않도록 `PoseVideoPort` 구현체 안에
   둘지, 그 위에 둘지 정해야 한다. `thumbnailUrl`도 같이 만료되는지 YMove 확인이 필요하다.
6. **YMove의 `slug`가 안정적인가.** 우리 seed가 `ymove_slug`를 유일한 연결 고리로 쓰므로,
   YMove 쪽에서 slug가 바뀌면 해당 운동·자세가 통째로 깨진다. 불안정하면 내부 id를 함께
   저장해야 한다.
7. **고민 유형(`Concern`)은 P1**이다(`AGENTS.md` §3). `screening.body_part` 아래 자리만 비워두고
   지금 만들지 않는다. 추가될 때 `screening.concern` + `cause_rule`에 `concern_code`가 붙는다.

## 8. 이 분할에서 아직 정하지 않은 것

`docs/architecture.md` §11에 남은 것 중 **코루틴 사용 범위**와 **모듈 의존성 검증 태스크**는
여전히 미정이다. 도메인 분할만 이 문서로 확정됐다.
