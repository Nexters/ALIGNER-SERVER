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
| 도메인 간 의존 | **단방향** — `catalog` ← `course` ← `training`, `screening` ← `course`, `member` ← `course` |
| 마스터 데이터 소유 | 운동·자세·근육은 `catalog`, 자세 체감 → 원인 분기 규칙은 `screening`, 처방 규칙은 `course` |
| 도장(`Stamp`) | `course` 소유 (달성 판단은 `course`, 수행 기록은 `training`). **판정 = 코스의 전체 스텝 완료** (§7-8) |
| 자세 포인트(`PoseCheckpoint`) | **만들지 않는다.** 완료 판정은 "운동 수행 + 시간 종료"다 (§4-3) |
| 총 모듈 수 | **45개** — 도메인 41 + 루트 4. `course`에 `adapter-member`가 늘었다(§3) |
| 외부 시스템 | **YMove** — 영상·썸네일. `catalog`만 접근한다 (§4-3-1). **음성 큐잉 대본은 `catalog`가 소유한다** |

### 도메인 지도

| 도메인 | 소유하는 것 | MVP(`AGENTS.md` §3) |
| --- | --- | --- |
| `member` | 회원, 카카오 식별자, 프로필, 신체 정보 | 1, 8 |
| `screening` | 부위, 자세 체감 → 원인 분기 규칙, 회원 응답과 판별된 원인 | 2, 3(앞) |
| `catalog` | 보강 운동·목표 자세·근육, 음성 큐잉 대본(번역본), YMove 연동(영상) | 5, 7(정의) |
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
   └ 완수 판정 → 도장            course   (코스의 전체 스텝 완료, §7-8)
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
course   ──→ member:contract              몸무게 조회 (칼로리 계산)
catalog  ──→ YMove (외부 HTTP)            videoUrl·thumbnailUrl, 48시간 만료 (§4-3-1)
training ──→ course:contract              스텝 구성 조회 / 세션 완료 push
training ──→ catalog:contract             세션 중 운동 상세 조회
```

순환이 없다. `training` → `course` → `screening`·`catalog`·`member` 한 방향으로만 흐른다.

**`course → member` 를 뒤늦게 허용했다.** 초판 지도에는 없었는데, 홈 카드가 코스 칼로리를
보여주고 `kcal = MET × 3.5 × 체중 ÷ 200 × 분` 이라 **몸무게 없이는 계산이 성립하지 않는다**(§4-3).
`member` 는 아무 도메인도 의존하지 않으므로 순환은 생기지 않는다. 계약은 몸무게 하나로 좁게 둔다.

### 계약 시그니처 초안

`contract`는 통합 전용으로 좁게 만든다. 식별자는 원시 타입으로 받고 발행 DTO로 반환한다.
구현체는 `internal`로 대상 도메인 `service`에 두고 Bean도 거기서 등록한다(§7).

```kotlin
// member/contract
interface MemberAuthContract {
    fun findOrRegisterByKakao(command: KakaoMemberCommand): AuthenticatedMemberResponse
}
interface MemberBodyContract {
    fun findBody(memberId: Long): MemberBodyResponse?   // 몸무게만. 칼로리 계산용
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
    fun completeSession(command: CompleteSessionCommand): CourseProgressResponse
}
```

`ScreeningResultContract`가 **복수를 돌려준다.** 진단 결과 화면이 원인 부위를 순위로
나열하므로 단수로는 만들 수 없다. `bodyPartCode` 파라미터도 없다 — 진단이 부위를 결정하는
쪽이라 호출부가 미리 알고 들어오지 않는다.

`CourseProgressContract`에 **`completeSession` 하나만 둔다.** 초안에 있던 `completeStep`을
만들지 않는 이유는 스텝 완료를 세션 완료와 따로 부를 주체가 이 설계에 없기 때문이다 — 두
진입점이 열려 있으면 세션 없이 스텝만 완료하는 경로가 생긴다.

**재시도는 애그리거트가 흡수한다.** 이미 완료된 스텝을 다시 완료해도 진행도가 오르지 않고
도장도 한 번만 붙는다(§7-8). `training`이 세션을 저장한 뒤 push 에 실패해 재시도해도 안전하다.

### port ↔ adapter 대응

| 요청 도메인 port (`infrastructure`) | adapter 모듈 | 대상 contract |
| --- | --- | --- |
| `course` — `CauseLookupPort` | `course/adapter-screening` | `screening:contract` |
| `course` — `ExerciseCatalogPort` `TargetPoseCatalogPort` | `course/adapter-catalog` | `catalog:contract` |
| `training` — `CourseStepPort` `CourseProgressPort` | `training/adapter-course` | `course:contract` |
| `training` — `ExerciseDetailPort` | `training/adapter-catalog` | `catalog:contract` |
| `course` — `MemberBodyPort` | `course/adapter-member` | `member:contract` |
| `support-web` — `AuthMemberPort` | `member/adapter-auth` | `member:contract` |

`training`은 아무도 읽지 않으므로 **`contract`를 만들지 않는다.** 미리 만들지 않는 원칙(§3)이다.

---

## 4. 도메인별 상세

### 4-1. `member`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Member` (루트) |
| Command | 카카오 가입, 프로필 수정, 탈퇴 |
| Query | 프로필 조회 |
| 모듈 | 기본 6 + `contract` + `adapter-auth` = **8** |

```text
member.member    member_id, kakao_id(uk, null), nickname, profile_image_url,
                 height_cm(null), weight_kg(null), experience_level(null),
                 reinforcement_body_part_code(null), reinforcement_level(null),
                 withdrawn_at(null), created_at, updated_at
```

카카오 로그인의 웹 계층은 `support-web`, 회원 조회·가입은 `member`가 갖는다
(`docs/architecture.md` §9). `adapter-auth`가 빠지면 기동이 실패해야 정상이다.

**신체 정보를 `member`가 갖는 이유.** 키·몸무게·운동 경력은 온보딩의 진단 흐름 안에서
입력받지만 `screening`이 아니라 `member`에 둔다. 프로필 편집에서 진단과 무관하게 바뀌고,
무엇보다 **몸무게가 칼로리 계산의 입력**이라 코스·세션을 조회할 때마다 필요하다(§4-3).
가입 직후에는 없으므로 전부 nullable이다.

