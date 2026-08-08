# 프론트엔드 연동 가이드

이 문서는 현재 `develop`에 병합된 서버를 기준으로, 프론트엔드가 어떤 API를 호출하고 응답을
어떻게 해석해야 하는지 정리한 문서다.
기획상 전체 MVP의 설명이 아니라 **현재 실제 코드에 존재하는 계약**을 설명한다.

## 먼저 알아둘 현재 상태

현재 서버에서 실행 코드가 있는 범위는 다음과 같다.

| 범위 | 상태 | 프론트에서 할 수 있는 일 |
| --- | --- | --- |
| `support-web` 인증 | 구현됨 | 카카오 인가 코드로 Aligner JWT 발급 |
| `member` | 구현됨 | 프로필 조회, 온보딩 입력(경력·키·몸무게·강화 부위/난이도) 수정, 회원탈퇴 |
| `catalog` | 구현됨 | 목표 자세 목록·상세, 운동 상세 조회 |
| `screening` | 구현됨 | 부위 목록, 자세 체감 제출·원인 판별, 최신 결과 조회 |
| `course` | 미구현 | 맞춤 코스·스텝·진행도 API 없음 |
| `training` | 미구현 | 세션 시작·완료·수행 기록 API 없음 |

현재 Liquibase changelog는 `member`·`catalog`·`screening`의 **테이블만 생성**한다. 감수 콘텐츠
seed는 아직 없으므로 새 DB에서는 catalog 목록과 부위 목록이 빈 배열이고, 존재하지 않는 ID의
상세 조회는 404가 정상일 수 있다. 개발용 화면에서 임의 ID를 고정하지 말고 목록 응답의 ID를
사용한다.

**seed가 없는 동안 스크리닝 제출은 성공할 수 없다.** 서버가 정상이어도 그렇다. seed가 어디까지
들어왔는지에 따라 실패 지점이 다르다.

| 서버 상태 | `GET /catalog/target-poses` | `POST /screening/results` |
| --- | --- | --- |
| seed 없음 (지금) | `[]` | 400 `EMPTY_SCREENING_ANSWER` — 고를 자세가 없다 |
| 자세 seed만 있음 | 자세 그리드 | 422 `CAUSE_NOT_DETERMINED` — 분기 규칙이 없다 |
| 분기 규칙까지 있음 | 자세 그리드 | 200 |

지금 화면을 만든다면 세 경우를 모두 처리해 두는 편이 낫다. 어느 것도 프론트 요청이 틀려서 나는
오류가 아니다.

`bodyPartCode`의 값 집합은 이제 `screening`이 소유하며 `GET /screening/body-parts`가 내려준다.
프론트에서 임의의 영문 코드를 만들어 고정하지 말고 이 API의 응답 값을 그대로 쓴다. 구체적인
값은 부위 seed가 들어올 때 확정된다.

## 핵심 용어

| 용어 | 의미 | 프론트에서의 의미 |
| --- | --- | --- |
| `Member` | 카카오 식별자와 프로필을 가진 회원 | 인증 성공 후 내 프로필 화면의 주체 |
| `TargetPose` | 부위별 목표 자세(핀포즈) 마스터 | 온보딩 자세 그리드·자세 상세에 표시할 콘텐츠 |
| `Exercise` | 코스에서 수행하는 보강 운동 마스터 | 운동 가이드·세션 플레이어가 표시할 콘텐츠 |
| `Muscle` | 자세·운동에 연결된 근육 마스터 | 근육맵·부위 탭에 표시할 콘텐츠 |
| `MuscleRole` | 근육을 늘리는지 강화하는지 | `STRETCH` 또는 `STRENGTHEN`으로 구분 |
| `imageAssetKey` | 이미지 파일을 가리키는 안정적인 키 | URL로 바로 렌더링하지 말고 프론트 asset 매핑에 사용 |
| `bodyPartCode` | 자세·근육이 속한 부위 코드 | `GET /screening/body-parts`가 소유하는 문자열 |
| `metValue` | 운동 칼로리 계산에 쓰는 MET 값 | kcal 자체가 아니며 서버가 몸무게를 합쳐 계산하지 않음 |
| `voiceCue` | 운동 중 보여줄 한글 큐잉 문장 | `displayOrder` 순서로 재생·표시 |
| `BodyPart` | 부위 | 진단 결과 뒤 **강화할 부위**를 고르는 화면의 선택지 |
| `PerceivedDifficulty` | 자세를 해보고 느낀 체감 | `EASY`(쉬웠다) 또는 `HARD`(어려웠다) |
| `Cause` | 판별된 **원인** | 결과 화면이 보여줄 진단. `rank` 1이 가장 유력 |
| `ScreeningResult` | 제출 1회의 진단 결과 | 원인 목록을 담은 응답. `resultId`로 식별 |

