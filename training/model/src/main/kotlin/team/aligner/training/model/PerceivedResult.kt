package team.aligner.training.model

/**
 * 핀포즈를 수행한 직후의 체감. 화면의 "오늘 파이어로그, 어땠어요?" 3 지선다다.
 *
 * 감수 데이터가 아니라 화면 선택지와 1:1 인 닫힌 어휘이므로 코드에 둔다. DDL 의
 * `ck_session_perceived_result` 가 같은 값 집합을 강제한다 — 값 집합이 열려 있는
 * `difficulty`·`category` 를 문자열로 둔 것과 반대 판단이다.
 *
 * **`screening` 의 `PerceivedDifficulty` 와 다른 어휘다.** 그쪽은 온보딩에서 해본 적 있는
 * 자세를 고르는 EASY·HARD 이고, 이쪽은 방금 수행한 결과라 "안될 거 같아요" 라는 세 번째
 * 값이 있다. 같은 이름을 쓰면 두 화면이 같은 것을 묻는다고 읽힌다.
 */
enum class PerceivedResult {
    /** 잘됐어요. */
    SUCCEEDED,

    /** 아직 어려워요. 계속 도전한다. */
    STILL_HARD,

    /**
     * 안될 거 같아요.
     *
     * 화면은 이때 "다음 자세로 바꿔드려요" 를 안내한다. **서버가 자동으로 바꾸지 않는다** —
     * 어떤 자세로 옮길지가 기획 미확정이라, 지금은 화면이 부위·난이도 재선택으로 보내고
     * 기존 `POST /courses` 로 새 코스를 받는다.
     */
    TOO_HARD,
}