진단 시점의 몸을 보존할 필요가 있으면 `screening_result`에 스냅샷으로 복사한다. `member`의
현재 값을 거슬러 올라가 읽지 않는다.

**강화 부위와 난이도도 `member`가 갖는다.** 회원은 진단 결과를 본 뒤 강화할 부위와 난이도를
고르는데(§4-2), 이것이 코스 처방 시점의 일회성 입력이 아니라 **지속되는 설정**이다 —
마이페이지가 "등근육을 난이도 하로 강화하고 있어요"를 보여주고 "난이도 조정하기"로 언제든
바꾼다. 신체 정보를 `member`에 둔 것과 같은 이유다.

`reinforcement_body_part_code`는 `screening` 소유 어휘를 값으로 받고 FK를 걸지 않는다(§6).
값 집합을 검증하려면 `member → screening` 의존이 생기는데 그 방향은 §3에 없다.
`reinforcement_level`(1·2·3)이 `catalog.target_pose.level`과 같은 축인지는 **미확정**이다.

**탈퇴는 행을 지우지 않는다.** 운동 기록을 보존하기로 했고 그 기록이 `member_id`로 붙어 있다.
남는 개인정보가 카카오 식별자뿐이라 `kakao_id`만 `NULL`로 만들고 `withdrawn_at`을 남긴다.
그래서 `kakao_id`가 nullable이다. UNIQUE는 유지된다 — PostgreSQL은 `NULL`을 서로 다른 값으로
보므로 탈퇴 회원이 여럿이어도 충돌하지 않는다.

탈퇴 회원은 모든 조회에서 걸러진다. 리프레시 토큰이 없어 발급된 JWT를 회수할 수단이 없으므로,
아직 만료되지 않은 토큰으로 들어와도 조회 단계에서 없는 것으로 취급돼야 한다. 같은 카카오
계정이 다시 가입하면 **새 `member_id`**를 받고 이전 기록은 이어지지 않는다.

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
                             perceived_body_part_code(null),  -- 쓰지 않는다. 아래 참고
                             created_at
screening.screening_answer   answer_id(pk), result_id, target_pose_id,
                             perceived_difficulty            -- EASY | HARD
screening.screening_cause    screening_cause_id(pk), result_id, cause_code,
                             rank, score                     -- 판별된 "원인 부위"
```

#### 온보딩 순서 — 부위는 진단의 입력이 아니라 결과 이후의 선택이다

와이어프레임(`UT용 UI`, `node-id=616-5079`)의 온보딩 순서는 이렇다.

```text
소셜로그인 → 운동 경력 → 키·몸무게        member
  → 핀포즈 소개 → 쉬웠던 자세(최대 4)
  → 어려웠던 자세(최대 4)                 screening  ← 부위를 묻지 않는다
  → "근육 상태 분석중" → 원인 순위 표시    screening
  → 강화할 부위 선택 → 난이도 선택         member
```

**부위를 먼저 묻지 않는다.** 초판 설계는 "부위 선택 → 그 부위의 자세 그리드" 였고 그래서
`perceived_body_part_code`를 진단의 입력으로 뒀는데, 실제 화면은 자세를 먼저 받아 원인을
판별하고 **그 결과를 본 뒤에** 강화할 부위를 고른다. 뒤쪽 선택은 마이페이지에서 "난이도
조정하기"로 계속 바뀌는 회원의 지속 설정이므로 `member`가 갖는다(§4-1).

따라서 `POST /screening/results`는 부위를 받지 않고, `perceived_body_part_code`는 NULL 허용
컬럼으로 남아 있되 저장 경로가 채우지 않는다. 컬럼을 지우지 않은 것은 "느끼는 부위를 영영
받지 않는다"가 확정되지 않아서다 — `DROP COLUMN`은 되돌려도 데이터가 돌아오지 않는다.

`GET /screening/body-parts`는 없어지지 않는다. **강화 부위 선택 화면의 선택지**로 쓰인다.
판별된 원인의 부위만 내리지 않고 전체를 내린다 — 화면이 분석 결과에 없는 부위도 고를 수 있게
그리기 때문이다.

`GET /catalog/target-poses`의 `bodyPartCode`도 **선택 파라미터**가 됐다. 온보딩 그리드가
부위로 걸러지지 않고 핀포즈 전체를 펼치므로 그쪽이 기본이다.

#### 문항이 아니라 자세 선택을 받는다

온보딩은 **핀포즈 그리드에서 쉬웠던 자세와 어려웠던 자세를 각각 최대 4개** 고른다.
설문 문항이 없으므로 `screening_question` `screening_option`을 만들지 않는다.

`cause_rule`이 **(자세, 체감) → 원인** 분기표다. `weight`를 두는 것은 자세를 최대 8개까지
고르기 때문이다. 원인마다 점수가 쌓이고 그 합으로 순위를 매긴다. **집계 방식을 코드가 아니라
seed의 `weight`로 조절**한다 — 감수 결과가 바뀌어도 changeset만 새로 쌓는다.

#### 같은 자세를 두 번 고를 수 없다 — 제약을 어디에 두는가

점수를 합산해 순위를 매기므로 응답이 중복되면 그대로 순위가 뒤틀린다. 같은 자세가 두 번
들어오면 원인 점수가 부풀고, 같은 자세를 `EASY`와 `HARD`로 같이 제출하면 모순된 응답이
저장된다. 둘 다 집계 **전에** 막는다.

- `screening_answer`에 `UNIQUE (result_id, target_pose_id)`. 중복 제출과 난이도 양쪽 제출을
  한 제약으로 같이 막으려고 `perceived_difficulty`를 키에서 뺐다
- `perceived_difficulty`는 `CHECK (perceived_difficulty IN ('EASY', 'HARD'))`.
  `cause_rule`의 같은 이름 컬럼에도 동일하게 건다 — 분기표 좌변과 응답이 같은 값 집합이어야
  조인이 성립한다
- **난이도별 최대 4개는 DB 제약으로 만들지 않는다.** 행 개수를 세는 조건이라 `CHECK`으로
  쓸 수 없고 트리거는 과하다. `ScreeningResult` 애그리거트가 저장 전에 검증한다. 개수 상한은
  감수 데이터가 아니라 온보딩 화면 규칙이라 바뀔 때 changeset이 아니라 코드만 고치는 편이 맞다

`target_pose_id`는 `catalog`의 값이지만 **참조하지 않는다.** 자세 그리드는 클라이언트가
`catalog` API로 직접 그리고, `screening`은 식별자만 값으로 받아 저장한다. 덕분에 §1의
단방향 의존이 유지된다 — `screening`은 어떤 도메인도 의존하지 않는다.

#### 판별된 원인이 곧 부위를 정한다

`AGENTS.md` §1의 *"느끼는 부위가 아니라 원인 부위를 처방한다"* 는 그대로다. 다만 이제
**회원이 고른 부위와의 대비가 아니라 원인 판별 자체로** 성립한다 — 회원은 자세만 고르고,
부위는 서버가 판별한 원인이 결정한다.

- `screening_cause` — 판별된 원인. `cause.body_part_code`가 곧 원인 부위이고, `course`가
  처방에 쓰는 값이다
- `screening_result.perceived_body_part_code` — 옛 온보딩의 흔적. 항상 NULL이다

**원인이 복수다.** 진단 결과 화면이 원인 부위를 순위로 나열하므로 `screening_result` 하나에
`screening_cause`가 여러 개 달린다. `rank`가 표시 순서이고 `score`가 `weight` 합계다.

`member_id` `target_pose_id`는 값 컬럼이며 FK를 걸지 않는다(§6).

### 4-3. `catalog`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Exercise` `TargetPose` `Muscle` |
| Command | **없음** — `CommandService`·쓰기 port를 만들지 않는다(§4) |
| Query | 운동 상세, 자세 상세, 자세·운동별 근육 |
| 외부 의존 | **YMove** — 영상·썸네일의 정본. 음성 큐잉 대본은 `catalog` seed다 (§4-3-1) |
| 모듈 | 기본 6 + `contract` + `adapter-ymove` = **8** |