`TargetPose`는 목표 자세 콘텐츠의 이름이다. 예전 기획 용어인 `PoseCheckpoint`를 현재
서버가 제공한다고 가정하면 안 된다. 현재 도메인 결정에서는 `PoseCheckpoint`를 만들지
않으며, `course`·`training` 자체도 아직 구현되지 않았다.

## 기본 연동 흐름

```text
Kakao.Auth.authorize({ redirectUri })
        │  카카오 → HTTP 302 → 프론트 라우트 ?code={인가 코드}
        ▼
POST /auth/kakao  { authorizationCode }
        │  서버가 카카오와 토큰을 교환하고 Aligner access token 발급
        ▼
Authorization: Bearer {alignerAccessToken}
        │
        ├── GET   /members/me
        ├── PATCH /members/me
        ├── DELETE /members/me
        ├── GET   /screening/body-parts
        ├── POST  /screening/results
        ├── GET   /screening/results/latest
        ├── GET   /catalog/target-poses[?bodyPartCode=...]   부위는 선택
        ├── GET   /catalog/target-poses/{targetPoseId}
        └── GET   /catalog/exercises/{exerciseId}
```

로그인 API만 인증 없이 열려 있고, 나머지 현재 API는 모두 Aligner JWT가 필요하다.
카카오 액세스 토큰을 보호 API의 Bearer 토큰으로 사용하지 않는다.

### 온보딩 화면 순서

세 도메인이 한 흐름으로 이어지는 곳이라 호출 순서를 적어둔다.

```text
GET  /catalog/target-poses                       전체 핀포즈 그리드 (부위 파라미터 없이)
        │  회원이 쉬웠던 자세와 어려웠던 자세를 각각 최대 4개 고른다
        ▼
POST /screening/results                          제출 → 판별 → 결과가 한 번에 온다
        │
        ▼
결과 화면 (causes[0] 이 1순위 원인)
        │
        ▼
GET  /screening/body-parts                       강화할 부위 선택지
        │  회원이 강화할 부위와 난이도를 고른다
        ▼
(부위·난이도 저장 API는 아직 없다 — member 확장 이슈)
```

**부위를 먼저 묻지 않는다.** 자세를 먼저 받아 원인을 판별하고, 그 결과를 본 뒤에 강화할 부위를
고르는 순서다. `POST /screening/results` 요청에 `bodyPartCode`를 넣지 않는다.

**`targetPoseId`는 catalog에서 받아 screening으로 넘긴다.** 두 도메인이 서버에서 서로를
참조하지 않으므로 그 연결을 프론트가 들고 있는다. 자세 그리드 응답의 `targetPoseId`를 그대로
제출에 실으면 된다.

## 인증 API

### 로그인은 리다이렉트 왕복이다

**웹에서 클라이언트가 카카오 액세스 토큰을 받는 경로는 없다.** JavaScript SDK v2가 보안 권고에
따라 클라이언트 측 토큰 발급 함수(`Kakao.Auth.login()` 등)를 전부 제거했고, 카카오 문서도 토큰
발급 주체를 서비스 서버로 못박는다. 남은 것은 `Kakao.Auth.authorize()` 하나이고 이것은 인가 코드를
리다이렉트로 넘긴다.

그래서 프론트가 해야 할 일이 셋이다.

1. **인가 코드 착지 라우트를 만든다.** 이 라우트의 URL이 곧 `redirectUri`이고, `authorize()`에
   넘기는 값과 카카오 개발자 콘솔 등록값, 서버 설정값 **셋이 모두 완전히 일치**해야 한다.
   불일치하면 카카오가 교환을 거부한다
2. **`code` 쿼리 파라미터를 읽어 `POST /auth/kakao`로 넘긴다.** 서버가 여기서 토큰 교환을 한다.
   REST API 키와 client secret은 서버에만 있으므로 프론트는 이 값들을 갖지 않는다
