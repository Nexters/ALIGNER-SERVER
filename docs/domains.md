# 도메인 분할 — 확정본

Aligner 서버의 도메인 경계 확정본. 2026-07-27 결정, 2026-08-03 개정.

개정 근거는 Figma 와이어프레임(`web_프렙_디자인 작업방` › 와이어프레임 `154:1303`)과
콘텐츠 정본(핀포즈 9개·루틴 구성)이다. 바뀐 것은 셋이다 — `screening`이 문항이 아니라
**자세 선택**을 받는다(§4-2), **`PoseCheckpoint`를 만들지 않는다**(§4-3), `catalog`가
**근육**을 갖는다(§4-3).

`docs/architecture.md`가 **도메인 하나가 어떤 모양인지**를 정의하고, 이 문서는 **그 골격 위에
어떤 도메인을 몇 개 놓을지**를 정의한다. 계층 규칙·Bean 조립·`api`/`implementation` 판단이
필요하면 항상 `docs/architecture.md`를 읽는다. 여기서는 그 값을 복제하지 않는다.

## 1. 확정된 결정

| 항목 | 결정 |
| --- | --- |
| 도메인 수 | **5개** — `member` `screening` `catalog` `course` `training` |
| PostgreSQL schema | 도메인명과 동일 (`member` `screening` `catalog` `course` `training`) |
| 도메인 간 의존 | **단방향** — `catalog` ← `course` ← `training`, `screening` ← `course` |
| 마스터 데이터 소유 | 운동·자세·근육은 `catalog`, 자세 체감 → 원인 분기 규칙은 `screening`, 처방 규칙은 `course` |
| 도장(`Stamp`) | `course` 소유 (달성 판단은 `course`, 수행 기록은 `training`). **판정 기준 미정** (§7-8) |
| 자세 포인트(`PoseCheckpoint`) | **만들지 않는다.** 완료 판정은 "운동 수행 + 시간 종료"다 (§4-3) |
| 총 모듈 수 | **44개** — 도메인 40 + 루트 4 |
| 외부 시스템 | **YMove** — 영상·음성 대본. `catalog`만 접근한다 (§4-3-1) |

### 도메인 지도

| 도메인 | 소유하는 것 | MVP(`AGENTS.md` §3) |
| --- | --- | --- |
| `member` | 회원, 카카오 식별자, 프로필, 신체 정보 | 1, 8 |
| `screening` | 부위, 자세 체감 → 원인 분기 규칙, 회원 응답과 판별된 원인 | 2, 3(앞) |
| `catalog` | 보강 운동·목표 자세·근육, YMove 연동(영상·음성 대본) | 5, 7(정의) |
| `course` | 원인별 코스 템플릿, 회원별 처방 코스·스텝·진행 상태, 도장·해금 | 3(뒤), 4, 7 |
| `training` | 세션 시작·완료·수행 기록 | 6 |

### 핵심 도메인 루프(`AGENTS.md` §2)와의 대응

```text
① 부위 선택                     screening
② 자세 체감 선택                screening
③ 응답 분기 → ④ 원인 판별       screening
⑤ 원인별 코스 처방              course  (원인은 screening:contract로 읽음)
⑥ 세션 수행 → 완료 기록         training
⑦ 세션 완료                     training 이 기록 → course 로 push
   └ 완수 판정 → 도장·해금       course   ※ 판정 기준 미정 (§7-8)
```

**`AGENTS.md` §2와 어긋난다.** 그 문서의 루프는 `PoseCheckpoint 확인 → Stamp/다음 코스 보강`
이고 용어집에도 `PoseCheckpoint` 행이 있다. 자세 포인트를 만들지 않기로 하면서 ⑦이 바뀌었다.
`AGENTS.md`는 루트 판단 문서라 별도 승인 후 갱신한다 (§7-11).

---

## 2. 왜 이렇게 나눴는가

### `catalog`를 따로 두는 이유

운동·자세·근육은 **요가 지도자 감수 전 마스터 데이터**이고 쓰기가 없다
(`docs/architecture.md` §6). `course`와 `training` 두 도메인이 모두 읽는다.
YMove 연동이 붙은 뒤에도 이 성질은 유지된다 — `catalog`는 YMove를 **읽기만** 한다(§4-3-1).
덕분에 외부 시스템을 아는 도메인이 하나로 묶인다는 이점이 하나 더 생겼다.
한 도메인에 얹으면 그 도메인의 `contract`가 "처방 계약 + 운동 카탈로그 조회"로 넓어지는데,
§7은 `contract`를 좁게 만들라고 한다. Command가 없어 모듈 수 대비 실제 코드량은 작다.