```text
catalog.exercise            exercise_id(pk), ymove_slug(uk), name,
                            default_set_count, default_rep_count,
                            default_duration_seconds, met_value, difficulty,
                            category, caution_note                               [seed]
catalog.target_pose         target_pose_id(pk), ymove_slug(uk), name,
                            image_asset_key, body_part_code, level               [seed]
catalog.muscle              muscle_code(pk), name, body_part_code,
                            front_highlight_asset_key,
                            back_highlight_asset_key                             [seed]
catalog.pose_muscle         pose_muscle_id(pk), target_pose_id, muscle_code,
                            role, display_order                                  [seed]
catalog.exercise_muscle     exercise_muscle_id(pk), exercise_id, muscle_code,
                            role, display_order                                  [seed]
catalog.exercise_voice_cue  cue_id(pk), exercise_id, display_order,
                            start_offset_seconds(null), end_offset_seconds(null),
                            content                                              [seed]
```

`catalog`는 순수 카탈로그다. "어떤 원인에 어떤 운동을 쓰는가"는 처방 규칙이므로 `course`가 갖는다.
여기에 `cause_code`를 두지 않는다.

#### 핀포즈는 `exercise`와 `target_pose` 양쪽에 행을 갖는다

낙타자세 같은 핀포즈는 두 역할을 겸한다.

- **코스의 마지막 스텝** — 콘텐츠 정본의 루틴 표에 `camel-pose · 2분(40초×3) · 척추기립근,
  대둔근`으로 다른 준비 동작과 같은 형식으로 들어간다. 재생되고, 시간·세트·MET·음성 큐를 갖는다
- **코스의 목표** — 온보딩 자세 그리드에 나오고, 부위와 레벨을 갖고, 진행도·사다리 해금의 단위다

그래서 같은 `ymove_slug`가 `exercise`와 `target_pose` 양쪽에 존재한다. 중복되는 것은 이름과
slug 정도이고 둘 다 seed라 changeset을 같이 쌓으면 된다.

`target_pose`가 `exercise_id`를 참조하게 만들지 않는 이유는 **`target_pose_id`가 세 도메인에
값 컬럼으로 퍼져 있기 때문**이다 — `screening.cause_rule`, `course.course_template`,
`course.stamp`. 운동 카탈로그가 재편될 때 원인 매핑과 도장이 같이 흔들리면 안 된다.

핀포즈에 `exercise` 행이 없으면 음성 큐(`exercise_voice_cue`가 `exercise_id` 참조)·칼로리
(`met_value`)·수행 시간이 전부 비게 된다. §7-10이 MET을 "준비 동작과 핀포즈에 각각" 부여한다고
적은 것도 같은 이야기다.

`infrastructure`에는 `QueryRepository`와 **`PoseVideoPort`**(아래)만 둔다. 쓰기 port를 세트로
찍지 않는다.

#### `PoseCheckpoint`를 만들지 않는다

자세 포인트 체크를 하지 않기로 했다. **완료 판정은 "운동을 수행했고 시간이 끝나면 완료"** 다.

따라서 `catalog.pose_checkpoint`, `course.reinforcement_rule`, `training.checkpoint_result`,
`TargetPoseContract.findCheckpoints`, `PoseCheckpointPort`가 전부 없다. 미달 포인트를 다음
코스에 보강 편성하는 흐름도 없다.

도장·진행도의 판정 기준은 자세 포인트가 아니라 **코스 스텝 완료**로 정해졌다(§7-8).

#### 근육 — `catalog`가 갖는다

근육은 운동 가이드의 부위 탭과 근육맵에 쓰인다. "어떤 운동이 어느 근육을 쓰는가"는 카탈로그
성격이므로 `catalog`가 소유한다.

**근육맵 하이라이트 키가 앞·뒤 두 개다.** 세션 플레이어의 근육맵이 인체 앞면과 뒷면을
토글로 보여주고 각각 근육을 칠하므로, 어느 쪽 그림에 얹을 키인지가 구분돼야 한다. 척추기립근처럼
뒤에만 보이는 근육은 `front_highlight_asset_key`가 `NULL`이고 그 반대도 마찬가지다. 둘 다
비는 상태도 정상이라 "하나는 있어야 한다"는 제약을 걸지 않는다 — 자산 키 자체가 감수 전이다.

