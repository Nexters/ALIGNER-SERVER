package team.aligner.course.infrastructure

/**
 * 회원 몸무게를 읽는 out-port. `course/adapter-member` 가 구현한다.
 *
 * **`course → member` 는 초판 의존 지도에 없던 방향**이고 뒤늦게 허용됐다. 홈 카드가 코스
 * 칼로리를 보여주는데 `kcal = MET × 3.5 × 체중 ÷ 200 × 분` 이라 몸무게 없이는 계산이
 * 성립하지 않는다 (docs/domains.md §3, §4-3). `member` 는 아무 도메인도 의존하지 않으므로
 * 순환은 생기지 않는다.
 */
interface MemberBodyPort {
    /** 없거나 탈퇴한 회원, 또는 몸무게 미입력이면 null 이다. */
    fun findWeightKg(memberId: Long): Int?
}