3. **리다이렉트 왕복 너머로 화면 상태를 보존한다.** 로그인 시 현재 페이지를 떠나므로, 로그인 전에
   고른 상태(예: `BodyPart` 선택)는 `sessionStorage`나 `state` 파라미터로 넘겨야 한다

인가 코드는 **1회용**이다. 같은 값으로 재시도하면 401이 온다.

### `POST /auth/kakao`

카카오가 리다이렉트로 넘긴 인가 코드를 서버에 보내면, 서버가 카카오 인증 서버에서 액세스 토큰으로
교환하고 사용자 API로 확인한 뒤, 회원을 찾거나 새로 만들어 Aligner 자체 액세스 토큰을 발급한다.

요청:

```http
POST /auth/kakao
Content-Type: application/json

{"authorizationCode":"{kakao-authorization-code}"}
```

응답:

```json
{
  "accessToken": "{aligner-jwt}",
  "expiresIn": 1209600
}
```

`expiresIn`의 단위는 초다. 기본 설정은 14일이지만 서버 환경변수로 바뀔 수 있으므로
응답 값을 기준으로 만료를 관리한다. 서버에는 refresh token과 로그아웃 API가 없다.
로그아웃은 프론트가 보관 중인 Aligner 토큰을 삭제하는 것으로 처리한다.

토큰이 만료되어 401이 오면 refresh API를 찾지 말고 `Kakao.Auth.authorize()`를 다시 태워 인가
코드를 새로 받는다. **이때도 전체 페이지 리다이렉트 왕복이라 화면 전환이 발생한다.** 14일에 한
번이라 감수하기로 한 비용이다. refresh API와 로그아웃 API는 없다.

현재 JSON 필드명은 Kotlin DTO의 이름을 따른 camelCase다. 서버가 별도의 envelope를
씌우지 않으므로 성공 응답은 각 API의 배열 또는 객체 자체이고, 실패 응답만 `code`와
`message` 객체다.

## 회원 API

### `GET /members/me`

현재 JWT의 회원 프로필을 반환한다.

```json
{
  "memberId": 1,
  "nickname": "요가하는 사람",
  "heightCm": 170,
  "weightKg": 60,
  "experienceLevel": "ONE_TO_THREE_YEARS",
  "reinforcementBodyPartCode": "BACK",
  "reinforcementLevel": 1
}
```

`nickname`은 `null`일 수 있다. 카카오 프로필 제공에 동의하지 않은 회원에게 서버가 기본
닉네임을 만들어 주지 않는다.

**`profileImageUrl`은 내려가지 않는다.** 서버가 저장은 하지만 응답에서 뺐다 — 카카오 CDN
링크의 수명을 우리가 보장할 수 없어서다.

**나머지 필드가 전부 `null`이면 온보딩을 끝내지 않은 회원이다.** 홈 진입 시 이 값들로
온보딩으로 보낼지 판단한다.

`experienceLevel`은 `UNDER_ONE_YEAR`(1년 미만) · `ONE_TO_THREE_YEARS`(1~3년) ·
`OVER_THREE_YEARS`(3년 이상) 셋 중 하나다. 표시 문구는 프론트가 그린다.

`reinforcementLevel`은 `1`(하) · `2`(중) · `3`(상)이다.

### `PATCH /members/me`

**온보딩과 프로필 편집이 이 API 하나를 같이 쓴다.** 온보딩 전용 API는 없다.

**보낸 필드만 바뀐다.** 보내지 않은 필드는 그대로 유지되며, `null`을 보내도 값이 지워지지
않는다. 온보딩이 화면마다 조각을 나눠 보내는 구조라 이렇게 잡았다 — 값을 비우는 수단은
현재 없다.

```http
PATCH /members/me
Authorization: Bearer {alignerAccessToken}
Content-Type: application/json

{"heightCm":170,"weightKg":60}
```

화면별로는 이렇게 나눠 보내면 된다.

| 화면 | 보낼 필드 |
| --- | --- |
| 온보딩 — 운동 경력 | `experienceLevel` |
| 온보딩 — 키·몸무게 | `heightCm`, `weightKg` |
| 온보딩 — 강화 부위·난이도 / 마이 "난이도 조정하기" | `reinforcementBodyPartCode` + `reinforcementLevel` |
| 프로필 편집 | 바꾼 필드만 |