`role`은 **`STRETCH`(신장) | `STRENGTHEN`(강화)** 다. 같은 자세가 어떤 근육은 늘리고 어떤
근육은 쓰므로 구분이 필요하다. 콘텐츠 정본이 주동근을 "장요근(신장)"처럼 표기하는 그 구분이다.

`pose_muscle`·`exercise_muscle`은 서로게이트 PK에 `UNIQUE`를 건다 — 각각
`(target_pose_id, muscle_code)`와 `(exercise_id, muscle_code)`다. 현재 정본에는 한 자세에서 같은 근육이 두 역할을 갖는 사례가 없어 `role`을 키에서 뺐지만,
후굴에서 척추기립근이 수축과 신장을 겸하는 식의 감수 결과가 나올 수 있다. 그때 PK를 바꾸는
것보다 UNIQUE 제약만 재정의하는 편이 가볍다.

#### 운동 분류 — `category`

코스 개요가 스텝마다 운동 이름 아래에 `가동성 웜업` · `핀포즈` 같은 분류를 그린다.

**`CHECK`을 걸지 않고 코드에 enum 도 두지 않는다.** 값 집합이 감수 대상이라 확정되지 않았다 —
`difficulty`와 같은 이유다. `MuscleRole`(`STRETCH`·`STRENGTHEN`)은 이 문서가 값 집합을 확정해
enum 이지만 분류는 그렇지 않다. 확정되면 그때 `CHECK`을 새 changeset 으로 얹는다.

#### 주의사항은 문구 한 덩어리다 — `caution_note`

콘텐츠 정본에는 성격이 다른 둘이 섞여 있다.

- **금기 조건** — 낙타자세(고혈압·경추 디스크), 휠(손목·어깨·고혈압·임신), 파이어로그(무릎).
  회원의 신체 조건에 대한 것이고 목록이다
- **주의사항 문구** — "목을 뒤로 완전히 젖히지 마세요. 허리 아래쪽에 날카로운 통증이 오면 즉시
  중단하세요." 수행 중 안내이고 한 문단이다

MVP 화면이 요구하는 것은 **주의사항 탭에 글을 띄우는 것뿐**이다. 금기 조건을 코드화해 회원
프로필과 대조하는 기능은 P0에 없다. 그래서 컬럼 하나(`caution_note TEXT`)로 두고, 금기 조건은
그 문구 안에 문장으로 담는다. 이름을 `contraindications`에서 바꾼 것은 담기는 내용이 금기
목록이 아니라 안내 문구이기 때문이다.

대조 기능이 생기면 `catalog.exercise_contraindication` 테이블을 **추가**한다. 테이블 추가는
`CREATE` 한 번이라 되돌리기 비용이 낮다 — 음성 큐를 처음부터 테이블로 뺀 것과는 상황이 다르다.
그쪽은 `TEXT` → 테이블 전환이라 데이터 변환 changeset이 필요했다.

#### 이미지 파일은 프론트가 갖고 서버는 키만 내린다

MVP는 모바일 웹앱이라 프론트 재배포가 가볍고, 고유 자세가 32개로 고정이라 이미지가 자주
바뀌지 않는다. 이미지 호스팅·CDN을 지금 만들지 않는다.

다만 **파일을 누가 갖느냐와 매핑을 누가 갖느냐는 다르다.** 서버는 `image_asset_key`
`front_highlight_asset_key` `back_highlight_asset_key` 같은 안정된 키를 내리고, 프론트가
그 키로 자기 정적 자산을 찾는다. URL을 내리지 않는다.

컬럼을 아예 없애고 프론트가 `target_pose_id`로 직접 매핑하게 하지 않는 이유는 **id가 안정적이지
않기 때문**이다. `exercise_id` `target_pose_id`는 `GENERATED BY DEFAULT AS IDENTITY`라 seed
changeset을 다시 쌓거나 순서가 바뀌면 값이 달라진다. 그러면 프론트는 에러가 아니라 **엉뚱한
이미지를 그리고**, 발견이 늦다. 어떤 사진을 쓸지 자체가 감수 결정이기도 하다.

나중에 CDN으로 옮기더라도 서버가 키 → URL 해석만 바꾸면 되어 프론트 계약이 깨지지 않는다.

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

#### 음성 큐잉 대본 — 우리가 소유하고, 컬럼이 아니라 테이블로 둔다

**대본을 `catalog`가 소유한다.** YMove의 `instructions`는 영어 원문이고, 우리는 그것을 한글로
옮긴 **번역본을 소유**한다(§4-3-1). 요가 큐잉은 번역 품질이 곧 지도 품질이라 원문을 그대로
읽어줄 수 없고, 옮긴 결과는 감수 대상 seed다(§6).

**컬럼 하나가 아니라 별도 테이블이다.** `exercise.voice_guide_script TEXT` 한 컬럼으로 두면
지금은 충분하지만, 세션 플레이어의 MVP 기능이 "음성 큐잉·카운트·타이머"라 큐가 재생 시각에
붙어야 할 가능성이 남아 있다. 그때 `TEXT` → 테이블 전환은 **데이터 변환 changeset**을 요구하는데
`docs/architecture.md` §6이 "이미 적용된 changeset은 수정하지 않는다"고 못박았다. 반대로 처음부터
행 단위로 쪼개 두면 확장이 값 채우기로 끝난다.

- `display_order` — **지금의 재생 순서.** 타임코드가 없어도 순차 재생이 성립한다
- `start_offset_seconds` `end_offset_seconds` — **타임코드. 확정 전에는 둘 다 `NULL`이다.**
  확정되면 `UPDATE` changeset으로 값만 채운다. **스키마를 바꾸지 않는다**
- `content` — 한글 번역 대본

**큐를 순간이 아니라 구간으로 둔다.** 핀포즈가 "40초 × 3"처럼 유지 구간을 갖고 세션 플레이어가
카운트다운(`00:35`)을 그리므로, 큐가 언제 끝나는지가 화면에 필요하다. 끝을 다음 큐의 시작으로
추론하면 두 가지를 못 한다 — 큐 사이에 침묵 구간을 두는 것과, 마지막 큐의 끝을 아는 것이다.