### `course`가 `training`을 의존하지 않는 이유 — 기록은 training, 판단은 course

⑦의 완수 판정을 `course`가 `training`을 조회하는 pull로 짜면 `course ↔ training` 양방향이 된다.

그래서 **세션 완료를 `training`이 `course`로 push**한다. `training`은 "무슨 일이 있었나"만
기록하고, 도장·해금 **판단은 전부 `course`**가 한다. 판단 타이밍도 "다음날 조회 시"보다
"세션 완료 직후"가 자연스럽다.

자세 포인트를 만들지 않기로 하면서 push하는 내용이 "체크 결과"에서 "세션 완료"로 줄었다.
분리 자체는 유지한다 — 판단이 `course`에 있다는 성질이 그대로이고, `training`에 완수 판정이
생기면 잘못 나눈 것이라는 기준도 그대로다.

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
training ──→ course:contract              스텝 구성 조회 / 세션 완료 push
training ──→ catalog:contract             세션 중 운동 상세 조회
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
    fun findLatestCauses(memberId: Long): List<LatestCauseResponse>   // rank 순
}

// catalog/contract
interface ExerciseContract {
    fun findAllByIds(exerciseIds: List<Long>): List<ExerciseResponse>
}
interface TargetPoseContract {
    fun findById(targetPoseId: Long): TargetPoseResponse?
}

// course/contract
interface CourseStepContract {
    fun findStep(courseId: Long, stepOrder: Int): CourseStepResponse?
}
interface CourseProgressContract {
    fun completeStep(command: CompleteStepCommand)
    fun completeSession(command: CompleteSessionCommand)
}
```

`ScreeningResultContract`가 **복수를 돌려준다.** 진단 결과 화면이 원인 부위를 순위로
나열하므로 단수로는 만들 수 없다. `bodyPartCode` 파라미터도 없다 — 진단이 부위를 결정하는
쪽이라 호출부가 미리 알고 들어오지 않는다.

### port ↔ adapter 대응

| 요청 도메인 port (`infrastructure`) | adapter 모듈 | 대상 contract |
| --- | --- | --- |
| `course` — `CauseLookupPort` | `course/adapter-screening` | `screening:contract` |
| `course` — `ExerciseCatalogPort` `TargetPoseCatalogPort` | `course/adapter-catalog` | `catalog:contract` |
| `training` — `CourseStepPort` `CourseProgressPort` | `training/adapter-course` | `course:contract` |
| `training` — `ExerciseDetailPort` | `training/adapter-catalog` | `catalog:contract` |
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
                 height_cm(null), weight_kg(null), experience_level(null),
                 created_at, updated_at
```

카카오 로그인의 웹 계층은 `support-web`, 회원 조회·가입은 `member`가 갖는다
(`docs/architecture.md` §9). `adapter-auth`가 빠지면 기동이 실패해야 정상이다.

**신체 정보를 `member`가 갖는 이유.** 키·몸무게·운동 경력은 온보딩의 진단 흐름 안에서
입력받지만 `screening`이 아니라 `member`에 둔다. 프로필 편집에서 진단과 무관하게 바뀌고,
무엇보다 **몸무게가 칼로리 계산의 입력**이라 코스·세션을 조회할 때마다 필요하다(§4-3).
가입 직후에는 없으므로 전부 nullable이다.

진단 시점의 몸을 보존할 필요가 있으면 `screening_result`에 스냅샷으로 복사한다. `member`의
현재 값을 거슬러 올라가 읽지 않는다.

### 4-2. `screening`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `ScreeningResult` (루트) + `ScreeningAnswer` + `ScreeningCause` (자식) |
| 마스터 | `BodyPart` `CauseRule` `Cause` — 전부 seed. **문항·선택지는 없다** |
| Command | 자세 체감 선택 제출 → 원인 판별 → 결과 저장 |
| Query | 부위 목록, 회원의 최신 결과(복수 원인) |
| 모듈 | 기본 6 + `contract` = **7** |

