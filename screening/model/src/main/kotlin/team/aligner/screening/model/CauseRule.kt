package team.aligner.screening.model

/**
 * (자세, 체감) → 원인 분기표의 한 행. 전량 seed 이고 감수 대상이다.
 *
 * 판별은 이 규칙들을 port 로 읽어 도메인에서 집계한다. SQL 로 `GROUP BY` 하지 않는 이유는
 * 순위 매기기가 도메인 규칙이라 단위 테스트로 고정돼야 해서다.
 *
 * `weight` 가 코드가 아니라 seed 에 있으므로, 감수 결과가 바뀌어도 changeset 만 새로 쌓으면 된다
 * (docs/domains.md §4-2).
 */
data class CauseRule(
    val targetPoseId: Long,
    val perceivedDifficulty: PerceivedDifficulty,
    val causeCode: String,
    val weight: Int,
)