`end_offset_seconds`는 유지 구간이 없는 큐에서 `NULL`로 남는다. `end`만 있고 `start`가 없는
상태는 `CHECK`으로 막는다.
- PK는 서로게이트 `cue_id`, 순서 중복은 `UNIQUE (exercise_id, display_order)`로 막는다.
  `(exercise_id, display_order)`를 PK로 쓰면 재감수로 큐 순서가 바뀔 때마다 식별자가 흔들린다.
  `course.course_step_exercise`·`screening.screening_answer`도 자식 테이블에 서로게이트 키를 쓴다
- `exercise_id`는 **`catalog` 내부 FK를 건다.** 금지된 것은 도메인 간 FK다(§6)

**`target_pose`에는 두지 않는다.** 세션 재생 경로가 지나는 것은
`course.course_step_exercise → exercise_id`뿐이고, `training`이 `catalog:contract`로 읽는 것도
운동 상세다(§3). 핀포즈 역시 `exercise` 행으로 존재한다 — §7-10이 MET을 "준비 동작과 핀포즈에
각각" 부여한다고 적었고 `met_value`는 `exercise`에만 있다. `target_pose`는 진단 그리드와
사다리·도장의 마스터이지 재생 단위가 아니다.

**`audio_asset_key`를 지금 넣지 않는다.** 서버 TTS 산출물을 쓸지 클라이언트가 텍스트로 읽을지
정해지지 않았고(§7-14), `docs/architecture.md` §3·§4의 "미리 만들지 않는다"에 걸린다. 별도
테이블과 판단이 갈리는 이유는 **되돌리기 비용이 다르기 때문**이다 — 컬럼 추가는 `ADD COLUMN`
changeset 한 줄이지만 컬럼 → 테이블 전환은 데이터 변환을 동반한다.

**YMove 영문 원문(`source_content`)도 저장하지 않는다.** §4-3-1이 YMove 소유 값을 중복 저장하지
않기로 했고, 원문이 필요한 곳은 런타임이 아니라 번역·감수 과정이다. 원문이 바뀌었을 때 번역본을
어떻게 갱신할지는 §7-13에 열린 질문으로 남긴다. 필요해지면 이것도 `ADD COLUMN`이다.

#### 4-3-1. YMove 연동 — `catalog`가 순수 seed 도메인이 아닌 이유

영상과 음성은 YMove를 쓴다. **`videos[].videoUrl`이 48시간 만료라 DB에 넣을 수 없다.**
seed changeset에 박으면 이틀 뒤 전부 죽는다. 그래서 `catalog`는 "어떤 YMove 자세를 쓰는가"와
감수로 덧붙인 것만 소유하고, 재생에 필요한 값은 요청 시점에 YMove에서 읽는다.

경계는 **`ymove_slug`**다. 우리 seed는 slug만 들고, 나머지는 YMove가 정본이다.

| 값 | 소유 | 이유 |
| --- | --- | --- |
| `ymove_slug` | `catalog` seed | 어떤 자세를 쓸지는 우리 감수 결정이다 |
| `videoUrl` `thumbnailUrl` | YMove (매 요청) | 48시간 만료. 캐시 TTL을 그보다 짧게 둔다 |
| `instructions` | **`catalog` seed (번역본)** | 음성 큐잉 대본. **한 번 YMove로 넘겼다가 되찾아왔다.** 아래 참고 |
| `title` `description` `videoDurationSecs` | YMove | 중복 저장하지 않는다 |
| `name` | `catalog` seed | YMove `title`을 우리 표현으로 덮어야 할 때만 쓰는 override |
| `difficulty` | `catalog` seed | **YMove 값을 쓰지 않는다.** 아래 참고 |
| `muscleGroup` | `catalog` seed | 근육맵·부위 탭이 필요해 우리 마스터로 갖는다(§4-3) |
| `default_set_count` `default_rep_count` `default_duration_seconds` | `catalog` seed | YMove에 없다. 감수 대상이다 |
| `met_value` | `catalog` seed | YMove에 없다. 칼로리 계산 입력이다 |
| `caution_note` | `catalog` seed | 주의사항 문구. 운동 가이드의 주의사항 탭에 쓴다. 아래 참고 |

**대본을 되찾아온 이유.** 처음에는 "YMove가 `instructions`를 준다"는 사실만 보고 대본을 통째로
YMove 소유로 넘겼고, 그 결과로 `voice_guide_script` 컬럼을 지웠다. 그 판단이 놓친 것은 **YMove의
`instructions`가 영어**라는 점이다. 음성 큐잉은 세션 중에 **읽어주는** 문장이라 언어가 맞지 않으면
기능 자체가 성립하지 않고, 요가 큐잉은 번역 품질이 곧 지도 품질이라 기계 번역을 그대로 태울 수도
없다. 그래서 **번역을 전처리로 돌리고 그 산출물을 우리가 소유한다.** 우리가 감수해서 만든 값이라
`difficulty`·`met_value`와 같은 성격이 됐고, 소유가 `catalog`로 돌아오는 것이 일관된다.
넘겼다 되찾은 대상은 **번역본**이고, 영어 원문의 정본은 여전히 YMove다.

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

- **재생 시점에 YMove에서 받는 값은 `videoUrl`·`thumbnailUrl` 둘뿐이다.** 대본이 우리 것이 되면서
  외부 호출로만 얻을 수 있는 값의 범위가 그만큼 좁아졌다.
- **YMove가 죽어도 대본은 살아 있다.** `catalog.exercise_voice_cue`는 우리 DB이므로 외부 장애와
  무관하다. 다만 **영상 없이 세션을 진행할 수 없다는 사실은 그대로**여서 fallback은 여전히 없다.
  대본이 남는다는 것은 "장애 시 음성만으로 진행"이라는 선택지가 **생겼다**는 뜻이지 그렇게 하기로
  정했다는 뜻이 아니다 — 타임아웃과 실패 시 사용자에게 보일 메시지를 `course`·`training` 착수
  전에 정해야 한다(§7-4).
