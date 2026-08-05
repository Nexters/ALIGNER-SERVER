package team.aligner.screening.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import team.aligner.screening.model.CauseRule
import team.aligner.screening.model.PerceivedDifficulty
import team.aligner.screening.model.ScreeningAnswer
import team.aligner.screening.model.ScreeningResult
import team.aligner.screening.model.exception.CauseNotDeterminedException
import team.aligner.screening.model.exception.DuplicateScreeningAnswerException
import team.aligner.screening.model.exception.EmptyScreeningAnswerException
import team.aligner.screening.model.exception.TooManyScreeningAnswersException

/**
 * 애그리거트의 검증과 판별을 고정한다.
 *
 * 판별을 SQL 이 아니라 도메인에 둔 이유가 이 테스트다 — `GROUP BY` 로 짜면 순위 규칙이 쿼리에
 * 숨어 컨테이너 없이는 확인할 수 없다.
 */
class ScreeningResultTest :
    DescribeSpec({
        fun answer(
            poseId: Long,
            difficulty: PerceivedDifficulty,
        ) = ScreeningAnswer(targetPoseId = poseId, perceivedDifficulty = difficulty)

        fun rule(
            poseId: Long,
            difficulty: PerceivedDifficulty,
            causeCode: String,
            weight: Int,
        ) = CauseRule(
            targetPoseId = poseId,
            perceivedDifficulty = difficulty,
            causeCode = causeCode,
            weight = weight,
        )

        fun submit(answers: List<ScreeningAnswer>) =
            ScreeningResult.submit(memberId = 1L, perceivedBodyPartCode = "NECK_SHOULDER", answers = answers)

        describe("submit") {
            it("응답이 비어 있으면 막는다") {
                shouldThrow<EmptyScreeningAnswerException> { submit(emptyList()) }
            }

            it("같은 자세를 두 번 고르면 막는다") {
                shouldThrow<DuplicateScreeningAnswerException> {
                    submit(listOf(answer(1L, PerceivedDifficulty.EASY), answer(1L, PerceivedDifficulty.EASY)))
                }
            }

            it("같은 자세를 EASY 와 HARD 로 같이 내면 막는다") {
                // 중복 검사 하나가 모순 제출까지 같이 잡는다. DB 의 UNIQUE 도 같은 형태다.
                shouldThrow<DuplicateScreeningAnswerException> {
                    submit(listOf(answer(1L, PerceivedDifficulty.EASY), answer(1L, PerceivedDifficulty.HARD)))
                }
            }

            it("한 체감에 4 개를 넘기면 막는다") {
                shouldThrow<TooManyScreeningAnswersException> {
                    submit((1L..5L).map { answer(it, PerceivedDifficulty.HARD) })
                }
            }

            it("체감별로 4 개씩 총 8 개는 통과한다") {
                val answers =
                    (1L..4L).map { answer(it, PerceivedDifficulty.EASY) } +
                        (5L..8L).map { answer(it, PerceivedDifficulty.HARD) }

                submit(answers).answers.size shouldBe 8
            }
        }

        describe("determineCauses") {
            it("매칭된 규칙의 weight 를 원인별로 합산해 순위를 매긴다") {
                val result =
                    submit(
                        listOf(
                            answer(1L, PerceivedDifficulty.HARD),
                            answer(2L, PerceivedDifficulty.HARD),
                        ),
                    ).determineCauses(
                        listOf(
                            rule(1L, PerceivedDifficulty.HARD, "THORACIC", 3),
                            rule(2L, PerceivedDifficulty.HARD, "THORACIC", 2),
                            rule(1L, PerceivedDifficulty.HARD, "SHOULDER", 4),
                        ),
                    )

                result.causes.map { it.causeCode } shouldBe listOf("THORACIC", "SHOULDER")
                result.causes.map { it.score } shouldBe listOf(5, 4)
                result.causes.map { it.rank } shouldBe listOf(1, 2)
            }

            it("체감이 다르면 같은 자세라도 규칙이 매칭되지 않는다") {
                val result =
                    submit(listOf(answer(1L, PerceivedDifficulty.EASY)))
                        .determineCauses(
                            listOf(
                                // HARD 규칙이라 EASY 응답에는 걸리지 않는다.
                                rule(1L, PerceivedDifficulty.HARD, "THORACIC", 9),
                                rule(1L, PerceivedDifficulty.EASY, "CORE", 1),
                            ),
                        )

                result.causes.map { it.causeCode } shouldBe listOf("CORE")
            }

            it("고르지 않은 자세의 규칙은 버린다") {
                val result =
                    submit(listOf(answer(1L, PerceivedDifficulty.HARD)))
                        .determineCauses(
                            listOf(
                                rule(1L, PerceivedDifficulty.HARD, "THORACIC", 2),
                                rule(99L, PerceivedDifficulty.HARD, "HIP", 9),
                            ),
                        )

                result.causes.map { it.causeCode } shouldBe listOf("THORACIC")
            }

            it("동점이면 원인 코드 오름차순으로 끊는다") {
                // 정하지 않으면 같은 응답에 같은 순위가 매번 다르게 나온다.
                val result =
                    submit(
                        listOf(
                            answer(1L, PerceivedDifficulty.HARD),
                            answer(2L, PerceivedDifficulty.HARD),
                        ),
                    ).determineCauses(
                        listOf(
                            rule(1L, PerceivedDifficulty.HARD, "ZETA", 5),
                            rule(2L, PerceivedDifficulty.HARD, "ALPHA", 5),
                        ),
                    )

                result.causes.map { it.causeCode } shouldBe listOf("ALPHA", "ZETA")
                result.causes.map { it.rank } shouldBe listOf(1, 2)
            }

            it("걸리는 규칙이 하나도 없으면 빈 결과를 저장하지 않고 막는다") {
                // 원인 0 개인 진단이 남으면 course 가 처방할 것을 못 찾는다.
                shouldThrow<CauseNotDeterminedException> {
                    submit(listOf(answer(1L, PerceivedDifficulty.HARD))).determineCauses(emptyList())
                }
            }
        }
    })