**`reinforcementBodyPartCode`와 `reinforcementLevel`은 함께 보내야 한다.** 한쪽만 보내면
400 `INVALID_REINFORCEMENT_SETTING`이다. 한 화면에서 같이 고르는 값이라 서버가 짝을 강제한다.

검증 규칙이다. 어기면 400이므로 프론트에서 먼저 막는 편이 낫다.

- 닉네임은 앞뒤 공백을 제거한 뒤 1자 이상 50자 이하 — `INVALID_NICKNAME`. 성공 응답에는 trim된 값이 들어간다
- 키는 100 이상 250 이하 — `INVALID_HEIGHT`
- 몸무게는 20 이상 300 이하 — `INVALID_WEIGHT`
- 강화 난이도는 1 이상 3 이하 — `INVALID_REINFORCEMENT_SETTING`

### `DELETE /members/me`

회원탈퇴다. 성공하면 **204이고 본문이 없다.**

**서버는 회원 행을 지우지 않는다.** 운동 기록을 보존하기로 했고 그 기록이 회원 식별자로
붙어 있어서, 남는 개인정보인 카카오 식별자만 지우고 탈퇴 표시를 남긴다.

프론트가 알아야 할 것은 둘이다.

- 탈퇴 뒤에는 **가지고 있던 토큰이 무효**나 다름없다. 만료 전이어도 모든 API가 404
  `MEMBER_NOT_FOUND`를 낸다. 탈퇴 성공 시 저장한 JWT를 즉시 지우고 로그인 화면으로 보낸다
- 같은 카카오 계정으로 **다시 가입할 수 있지만 새 회원**이 된다. 이전 기록은 이어지지 않으므로
  "복구" 안내를 하면 안 된다

이미 탈퇴한 회원이 다시 호출하면 404다. 첫 요청으로 목적이 이미 달성된 상태이므로 화면은
성공과 같게 다뤄도 된다.

## 스크리닝 API

온보딩의 핵심이다. **회원은 자세만 고르고 부위는 서버가 판별한다.** 결과 화면은
`causes[].bodyPartCode`를 기준으로 그린다.

### `GET /screening/body-parts`

**강화할 부위를 고르는 화면**의 선택지다 — 온보딩 첫 화면이 아니라 진단 결과 다음 화면이다.
판별된 원인의 부위만이 아니라 전체 부위를 내리므로, 회원은 분석 결과에 없는 부위도 고를 수 있다.
화면 노출 순서로 정렬돼 있으므로 프론트에서 다시 정렬하지 않는다.

```json
[
  { "bodyPartCode": "NECK_SHOULDER", "name": "목·어깨" }
]
```

부위 seed가 들어오기 전에는 빈 배열 `[]`이다.

### `POST /screening/results`

**제출과 판별이 한 요청에서 끝난다.** 응답만 저장하고 결과를 따로 조회하는 2단계가 아니다.
요청 한 번에 저장·판별·결과 반환이 모두 일어나므로, 화면은 "선택 → 결과" 한 걸음으로 만든다.

요청:

```http
POST /screening/results
Authorization: Bearer {alignerAccessToken}
Content-Type: application/json

{
  "answers": [
    { "targetPoseId": 12, "perceivedDifficulty": "EASY" },
    { "targetPoseId": 15, "perceivedDifficulty": "HARD" }
  ]
}
```

응답:

```json
{
  "resultId": 3,
  "causes": [
    {
      "causeCode": "THORACIC_STIFFNESS",
      "name": "굳은 흉추",
      "bodyPartCode": "UPPER_BACK",
      "description": "결과 화면에 보여줄 설명",
      "rank": 1,
      "score": 7
    }
  ],
  "createdAt": "2026-08-07T10:00:00Z"
}
```

제출 규칙은 다음과 같다. 어기면 400이므로 프론트에서 먼저 막는 편이 낫다.

- `answers`가 비면 안 된다 — `EMPTY_SCREENING_ANSWER`
- `EASY`와 `HARD` **각각 최대 4개**다. 합계 8개가 아니라 체감별로 4개다 — `TOO_MANY_SCREENING_ANSWERS`
- 같은 `targetPoseId`를 두 번 넣을 수 없다. `EASY`와 `HARD`에 나눠 넣는 것도 중복이다 —
  `DUPLICATE_SCREENING_ANSWER`

`causes`는 **`rank` 오름차순**으로 온다. `rank`가 1인 것이 가장 유력한 원인이다. `score`는 분기
규칙 가중치의 합이고 순위의 근거다 — 화면에 그대로 노출할 값은 아니지만, 순위가 왜 그렇게
나왔는지 확인할 때 쓴다. `description`은 `null`일 수 있다.