- `training`이 세션 중 운동 상세를 `catalog:contract`로 읽는데, 그 응답에 재생 URL이 실리면
  스텝마다 YMove를 친다. 캐시 위치와 TTL을 정해야 한다(§7-5).
- 홈·코스 목록이 스텝마다 썸네일과 시간을 그린다. **목록 조회에서 YMove를 스텝 수만큼 치면
  안 된다.** `difficulty` `default_duration_seconds`를 seed로 내린 이유이기도 하다.

### 4-4. `course`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Course`(루트) + `CourseStep` + `CourseStepExercise`, `Stamp`(루트) |
| 마스터 | `CourseTemplate` + `TemplateStep`, 자세 사다리 — seed |
| Command | 코스 처방, 스텝 완료, 세션 완료 반영(도장·해금) |
| Query | 코스 상세(스텝+운동), 진행도, 획득한 도장 |
| 모듈 | 기본 6 + `contract` + `adapter-screening` + `adapter-catalog` + `adapter-member` = **10** |

```text
course.course_template          template_id(pk), target_pose_id(uk), name,
                                recommendation_reason                             [seed]
course.template_step            template_step_id(pk), template_id, step_order     [seed]
course.template_step_exercise   template_step_exercise_id(pk), template_step_id,
                                exercise_id, display_order,
                                duration_seconds, set_count                       [seed]

course.course                   course_id(pk), member_id, template_id, target_pose_id,
                                cause_code(null), status, created_at, completed_at
                                UNIQUE (member_id, target_pose_id)
course.course_step              course_step_id(pk), course_id, step_order, status, completed_at
course.course_step_exercise     course_step_exercise_id(pk), course_step_id, exercise_id,
                                display_order, duration_seconds, set_count
course.stamp                    stamp_id(pk), member_id, target_pose_id, course_id, acquired_at
                                UNIQUE (member_id, target_pose_id)
```

`course.course`에 `version`이 있다. **동시 세션 완료가 서로를 덮지 않게 하는 낙관적 락**이다 —
애그리거트를 통째로 저장하므로 버전이 없으면 나중 저장이 앞선 완료를 지운다. 충돌하면 서비스가
다시 읽어 한 번 재시도한다.

**하나의 핀포즈가 곧 하나의 코스다.** 회원이 고른 (강화 부위, 난이도)가 곧
`catalog.target_pose`의 (부위, 레벨)이고 그 자세의 템플릿으로 코스가 만들어진다. 그래서
`course_template`의 자연키가 `target_pose_id`다.

- **`scheduled_on`이 없다.** 초판은 홈이 "오늘의 코스 / 내일의 코스"를 보여준다는 전제로
  코스를 일자 단위로 뒀는데, 확정된 화면에 내일의 코스가 없다. **진행 중인 코스가 곧 오늘의
  코스**다(§7-9 해소).
- **`unlock_required_target_pose_id`가 없다.** 회원이 난이도를 직접 고르므로 해금 사다리가
  없다(§7-2 재해소).
- **`course_template.cause_code`가 없다.** 처방 입력이 (부위, 레벨)로 바뀌었다. 원인은 여전히
  쓰이되 "회원이 고른 부위가 실제 진단 결과에 있는가"를 **검증**하는 데만 쓴다(§2). 검증에 쓴
  원인은 `course.cause_code`에 스냅샷으로 남는다 — 재진단으로 원인이 바뀌어도 이 코스가 왜
  처방됐는지는 남아야 한다.
- **`(member_id, target_pose_id)` 유니크가 처방 멱등성을 만든다.** 같은 요청이 재시도돼도 새
  코스가 생기지 않고 이미 있는 코스가 돌아간다. 조회와 저장 사이의 틈은 제약 위반을 잡아
  다시 읽는 것으로 메운다 — 조회만으로는 동시 요청을 막을 수 없다.
- **도장은 `ON CONFLICT DO NOTHING` 한 문장으로 넣는다.** "있는지 보고 없으면 넣는다" 로
  짜면 두 요청이 확인을 함께 통과해 유니크 제약에 걸린다. 새로 붙었는지는 저장 결과가 알려주고,
  서비스가 "방금 완료됐나" 로 짐작하지 않는다.
- **`IN_PROGRESS` 코스는 회원당 여럿일 수 있다.** 도전 현황 화면이 `도전 중 3`을 동시에
  보여주므로 하나로 제한하지 않는다. 홈의 "오늘의 코스" 는 그중 **가장 최근에 처방된 것**이다.
- **진행도는 `course_step`의 완료 개수 / 전체 개수**다. 자세 도전 현황의 `3 / 4`가 이 값이고,
  전부 완료하면 `Stamp`가 붙는다(§7-8 해소). 별도의 "필요 횟수" seed가 필요 없다.
- `current_step_order` 컬럼을 두지 않는다. 스텝 상태에서 계산한다 — 컬럼으로 두면 스텝 완료와
  커서 갱신이 어긋날 수 있는데 그 상태를 표현할 이유가 없다.
- `duration_seconds` `set_count`는 nullable이며 비어 있으면 `catalog.exercise`의 기본값을 쓴다.
- `exercise_id` `target_pose_id` `member_id`는 전부 값 컬럼이고 FK가 없다.
- `reinforcement_rule`과 `course_step_exercise.source`는 **없다.** 보강 편성이 사라졌다(§4-3).

### 4-5. `training`

| 항목 | 내용 |
| --- | --- |
| 애그리거트 | `Session`(루트) + `SessionExerciseRecord` |
| Command | 세션 시작, 세션 완료 |
| Query | 세션 조회(복구) |
| 모듈 | 기본 6 + `adapter-course` + `adapter-catalog` = **8** |

```text
training.session                  session_id(pk), member_id, course_id, step_order,
                                  status, started_at, completed_at
training.session_exercise_record  record_id(pk), session_id, course_step_exercise_id,
                                  exercise_id, display_order,
                                  completed, performed_duration_seconds
                                  UNIQUE (session_id, course_step_exercise_id)
```

세션 완료 이후 `CourseProgressPort`로 `course`에 밀어넣는다.
**`training`에는 도장·진행도 판단이 없다.** 그 로직이 여기 생기면 잘못 나눈 것이다.