```text
screening.body_part          body_part_code(pk), name, display_order              [seed]
screening.cause              cause_code(pk), name, body_part_code, description    [seed]
screening.cause_rule         rule_id(pk), target_pose_id, perceived_difficulty,
                             cause_code, weight                                   [seed]

screening.screening_result   result_id(pk), member_id,
                             perceived_body_part_code,       -- 회원이 고른 "느끼는 부위"
                             created_at
screening.screening_answer   answer_id(pk), result_id, target_pose_id,
                             perceived_difficulty            -- EASY | HARD
screening.screening_cause    screening_cause_id(pk), result_id, cause_code,
                             rank, score                     -- 판별된 "원인 부위"
```

#### 문항이 아니라 자세 선택을 받는다

온보딩은 부위를 고른 뒤 **핀포즈 그리드에서 쉬웠던 자세와 어려웠던 자세를 각각 최대 4개**
고른다. 설문 문항이 없으므로 `screening_question` `screening_option`을 만들지 않는다.

`cause_rule`이 **(자세, 체감) → 원인** 분기표다. `weight`를 두는 것은 자세를 최대 8개까지
고르기 때문이다. 원인마다 점수가 쌓이고 그 합으로 순위를 매긴다. **집계 방식을 코드가 아니라
seed의 `weight`로 조절**한다 — 감수 결과가 바뀌어도 changeset만 새로 쌓는다.

`target_pose_id`는 `catalog`의 값이지만 **참조하지 않는다.** 자세 그리드는 클라이언트가
`catalog` API로 직접 그리고, `screening`은 식별자만 값으로 받아 저장한다. 덕분에 §1의
단방향 의존이 유지된다 — `screening`은 어떤 도메인도 의존하지 않는다.

#### 느끼는 부위와 원인 부위를 분리한다

회원이 고른 부위와 판별된 원인의 부위는 다르다. `AGENTS.md` §1의 *"느끼는 부위가 아니라
원인 부위를 처방한다"* 가 스키마에 드러난 것이다.

- `screening_result.perceived_body_part_code` — 회원이 고른 것. 진단의 입력이자 맥락이다
- `screening_cause` — 판별된 것. `course`가 처방에 쓰는 값이다

**원인이 복수다.** 진단 결과 화면이 원인 부위를 순위로 나열하므로 `screening_result` 하나에
`screening_cause`가 여러 개 달린다. `rank`가 표시 순서이고 `score`가 `weight` 합계다.

`member_id` `target_pose_id`는 값 컬럼이며 FK를 걸지 않는다(§6).

### 4-3. `catalog`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Exercise` `TargetPose` `Muscle` |
| Command | **없음** — `CommandService`·쓰기 port를 만들지 않는다(§4) |
| Query | 운동 상세, 자세 상세, 자세·운동별 근육 |
| 외부 의존 | **YMove** — 영상·음성 대본의 정본 (§4-3-1) |
| 모듈 | 기본 6 + `contract` + `adapter-ymove` = **8** |

```text
catalog.exercise         exercise_id(pk), ymove_slug(uk), name,
                         default_set_count, default_rep_count,
                         default_duration_seconds, met_value, difficulty,
                         contraindications                                       [seed]
catalog.target_pose      target_pose_id(pk), ymove_slug(uk), name, image_url,
                         body_part_code, level                                   [seed]
catalog.muscle           muscle_code(pk), name, body_part_code,
                         highlight_asset_key                                     [seed]
catalog.pose_muscle      target_pose_id, muscle_code, role, display_order        [seed]
catalog.exercise_muscle  exercise_id, muscle_code, role, display_order           [seed]
```

`catalog`는 순수 카탈로그다. "어떤 원인에 어떤 운동을 쓰는가"는 처방 규칙이므로 `course`가 갖는다.
여기에 `cause_code`를 두지 않는다.

`infrastructure`에는 `QueryRepository`와 **`PoseVideoPort`**(아래)만 둔다. 쓰기 port를 세트로
찍지 않는다.

#### `PoseCheckpoint`를 만들지 않는다

자세 포인트 체크를 하지 않기로 했다. **완료 판정은 "운동을 수행했고 시간이 끝나면 완료"** 다.

따라서 `catalog.pose_checkpoint`, `course.reinforcement_rule`, `training.checkpoint_result`,
`TargetPoseContract.findCheckpoints`, `PoseCheckpointPort`가 전부 없다. 미달 포인트를 다음
코스에 보강 편성하는 흐름도 없다.

도장·해금·진행도의 판정 기준이 이 결정으로 비었다. §7-8에 열린 질문으로 남긴다.

