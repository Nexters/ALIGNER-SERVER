package team.aligner.screening.model

import team.aligner.screening.model.exception.CauseNotDeterminedException
import team.aligner.screening.model.exception.DuplicateScreeningAnswerException
import team.aligner.screening.model.exception.EmptyScreeningAnswerException
import team.aligner.screening.model.exception.TooManyScreeningAnswersException
import java.time.Instant

/**
 * 진단 1 회. 애그리거트 루트다.
 *
 * `perceivedBodyPartCode` 는 회원이 고른 **느끼는 부위**이고, `causes` 는 판별된 **원인 부위**다.
 * 둘이 다르다는 것이 이 도메인의 요점이다 (`AGENTS.md` §1).
 *
 * Spring Data JDBC 에는 더티체킹이 없다. [determineCauses] 는 새 인스턴스를 반환하고 호출부가
 * save 를 명시한다 (docs/architecture.md §4).
 */
data class ScreeningResult(
    val identity: ScreeningResultIdentity?,
    val memberId: Long,
    val perceivedBodyPartCode: String,
    val answers: List<ScreeningAnswer>,
    val causes: List<ScreeningCause>,
    val createdAt: Instant?,
) {
    /**
     * 응답을 분기 규칙에 맞춰 원인 순위로 바꾼다.
     *
     * 규칙을 port 로 읽어 여기서 집계한다. SQL 로 `GROUP BY` 하면 순위 규칙이 쿼리에 숨어
     * 단위 테스트로 고정할 수 없다. 자세가 최대 8 개라 규칙 행도 그 정도이고 성능 문제가 없다.
     *
     * 매칭되지 않는 규칙은 그냥 버린다. seed 에는 회원이 고르지 않은 자세의 규칙도 들어 있다.
     */
    fun determineCauses(rules: List<CauseRule>): ScreeningResult {
        val answered = answers.map { it.targetPoseId to it.perceivedDifficulty }.toSet()

        val scoreByCause =
            rules
                .filter { (it.targetPoseId to it.perceivedDifficulty) in answered }
                .groupingBy { it.causeCode }
                .fold(0) { sum, rule -> sum + rule.weight }

        if (scoreByCause.isEmpty()) {
            // 빈 결과를 저장하면 "원인 0 개인 진단" 이 남아 course 가 처방할 것을 못 찾는다.
            throw CauseNotDeterminedException()
        }

        val determined =
            scoreByCause
                .entries
                // 점수 내림차순. 동점은 causeCode 오름차순으로 끊는다 — 정하지 않으면 같은
                // 응답에 같은 순위가 매번 다르게 나온다.
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .mapIndexed { index, (causeCode, score) ->
                    ScreeningCause(causeCode = causeCode, rank = index + 1, score = score)
                }

        return copy(causes = determined)
    }

    companion object {
        /** 쉬웠던 자세와 어려웠던 자세를 **각각** 최대 4 개 고른다 (docs/domains.md §4-2). */
        const val MAX_ANSWERS_PER_DIFFICULTY = 4

        /**
         * 회원의 선택을 받는다. 원인은 아직 비어 있고 [determineCauses] 가 채운다.
         *
         * **개수와 중복을 여기서 막는다.** DB 에도 `UNIQUE (result_id, target_pose_id)` 가 있지만
         * 거기까지 가면 제약 위반이 500 으로 나간다. 회원 입력 문제는 400 이어야 한다.
         *
         * 개수 상한을 DB 제약으로 만들지 않은 이유는 행 개수를 세는 조건이라 `CHECK` 으로 쓸 수
         * 없고 트리거는 과해서다. 상한값 자체도 감수 데이터가 아니라 온보딩 화면 규칙이라,
         * 바뀔 때 changeset 이 아니라 이 상수만 고치는 편이 맞다.
         *
         * **받은 목록을 그대로 들고 있지 않고 복사한다.** Kotlin 의 `List` 는 읽기 전용일 뿐
         * 불변이 아니라서, 호출부가 `MutableList` 를 넘기고 검증 뒤에 원소를 더하면 위 세 검사가
         * 통째로 무의미해진다. 애그리거트가 자기 불변식을 스스로 지켜야 한다.
         */
        fun submit(
            memberId: Long,
            perceivedBodyPartCode: String,
            answers: List<ScreeningAnswer>,
        ): ScreeningResult {
            // 검사 도중에도 바뀔 수 있으므로 먼저 스냅샷을 뜨고, 그 스냅샷만 본다.
            val submitted = answers.toList()

            if (submitted.isEmpty()) {
                throw EmptyScreeningAnswerException()
            }
            if (submitted.distinctBy { it.targetPoseId }.size != submitted.size) {
                // 같은 자세를 EASY 와 HARD 로 같이 낸 모순도 여기서 함께 걸린다.
                throw DuplicateScreeningAnswerException()
            }
            if (submitted.groupingBy { it.perceivedDifficulty }.eachCount().any { it.value > MAX_ANSWERS_PER_DIFFICULTY }) {
                throw TooManyScreeningAnswersException()
            }

            return ScreeningResult(
                identity = null,
                memberId = memberId,
                perceivedBodyPartCode = perceivedBodyPartCode,
                answers = submitted,
                causes = emptyList(),
                createdAt = null,
            )
        }
    }
}

/**
 * 회원이 고른 자세 하나와 그 체감.
 *
 * `targetPoseId` 는 `catalog` 의 값이지만 참조하지 않는다. 자세 그리드는 클라이언트가 catalog
 * API 로 직접 그리고, screening 은 식별자만 값으로 받아 저장한다 (docs/domains.md §4-2).
 */
data class ScreeningAnswer(
    val targetPoseId: Long,
    val perceivedDifficulty: PerceivedDifficulty,
)

/**
 * 판별된 원인 하나.
 *
 * `score` 를 저장해 두는 것은 seed 의 `weight` 가 나중에 바뀌어도 그때 내린 진단이
 * 재계산되지 않게 하기 위해서다.
 */
data class ScreeningCause(
    val causeCode: String,
    val rank: Int,
    val score: Int,
)