- 세션 시작 시 **코스 스텝 구성을 복사**해 `completed = false`로 만들어 두고 완료 요청이 값을
  채운다. 복사하는 이유는 세션 중 코스가 바뀌어도 이 세션이 무엇을 수행했는지가 흔들리면 안
  되기 때문이다.
- `course_step_exercise_id`를 함께 두는 것은 `exercise_id`만으로는 같은 운동이 한 스텝에 두 번
  편성된 경우를 가릴 수 없어서다. `exercise_id`도 남기는 것은 catalog 조회에 코스를 다시 읽지
  않기 위해서다.
- **완료는 멱등하다.** 이미 완료된 세션은 기록을 덮어쓰지 않고, `course`로의 push 는 다시 하되
  `course` 애그리거트가 흡수해 진행도가 두 번 오르지 않는다(§7-8).
- **요청에 없는 운동은 수행하지 않은 것으로 남는다.** 부분 완료가 정상이다.
- 경로는 `/sessions` 아래다. 이 저장소가 경로 앞부분으로 도메인을 가르고 있어
  `POST /courses/{courseId}/sessions`는 그 규칙과 어긋난다.
- **휴식 타이머·±10초·이전/다음·음성 재생 전환에 API 를 두지 않는다.** 전부 클라이언트
  동작이고 휴식 타이머는 와이어프레임에서 deprecated 처리됐다.

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
             adapter-screening adapter-catalog adapter-member
training:    model infrastructure service repository-jdbc api schema
             adapter-course adapter-catalog