#### 근육 — `catalog`가 갖는다

근육은 운동 가이드의 부위 탭과 근육맵에 쓰인다. "어떤 운동이 어느 근육을 쓰는가"는 카탈로그
성격이므로 `catalog`가 소유한다.

`role`은 **`STRETCH`(신장) | `STRENGTHEN`(강화)** 다. 같은 자세가 어떤 근육은 늘리고 어떤
근육은 쓰므로 구분이 필요하다. `highlight_asset_key`는 근육맵 이미지 식별자이고 실제 파일은
정적 asset이다. DB에는 키만 둔다.

**`screening`은 근육을 참조하지 않는다.** 진단 결과의 "어떤 근육이 부족하다"는 문구는
`screening.cause.description`에 감수 문구로 넣는다. 원인 자체가 "어떤 근육이 약한가"라
같은 것을 두 번 모델링할 필요가 없다. 덕분에 `screening → catalog` 의존이 생기지 않는다.
근육 이름이 `catalog.muscle.name`과 `screening.cause.description` 두 곳에 있게 되지만,
둘 다 seed라 감수 결과가 바뀌면 changeset을 같이 쌓으면 되고 코드는 손대지 않는다.

#### 칼로리는 저장하지 않는다

`kcal = MET × 3.5 × 체중(kg) ÷ 200 × 분` 이라 **회원 몸무게의 함수**다. 저장값이 아니라
조회 시 계산값이므로 seed에는 `met_value`만 둔다. 몸무게는 `member`가 갖는다(§4-1).

MET을 레벨이 아니라 **운동 단위로 두는 이유**는 코스 구성이 회원마다 달라질 수 있어서다.
레벨별 고정값을 쓰면 구성이 바뀐 코스의 칼로리가 실제와 어긋난다. 코스 칼로리는 스텝 합으로
계산한다.

MET 값의 출처와 보간 근거는 감수 대상이다.

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
| `title` `description` `videoDurationSecs` | YMove | 중복 저장하지 않는다 |
| `name` | `catalog` seed | YMove `title`을 우리 표현으로 덮어야 할 때만 쓰는 override |
| `difficulty` | `catalog` seed | **YMove 값을 쓰지 않는다.** 아래 참고 |
| `muscleGroup` | `catalog` seed | 근육맵·부위 탭이 필요해 우리 마스터로 갖는다(§4-3) |
| `default_set_count` `default_rep_count` `default_duration_seconds` | `catalog` seed | YMove에 없다. 감수 대상이다 |
| `met_value` | `catalog` seed | YMove에 없다. 칼로리 계산 입력이다 |
| `contraindications` | `catalog` seed | 금기 사항. 운동 가이드의 주의사항 탭에 쓴다 |

**`difficulty`를 YMove에서 받지 않는다.** YMove는 요가 콘텐츠 전량을 `beginner`로 태깅하고
있어 값이 변별력이 없다. 레벨은 우리가 감수로 부여한다. `target_pose.level`도 같은 이유다.

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
- 홈·코스 목록이 스텝마다 썸네일과 시간을 그린다. **목록 조회에서 YMove를 스텝 수만큼 치면
  안 된다.** `difficulty` `default_duration_seconds`를 seed로 내린 이유이기도 하다.

### 4-4. `course`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Course`(루트) + `CourseStep` + `CourseStepExercise`, `Stamp`(루트) |
| 마스터 | `CourseTemplate` + `TemplateStep`, 자세 사다리 — seed |
| Command | 코스 처방, 스텝 완료, 세션 완료 반영(도장·해금) |
| Query | 코스 상세(스텝+운동), 진행도, 획득한 도장 |
| 모듈 | 기본 6 + `contract` + `adapter-screening` + `adapter-catalog` = **9** |

```text
course.course_template          template_id(pk), cause_code, target_pose_id, name,
                                unlock_required_target_pose_id(null)              [seed]
course.template_step            template_step_id(pk), template_id, step_order     [seed]
course.template_step_exercise   template_step_id, exercise_id, display_order      [seed]

course.course                   course_id(pk), member_id, template_id, cause_code,
                                target_pose_id, scheduled_on,
                                current_step_order, status, created_at
course.course_step              course_step_id(pk), course_id, step_order, status
course.course_step_exercise     course_step_exercise_id(pk), course_step_id, exercise_id,
                                display_order, duration_seconds, set_count
course.stamp                    stamp_id(pk), member_id, target_pose_id, acquired_at
```