**422 `CAUSE_NOT_DETERMINED`를 화면에서 처리해야 한다.** 고른 자세 조합이 어떤 분기 규칙에도
걸리지 않으면 원인 0개인 진단을 저장하지 않고 422로 돌려준다. 회원이 뭘 잘못한 것이 아니라
서버 seed가 그 조합을 덮지 못한 것이므로, "다시 입력하세요"가 아니라 "결과를 낼 수 없다"에
가까운 안내가 맞다.

검증 순서는 **제출 규칙 → 판별**이다. 부위를 받지 않으므로 `BODY_PART_NOT_FOUND`는 이 API에서
더 이상 나오지 않는다. 위 「먼저 알아둘 현재 상태」의 표가 seed 상태별로 어디서 끊기는지
정리해 뒀다.

### `GET /screening/results/latest`

회원이 가장 최근에 받은 진단이다. 응답 모양은 `POST /screening/results`와 같다.

**진단한 적이 없으면 404 `SCREENING_RESULT_NOT_FOUND`다.** 빈 응답이 아니다. 화면은 이 404를
"온보딩으로 보내라"는 신호로 읽는다 — 홈 진입 시 이 API가 404면 온보딩, 200이면 결과·코스
화면으로 분기하는 것이 의도된 사용법이다.

남의 `resultId`는 볼 수 없다. 서버가 회원 식별자를 조건에 함께 넣으므로 존재하지 않는 진단과
남의 진단이 똑같이 404로 온다.

## Catalog API

catalog는 회원별 데이터가 아닌 조회 전용 마스터 데이터다. 현재 API도 인증은 필요하지만
회원 ID에 따라 결과가 달라지지 않는다.

### `GET /catalog/target-poses?bodyPartCode={code}`

목표 자세 그리드용 요약 목록을 반환한다. 결과는 `bodyPartCode`, `level`, `targetPoseId`
순으로 정렬된다.

**`bodyPartCode`는 선택 파라미터다.** 생략하면 전체 핀포즈가 온다 — 온보딩 그리드가 부위로
걸러지지 않고 전체를 펼치므로 그쪽이 기본 사용법이다. 부위를 주면 그 부위만 걸러 온다.

```json
[
  {
    "targetPoseId": 1,
    "name": "예시 자세",
    "imageAssetKey": "target-pose/example",
    "bodyPartCode": "{code}",
    "level": 1
  }
]
```

목록에는 근육 정보가 없다. 카드·그리드는 이 API만 사용하고, 자세 상세가 필요할 때
아래 상세 API를 호출한다. 해당 부위가 없거나 알 수 없는 코드면 오류가 아니라 빈 배열
`[]`이 반환된다.

### `GET /catalog/target-poses/{targetPoseId}`

자세 상세와 연결된 근육을 반환한다.

```json
{
  "targetPoseId": 1,
  "name": "예시 자세",
  "imageAssetKey": "target-pose/example",
  "bodyPartCode": "{code}",
  "level": 1,
  "muscles": [
    {
      "muscleCode": "muscle-example",
      "name": "예시 근육",
      "bodyPartCode": "{code}",
      "highlightAssetKey": "muscle/example",
      "role": "STRETCH",
      "displayOrder": 1
    }
  ]
}
```

`role`은 현재 `STRETCH`(신장)와 `STRENGTHEN`(강화)만 사용한다. 근육 배열은
`displayOrder` 순서로 표시한다.

### `GET /catalog/exercises/{exerciseId}`

운동 가이드에 필요한 기본 수행 정보·근육·음성 큐를 한 번에 반환한다.

```json
{
  "exerciseId": 1,
  "name": "예시 운동",
  "defaultSetCount": 3,
  "defaultRepCount": null,
  "defaultDurationSeconds": 40,
  "metValue": 3.5,
  "difficulty": "beginner",
  "cautionNote": "허리에 불편함이 있으면 범위를 줄입니다.",
  "muscles": [],
  "voiceCues": [
    {
      "displayOrder": 1,
      "startOffsetSeconds": null,
      "endOffsetSeconds": null,
      "content": "호흡을 천천히 유지합니다."
    }
  ]
}
```

주의할 점:

