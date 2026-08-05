package team.aligner.screening.model

/**
 * 회원이 자세를 해보고 느낀 난이도.
 *
 * `screening.screening_answer` 와 `screening.cause_rule` 의 같은 이름 컬럼이 이 값 집합을
 * `CHECK` 으로 강제한다. 분기표 좌변과 응답이 같은 집합이어야 조인이 성립한다.
 */
enum class PerceivedDifficulty {
    /** 쉬웠던 자세. */
    EASY,

    /** 어려웠던 자세. */
    HARD,
}