- `scheduled_on`이 **며칠 코스인가**다. 홈이 "오늘의 코스 / 내일의 코스"를 보여주므로 코스가
  일자 단위로 존재한다. 며칠치를 미리 만드는지와 `(member_id, scheduled_on)` 유니크 여부는
  미정이다(§7-9).
- `duration_seconds` `set_count`는 nullable이며 비어 있으면 `catalog.exercise`의 기본값을 쓴다.
- **자세 사다리는 분기하지 않는다.** 부위별로 레벨 1→2→3 선형이므로
  `unlock_required_target_pose_id` 한 컬럼으로 충분하다.
- `exercise_id` `target_pose_id` `member_id`는 전부 값 컬럼이고 FK가 없다.
- `reinforcement_rule`과 `course_step_exercise.source`는 **없다.** 보강 편성이 사라졌다(§4-3).

### 4-5. `training`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Session`(루트) + `SessionExerciseRecord` |
| Command | 세션 시작, 세션 완료 |
| Query | 세션 기록 조회 |
| 모듈 | 기본 6 + `adapter-course` + `adapter-catalog` = **8** |

```text
training.session                 session_id(pk), member_id, course_id, step_order,
                                 started_at, completed_at, status
training.session_exercise_record  record_id(pk), session_id, exercise_id,
                                 completed, performed_duration_seconds
```

세션 완료 이후 `CourseProgressPort`로 `course`에 밀어넣는다.
**`training`에는 도장·해금 판단이 없다.** 그 로직이 여기 생기면 잘못 나눈 것이다.