- `defaultSetCount`, `defaultRepCount`, `defaultDurationSeconds`, `metValue`,
  `difficulty`, `cautionNote`는 null일 수 있다. null을 0이나 빈 문자열로 바꾸지 않는다.
- `metValue`는 kcal이 아니다. 현재 회원 몸무게를 함께 조회해 kcal을 계산하는 API도 없다.
- 음성 큐는 `displayOrder` 순서다. `startOffsetSeconds`가 null이면 타임코드가 확정되지
  않은 상태이므로 순차 재생으로 처리한다. `endOffsetSeconds`가 null인 큐는 유지 구간이
  없는 문장이다.
- 현재 운동 응답에는 동영상 재생 URL·썸네일 URL이 없다. YMove 연동은 후속 범위다.
- 현재 운동 목록 API(`GET /catalog/exercises`)는 없다. 화면에 필요한 운동 ID를 가진
  코스 API가 아직 구현되지 않았기 때문이다.

## 이미지 자산 처리

`imageAssetKey`와 `highlightAssetKey`는 URL이 아니다. 서버가 반환하는 키를 프론트의
정적 파일 매핑에 사용한다.

예를 들면 다음처럼 API 클라이언트와 자산 resolver를 분리하는 것을 권장한다.

```ts
const targetPoseImageMap: Record<string, string> = {
  "target-pose/example": "/assets/target-poses/example.webp",
};

function resolveTargetPoseImage(key: string | null): string | null {
  return key === null ? null : targetPoseImageMap[key] ?? null;
}
```

키가 null이거나 프론트 매핑에 없을 때 깨진 URL을 조합하지 말고 placeholder를 사용한다.
서버가 파일 URL을 내려준다고 가정해 `new URL(imageAssetKey)`처럼 처리하면 안 된다.

## 공통 실패 응답

도메인 오류와 인증 필터 오류 모두 다음 모양이다.

```json
{
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다"
}
```

현재 프론트에서 우선 처리할 코드는 다음과 같다.

| HTTP | `code` | 대응 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | JSON 형식·필수 필드 등 요청 자체를 수정 |
| 400 | `INVALID_NICKNAME` | 닉네임을 1~50자로 다시 입력 |
| 400 | `EMPTY_SCREENING_ANSWER` | 자세를 하나도 고르지 않음. 제출 버튼을 먼저 막는다 |
| 400 | `INVALID_HEIGHT` | 키가 100~250cm 밖. 입력 UI에서 먼저 막는다 |
| 400 | `INVALID_WEIGHT` | 몸무게가 20~300kg 밖. 입력 UI에서 먼저 막는다 |
| 400 | `INVALID_REINFORCEMENT_SETTING` | 강화 부위·난이도를 한쪽만 보냈거나 난이도가 1~3 밖 |
| 400 | `TOO_MANY_SCREENING_ANSWERS` | 한 체감에 4개를 넘김. 선택 UI에서 먼저 막는다 |
| 400 | `DUPLICATE_SCREENING_ANSWER` | 같은 자세를 두 번 넣음. `EASY`·`HARD`에 나눠 넣은 경우도 포함 |
| 401 | `UNAUTHORIZED` | 토큰이 없거나 만료됨. `Kakao.Auth.authorize()`부터 다시 수행 |
| 401 | `KAKAO_AUTH_CODE_INVALID` | 인가 코드가 무효·만료·재사용. **같은 코드로 재시도하지 말고** `authorize()`부터 다시 태운다 |
| 401 | `KAKAO_TOKEN_INVALID` | 서버가 교환한 카카오 액세스 토큰이 사용자 조회에서 거부됨. 서버·카카오 쪽 문제이므로 재로그인으로 풀리지 않으면 서버팀에 알린다 |
| 403 | `FORBIDDEN` | 현재 권한으로 접근할 수 없음 |
| 404 | `MEMBER_NOT_FOUND` | 현재 토큰의 회원이 없음 |
| 404 | `SCREENING_RESULT_NOT_FOUND` | 아직 진단한 적이 없음. **온보딩으로 보낸다** |
| 404 | `TARGET_POSE_NOT_FOUND` | 존재하지 않는 목표 자세 ID |
| 404 | `EXERCISE_NOT_FOUND` | 존재하지 않는 운동 ID |
| 422 | `CAUSE_NOT_DETERMINED` | 고른 조합이 분기 규칙에 걸리지 않음. 재입력이 아니라 "결과를 낼 수 없다" 안내 |
| 502 | `KAKAO_UNAVAILABLE` | 카카오 인증 서버 장애·네트워크 오류. 재시도 안내 |
| 500 | `INTERNAL_ERROR` | 사용자에게 내부 오류를 노출하지 말고 재시도 안내 |