```

패키지 루트는 `team.aligner.{domain}`이다(§10).

현재 `settings.gradle.kts`는 루트 3개에 더해 `member` 8개, `catalog` 7개, `screening` 7개,
`course` 10개, `training` 8개를 include 한다. `build-logic`은 `includeBuild` 대상이다.
**도메인 5개가 모두 들어왔다.** `catalog`의 `adapter-ymove`만 §7-4·5·6이 정해진 뒤 후속으로
붙인다.

`course`의 adapter 가 셋인 것은 처방에 원인 검증(`screening`), 자세·운동 조회(`catalog`),
칼로리 계산용 몸무게(`member`)가 모두 필요하기 때문이다(§3).

도메인이 다 구현되면 `application-api`는 5개 도메인의 `api` · `repository-jdbc` · `schema` ·
`adapter-*`와 `member:contract` · `support-web` · `support-core`를 조립한다.

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

~~`course`는 §7-8·§7-9가 정해지기 전에는 착수할 수 없다.~~ **둘 다 해소돼 착수했다.**
`course`는 `member`(몸무게)·`catalog`(자세·운동)·`screening`(원인)이 모두 있어야 하므로
의존 말단 중 가장 마지막이다.

---

## 6. 스키마 계획

`docs/architecture.md` §6 적용.

- schema 5개. 각 도메인 changelog 첫 changeset에 `CREATE SCHEMA IF NOT EXISTS {domain}`.
- 모든 DDL은 schema-qualified. 엔티티에 `@Table(schema = "{domain}", ...)`, `JdbcClient` SQL도 동일.
- 루트 `changelog-master.yaml`에 5개 include를 추가한다.
- **도메인 간 FK 없음.** `member_id` `exercise_id` `target_pose_id` `muscle_code` `cause_code`는
  전부 값 컬럼이다. 존재 검증이 필요하면 port로 한다.
- seed는 `schema/seed/`의 changeset으로 넣는다. 감수 전 데이터가 들어가는 곳은 여섯이다.

| seed | 위치 | 감수 대상 |
| --- | --- | --- |
| 부위·원인·자세 체감 분기 규칙(`weight` 포함) | `screening/schema/seed/` | ✅ |
| 운동·목표 자세·레벨·난이도·MET·주의사항 | `catalog/schema/seed/` | ✅ |
| 근육 마스터·자세↔근육·운동↔근육 | `catalog/schema/seed/` | ✅ |
| 운동별 음성 큐잉 번역 대본(순서·타임코드) | `catalog/schema/seed/` | ✅ |
| 원인별 코스 템플릿·스텝 구성 | `course/schema/seed/` | ✅ |
| 자세 사다리(선행 자세) | `course/schema/seed/` | ✅ |

**번역 대본이 감수 대상인 근거.** 세션 중에 읽어주는 문장이라 잘못된 큐가 곧 잘못된 지도이고,
`docs/architecture.md` §6이 감수 전 데이터의 하드코딩을 금지한다. 원문이 YMove 것이어도 **옮긴
결과는 우리 판단이 들어간 값**이다 — 기계 번역 산출물을 그대로 넣더라도 감수를 거치지 않은 채
사용자에게 읽히면 안 된다. 전처리를 어디서 돌릴지는 §7-12에 남아 있다.

이 여섯 중 어느 것도 코드에 하드코딩하지 않는다. 하드코딩은 그 자체로 `[필수]` 지적이다.

---

## 7. 열린 질문 — 구현 전에 답해야 한다

1. **운동의 수행 시간·세트를 누가 정하는가.** 위 설계는 `catalog.exercise`에 기본값을 두고
   `course.course_step_exercise`가 필요할 때만 덮어쓰는 방식이다. 같은 운동이 코스마다 다른
   시간을 갖는 사례가 콘텐츠 정본에 있으므로(같은 준비 동작이 레벨별로 1분 30초·2분·2분 30초)
   override 컬럼은 유지한다. 세트 override까지 필요한지는 확인이 남았다.
2. ~~**자세 해금 사다리의 표현.**~~ **해소 — 사다리 자체가 없다.** 회원이 온보딩과 마이페이지에서
   **난이도를 직접 고르고 그 난이도가 곧 자세 레벨**이다. 잠금이 없으므로
   `unlock_required_target_pose_id`도 만들지 않는다.
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
8. ~~**완수·도장의 판정 기준.**~~ **해소.** 하나의 핀포즈가 하나의 코스이고, 도전 현황의
   `3 / 4`는 **그 코스 안에서 완료한 스텝 개수**다. 전부 완료하면 `완성`이고 그때 `Stamp`가
   붙는다. 자세 포인트도, `target_pose`의 "필요 횟수" seed도 필요 없다 — 진행도는
   `course_step` 완료 상태의 집계다.

   `CourseProgressContract`도 확정했다. `completeSession`을 **단일 진입점**으로 두고
   `completeStep`은 만들지 않는다 — 스텝 완료를 세션 완료와 따로 부를 주체가 이 설계에 없다.
   재시도는 애그리거트가 흡수한다. 이미 완료된 스텝을 다시 완료해도 진행도가 오르지 않고
   도장도 한 번만 붙는다.
9. ~~**오늘 코스와 내일 코스의 차이.**~~ **해소 — 내일의 코스가 없다.** 살아 있는 화면에
   "내일 코스"가 나오는 곳이 없고, 유일한 언급이 deprecated 처리된 체크 화면이었다.
   **진행 중인 코스가 곧 오늘의 코스**이므로 `course.scheduled_on`을 만들지 않는다.
   진행 중인 코스가 여럿이면 가장 최근에 처방된 것을 홈에 그린다.
10. **MET 값의 출처와 보간.** 레벨별 값이 표준 참조의 보간이라 감수가 필요하다. 운동 단위로
    값을 부여하는 이상 준비 동작과 핀포즈에 각각 무엇을 줄지도 감수 대상이다.
11. **`AGENTS.md` 갱신.** §1 핵심 루프의 `PoseCheckpoint 확인 → Stamp/다음 코스 보강`,
    §2 용어집의 `PoseCheckpoint` 행이 이 문서와 어긋난다. §2에 `CourseTemplate` 행을 추가하는
    것도 남아 있다. `AGENTS.md`는 루트 판단 문서라 **별도 승인 후** 갱신한다.
12. **번역 전처리를 언제·어디서 돌리는가.** YMove `instructions`(영어) → 한글 대본으로 옮기는
    작업의 위치가 미정이다. 오프라인 스크립트로 돌려 산출물을 `catalog/schema/seed/` changeset에
    박는 방식이 유력하나(감수 대상이라 결과가 고정돼야 한다), 런타임 번역이라면 `adapter-ymove`
    이후로 밀린다. **스크립트를 이 저장소에 둘지도 정하지 않았다** — 두면 `build-logic`·모듈
    레이아웃(`docs/architecture.md` §3) 어디에도 자리가 없는 새 범주가 생긴다.
13. **YMove 원문이 바뀌면 번역본을 어떻게 갱신하는가.** 우리는 번역본만 갖고 영어 원문은 저장하지
    않으므로(§4-3), 원문이 조용히 바뀌어도 우리 대본은 그대로다. §7-6의 `ymove_slug` 안정성과
    같은 성격의 문제다 — 감지 수단이 필요하면 원문 해시나 `source_content` 컬럼을 `ADD COLUMN`으로
    붙인다. **YMove가 원문 변경을 통지하는지부터 확인이 필요하다.**
14. **음성 큐의 산출물이 텍스트인가 오디오인가.** 서버에서 TTS를 돌려 오디오 파일을 만들어 두면
    `exercise_voice_cue`에 `audio_asset_key`가 붙고 정적 asset 저장소가 필요해진다(§4-3의
    `front_highlight_asset_key`와 같은 형태). 클라이언트가 Web Speech로 읽으면 텍스트만 내리면 된다.
    "음악 위에 얹기·백그라운드 재생"(리서치 IA 3번)이 어느 쪽으로 가능한지가 판단 근거인데
    **확인되지 않았다.** 결정 전에는 컬럼을 만들지 않는다.
15. **타임코드를 언제 확정하는가.** `exercise_voice_cue`의 `start_offset_seconds`·
    `end_offset_seconds`는 확정 전까지 `NULL`이고 재생은 `display_order` 순차로 한다. 큐를 영상
    재생 시각에 맞출지, 클라이언트 타이머에 맞출지, 아니면 순차 재생으로 MVP를 끝낼지가 미정이다. **스키마는 이 결정을 기다리지 않아도 되지만**
    (§4-3 — 확정 시 `UPDATE` changeset), seed 값을 두 번 만들지 않으려면 seed 이슈 전에 정하는
    편이 낫다.
16. **콘텐츠 운영 API를 만들 것인가.** 운동·자세·근육·음성 큐를 감수자가 직접 추가·수정하는
    화면과 API가 언젠가 필요하다. **MVP에서는 만들지 않는다** — 콘텐츠가 고유 자세 32개로
    고정이고 감수 주기가 길어 seed changeset으로 충분하다.

    지금 구조가 이것을 막지는 않는다. `catalog`에 쓰기가 붙는 것은 전부 **추가**다 — 애그리거트
    클래스, `CommandService`, 쓰기 port, `@Table` 엔티티를 그때 만들면 되고 스키마는 그대로다.
    `created_at`·`updated_at`도 그때 `ADD COLUMN`한다. seed만 있는 지금은 감사 시각이 의미가 없다.

    다만 착수 전에 답해야 할 것이 셋이다.

    - **seed와 운영 API가 같은 테이블을 쓰면 DB 상태가 changeset과 갈라진다.** 환경마다 콘텐츠가
      달라지고 감수 이력이 Git에 남지 않는다. 운영 API로 넣은 것을 changeset으로 역수출할지,
      아니면 처음부터 한쪽만 쓸지 정해야 한다
    - **어디에 배포하나.** `application-api`는 회원용이고 운영 API는 보안 경계가 다르다.
      `docs/architecture.md` §2가 실행 모듈을 `application-api` 하나로 두고 있어, 운영 앱을
      따로 두는 것은 그 결정을 건드린다
    - **감수자가 직접 쓰는가.** 감수자가 쓰는 화면이면 프론트가 하나 더 필요하다. 개발자가 쓰는
      내부 도구라면 seed changeset과 실익 차이가 크지 않다

## 8. 이 분할에서 아직 정하지 않은 것

`docs/architecture.md` §11에 남은 것 중 **코루틴 사용 범위**와 **모듈 의존성 검증 태스크**는
여전히 미정이다. 도메인 분할만 이 문서로 확정됐다.