`checkpoint_result`와 `checkpoint_result_item`은 **없다.** 자세 포인트 체크를 하지
않는다(§4-3).

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
1. build-logic + support-core + support-web + application-api  (골격)   ✅ #3
2. member  (+ contract, adapter-auth)   — 인증이 없으면 나머지 API를 붙일 수 없다  ✅ #5
3. catalog                              — 의존이 없고 seed만 있어 가장 가볍다
4. screening                            — catalog 의 target_pose_id 를 값으로 쓴다
5. course    (adapter-screening, adapter-catalog)
6. training  (adapter-course, adapter-catalog)
```

`screening`은 `catalog`를 **의존하지 않지만**(§4-2) `target_pose_id`를 값으로 참조하므로
자세 seed가 먼저 있어야 분기 규칙 seed를 쓸 수 있다. 순서는 그대로다.

`course`는 §7-8·§7-9가 정해지기 전에는 착수할 수 없다. 도장·해금·진행도와 코스 스케줄이
비어 있기 때문이다. `catalog`와 `screening`은 그 결정과 무관하게 진행할 수 있다.

---

## 6. 스키마 계획

`docs/architecture.md` §6 적용.

- schema 5개. 각 도메인 changelog 첫 changeset에 `CREATE SCHEMA IF NOT EXISTS {domain}`.
- 모든 DDL은 schema-qualified. 엔티티에 `@Table(schema = "{domain}", ...)`, `JdbcClient` SQL도 동일.
- 루트 `changelog-master.yaml`에 5개 include를 추가한다.
- **도메인 간 FK 없음.** `member_id` `exercise_id` `target_pose_id` `muscle_code` `cause_code`는
  전부 값 컬럼이다. 존재 검증이 필요하면 port로 한다.
- seed는 `schema/seed/`의 changeset으로 넣는다. 감수 전 데이터가 들어가는 곳은 다섯이다.

| seed | 위치 | 감수 대상 |
| --- | --- | --- |
| 부위·원인·자세 체감 분기 규칙(`weight` 포함) | `screening/schema/seed/` | ✅ |
| 운동·목표 자세·레벨·난이도·MET·금기 | `catalog/schema/seed/` | ✅ |
| 근육 마스터·자세↔근육·운동↔근육 | `catalog/schema/seed/` | ✅ |
| 원인별 코스 템플릿·스텝 구성 | `course/schema/seed/` | ✅ |
| 자세 사다리(선행 자세) | `course/schema/seed/` | ✅ |

이 넷 중 어느 것도 코드에 하드코딩하지 않는다. 하드코딩은 그 자체로 `[필수]` 지적이다.

---

## 7. 열린 질문 — 구현 전에 답해야 한다

1. **운동의 수행 시간·세트를 누가 정하는가.** 위 설계는 `catalog.exercise`에 기본값을 두고
   `course.course_step_exercise`가 필요할 때만 덮어쓰는 방식이다. 같은 운동이 코스마다 다른
   시간을 갖는 사례가 콘텐츠 정본에 있으므로(같은 준비 동작이 레벨별로 1분 30초·2분·2분 30초)
   override 컬럼은 유지한다. 세트 override까지 필요한지는 확인이 남았다.
2. ~~**자세 해금 사다리의 표현.**~~ **해소.** 부위별 레벨 1→2→3 선형이고 분기가 없다.
   `unlock_required_target_pose_id` 한 컬럼으로 충분하다.
3. ~~**`Course` 용어.**~~ **해소.** 마스터(`CourseTemplate`)와 회원 인스턴스(`Course`) 구분이
   화면과 일치한다. `AGENTS.md` 용어집에 `CourseTemplate` 행을 추가하는 것은 §7-11에 함께 둔다.
4. **YMove 장애 시 무엇을 보여주는가.** 영상 없이는 세션을 진행할 수 없어 fallback이 없다.
   타임아웃 값, 재시도 여부, 사용자에게 보일 메시지를 `catalog` 착수 전에 정해야 한다.
5. **재생 URL 캐시 위치와 TTL.** 48시간 만료이므로 그보다 짧아야 한다. 코스 상세 조회처럼
   여러 자세를 한 번에 그리는 화면에서 매번 YMove를 치지 않도록 `PoseVideoPort` 구현체 안에
   둘지, 그 위에 둘지 정해야 한다. `thumbnailUrl`도 같이 만료되는지 YMove 확인이 필요하다.
6. **YMove의 `slug`가 안정적인가.** 우리 seed가 `ymove_slug`를 유일한 연결 고리로 쓰므로,
   YMove 쪽에서 slug가 바뀌면 해당 운동·자세가 통째로 깨진다. 불안정하면 내부 id를 함께
   저장해야 한다. 좌우가 나뉘는 자세(`*-left` `*-right`)를 `exercise` 한 행으로 볼지 두 행으로
   볼지도 여기서 같이 정한다.
7. **고민 유형(`Concern`)은 P1**이다(`AGENTS.md` §3). `screening.body_part` 아래 자리만 비워두고
   지금 만들지 않는다. 추가될 때 `screening.concern` + `cause_rule`에 `concern_code`가 붙는다.
8. **완수·도장·해금의 판정 기준.** 자세 포인트를 만들지 않기로 하면서(§4-3) 판정 근거가 비었다.
   "운동 수행 + 시간 종료"는 **세션** 완료의 정의이고, **자세** 완수는 별개다. 도전 현황 화면이
   `3 / 4` 형태의 진행도와 `도전 중 / 완성` 상태를 보여주므로 세션 1회로는 부족하다.
   누적 횟수(`target_pose`에 필요 횟수 seed) 방식이 유력하나 **기획 확정 대기 중**이다.
   이것이 정해지기 전에는 `course`의 도장·해금과 진행도 Query를 확정할 수 없다.
9. **오늘 코스와 내일 코스의 차이.** 보강 편성이 사라지면서 두 코스가 달라질 근거가 없어졌다.
   같은 코스의 반복인지, 레벨 사다리를 따라 진행하는지, 며칠치를 미리 만드는지가 미정이다.
   `course.scheduled_on`의 유니크 제약도 여기 달렸다. **기획 확정 대기 중.**
10. **MET 값의 출처와 보간.** 레벨별 값이 표준 참조의 보간이라 감수가 필요하다. 운동 단위로
    값을 부여하는 이상 준비 동작과 핀포즈에 각각 무엇을 줄지도 감수 대상이다.
11. **`AGENTS.md` 갱신.** §1 핵심 루프의 `PoseCheckpoint 확인 → Stamp/다음 코스 보강`,
    §2 용어집의 `PoseCheckpoint` 행이 이 문서와 어긋난다. §2에 `CourseTemplate` 행을 추가하는
    것도 남아 있다. `AGENTS.md`는 루트 판단 문서라 **별도 승인 후** 갱신한다.

## 8. 이 분할에서 아직 정하지 않은 것

`docs/architecture.md` §11에 남은 것 중 **코루틴 사용 범위**와 **모듈 의존성 검증 태스크**는
여전히 미정이다. 도메인 분할만 이 문서로 확정됐다.