HTTP 상태만 보지 말고 가능하면 `code`를 기준으로 화면 동작을 결정한다. 단, 서버에서
새로운 오류 코드가 추가될 수 있으므로 알 수 없는 코드는 일반 오류로 처리한다.

## 프론트 구현 시 지켜야 할 것

1. API 응답의 `null`을 정상 상태로 모델링한다. 특히 프로필·이미지·운동 기본값은
   미입력일 수 있다.
2. ID를 화면 코드에 하드코딩하지 않는다. catalog seed가 바뀌면 ID가 달라질 수 있으므로
   목록 응답에서 받은 ID를 상세 조회에 사용한다.
3. `bodyPartCode`, `muscleCode`, `imageAssetKey`를 표시용 한글과 혼용하지 않는다.
   서버 값은 식별·매핑용이고, 별도 화면 라벨이 필요하면 프론트 표시 테이블을 둔다.
4. 현재 서버가 주지 않는 영상 URL·운동 썸네일·코스를 catalog 응답에서 추측해 만들지 않는다.
   필요한 계약은 해당 도메인 API가 추가될 때 정한다.
5. 인증 토큰은 API 클라이언트 한 곳에서 `Authorization` 헤더로 붙인다. 로그인 API에는
   기존 Aligner 토큰을 붙일 필요가 없다.
6. 인가 코드를 두 번 보내지 않는다. 착지 라우트가 리렌더·StrictMode 등으로 두 번 실행되면
   두 번째 요청이 `KAKAO_AUTH_CODE_INVALID`로 실패한다. 코드는 1회용이다.
7. 401 처리에서 무한 재로그인 루프를 만들지 않는다. 로그인 실패가 반복되면 세션을
   비우고 로그인 화면으로 보낸다.
8. `fetch`에 `credentials: 'include'`를 쓰지 않는다. 아래 「CORS」 참고 — 서버가 자격 증명
   모드를 열지 않으므로 그 옵션을 켜면 요청이 통째로 막힌다.

## CORS

프론트와 서버는 오리진이 다르다(`http://localhost:5173` ↔ `http://localhost:8080`). 서버가 허용
목록에 있는 오리진에만 교차 출처 요청을 열어준다.

| 항목 | 값 |
| --- | --- |
| 허용 오리진 | 서버 설정값. 로컬 기본값은 `http://localhost:5173` |
| 허용 메서드 | `GET` `POST` `PATCH` `DELETE` |
| 허용 요청 헤더 | `Authorization` `Content-Type` |
| 자격 증명(`credentials`) | **열지 않음** |

지켜야 할 것이 셋이다.

1. **`credentials: 'include'`를 쓰지 않는다.** 인증은 `Authorization: Bearer` 헤더 하나뿐이고
   서버가 쿠키를 내리지 않는다. 이 옵션을 켜면 서버가 `Access-Control-Allow-Credentials`를
   주지 않으므로 브라우저가 응답을 통째로 버린다 — 401도 아니고 네트워크 오류로 보인다
2. **오리진은 스킴·호스트·포트가 완전히 일치해야 한다.** `http://localhost:5173`과
   `http://127.0.0.1:5173`은 브라우저에게 다른 오리진이다. 개발 서버 포트를 바꾸면
   서버의 `CORS_ALLOWED_ORIGINS`도 같이 바꿔야 한다
3. **배포 오리진은 배포 전에 서버팀에 알린다.** 와일드카드를 쓰지 않으므로 도메인이 정해지면
   서버 설정에 넣어야 한다. 넣기 전까지는 배포 환경에서 모든 요청이 막힌다

실패 응답(401·404 등)에도 CORS 헤더가 붙으므로 오류 본문의 `code`를 정상적으로 읽을 수 있다.

## 서버 코드를 찾는 위치

프론트 요구사항이 현재 계약에 없는 경우, 아래 위치를 기준으로 변경 범위를 잡는다.

| 프론트 요구 | 현재 코드 위치 | 현재 상태 |
| --- | --- | --- |
| 카카오 로그인·JWT·공통 오류 | `support-web/src/main/kotlin/team/aligner/support/web` | 구현됨 |
| 프로필·온보딩 입력·탈퇴 API | `member/api`, `member/service`, `member/model` | 구현됨 |
| 목표 자세·운동 API 응답 DTO | `catalog/api/src/main/kotlin/team/aligner/catalog/api/dto` | 구현됨 |
| 목표 자세·운동 조회 SQL | `catalog/repository-jdbc` | 구현됨 |
| catalog 콘텐츠 추가·수정 | `catalog/schema`의 Liquibase seed | 현재 seed 미구현 |
| 부위·스크리닝·원인 API 응답 DTO | `screening/api/src/main/kotlin/team/aligner/screening/api/dto` | 구현됨 |
| 원인 판별 규칙 | `screening/model`의 `ScreeningResult.determineCauses` | 구현됨 |
| 부위·원인·분기 규칙 데이터 | `screening/schema`의 Liquibase seed | 현재 seed 미구현 |
| CORS 허용 오리진 | `support-web`의 `SecurityConfig`·`CorsProperties` | 구현됨 |
| 코스·진행도·세션 | `course`, `training` 도메인 | 도메인 미구현 |

예를 들어 프론트가 “코스 상세에서 운동 목록을 받고 싶다”고 요청하면,
`GET /catalog/exercises`를 임시로 만들어 쓰는 것이 아니라 `course`의 코스 계약과
응답에 어떤 운동 정보가 포함될지 먼저 정해야 한다. 프론트 화면에 필요한 데이터가
현재 API에 없다는 사실 자체가 서버에 전달해야 할 요구사항이다.

## 로컬 서버를 사용할 때

실행 모듈은 `application-api` 하나다. 기본 포트는 `8080`이며, PostgreSQL이 필요하다.
최소 환경변수는 다음과 같다.

```bash
export DB_PASSWORD=<db_password>
export JWT_SECRET=<32바이트 이상>
export KAKAO_CLIENT_ID=<kakao_rest_api_key>
export KAKAO_CLIENT_SECRET=<kakao_client_secret>
./gradlew :application-api:bootRun
```

네 개 모두 기본값이 없어 빠지면 기동에 실패한다. 실제 값은 저장소에 두지 않는다.

`DB_URL`의 기본값은 `jdbc:postgresql://localhost:5432/aligner`, `DB_USERNAME`의
기본값은 `aligner`다. 포트를 바꾸려면 `SERVER_PORT`를 사용한다.

`KAKAO_REDIRECT_URI`의 기본값은 `http://localhost:5173/oauth/kakao`다. 프론트 개발 서버가
다른 포트를 쓰면 이 값을 바꾸고, **같은 값을 카카오 개발자 콘솔에도 등록해야 한다.**
`authorize()`에 넘기는 값과 한 글자라도 다르면 토큰 교환이 거부된다.

`CORS_ALLOWED_ORIGINS`의 기본값은 `http://localhost:5173`이다. **개발 서버 포트를 바꾸면
`KAKAO_REDIRECT_URI`와 이 값을 함께 바꿔야 한다** — 앞의 것은 인가 코드가 착지하는 곳이고
뒤의 것은 API를 부르는 곳인데, 프론트에서는 같은 오리진이다. 하나만 바꾸면 로그인은 되는데
API가 전부 막히거나 그 반대가 된다.

여러 오리진은 쉼표로 준다. `*`는 기동 시점에 거부한다.

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:5173,https://aligner.example.com
```

Swagger/OpenAPI는 기본적으로 꺼져 있다. 확인이 필요하면 서버 실행 시
`SPRINGDOC_ENABLED=true`를 별도로 설정한다. 이것이 켜져 있지 않으면 Swagger가 안 보이는
것은 API 미구현의 증거가 아니다.

## 백엔드에 기능을 추가할 때 읽을 문서

- 코드가 어느 도메인·계층에 들어가는지: [`architecture.md`](./architecture.md)
- 도메인 경계와 아직 미확정인 범위: [`domains.md`](./domains.md)
- 브랜치·커밋·PR 규칙: [`../CONTRIBUTING.md`](../CONTRIBUTING.md)

프론트가 새 화면을 만들기 전에 먼저 “현재 API로 가능한 화면인지”를 이 문서의 구현 상태
표에서 확인한다. 없는 API를 프론트에서 임시로 가정하면 이후 서버 계약과 충돌하기 쉽다.
