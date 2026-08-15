package team.aligner.training.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import team.aligner.training.model.ExerciseResult
import team.aligner.training.model.Session
import team.aligner.training.model.SessionStatus
import team.aligner.training.model.StepExercise
import team.aligner.training.model.exception.DuplicateExerciseRecordException
import team.aligner.training.model.exception.EmptyCourseStepException
import team.aligner.training.model.exception.UnknownExerciseRecordException
import java.time.Instant

/**
 * 세션 애그리거트의 시작·완료·멱등성을 고정한다.
 *
 * **완수 판정이 여기 없다는 것도 함께 고정한다** — 진행도·도장은 course 가 판단한다
 * (docs/domains.md §2).
 */
class SessionTest :
    DescribeSpec({
        val at = Instant.parse("2026-08-10T00:00:00Z")

        fun start(count: Int = 2) =
            Session.start(
                memberId = 1L,
                courseId = 20L,
                stepOrder = 1,
                exercises =
                    (1..count).map {
                        StepExercise(courseStepExerciseId = it.toLong(), exerciseId = it.toLong() + 100, displayOrder = it)
                    },
            )

        describe("start") {
            it("스텝 구성을 복사하고 전부 미수행으로 연다") {
                val session = start()

                session.status shouldBe SessionStatus.IN_PROGRESS
                session.records.size shouldBe 2
                session.records.all { !it.completed } shouldBe true
                session.records.all { it.performedDurationSeconds == null } shouldBe true
                session.completedAt.shouldBeNull()
            }

            it("운동이 없는 스텝으로는 세션을 열지 않는다") {
                shouldThrow<EmptyCourseStepException> { start(count = 0) }
            }
        }

        describe("complete") {
            it("수행 결과를 채우고 세션을 닫는다") {
                val session =
                    start().complete(
                        results = listOf(ExerciseResult(1L, completed = true, performedDurationSeconds = 120)),
                        at = at,
                    )

                session.status shouldBe SessionStatus.COMPLETED
                session.completedAt shouldBe at
                session.records.first { it.courseStepExerciseId == 1L }.completed shouldBe true
                session.records.first { it.courseStepExerciseId == 1L }.performedDurationSeconds shouldBe 120
            }

            /**
             * 부분 완료가 정상이다. 회원이 중간에 그만둔 세션도 기록으로 남아야 한다.
             */
            it("요청에 없는 운동은 수행하지 않은 것으로 남는다") {
                val session =
                    start().complete(
                        results = listOf(ExerciseResult(1L, completed = true, performedDurationSeconds = 120)),
                        at = at,
                    )

                session.records.first { it.courseStepExerciseId == 2L }.completed shouldBe false
                session.records
                    .first { it.courseStepExerciseId == 2L }
                    .performedDurationSeconds
                    .shouldBeNull()
            }

            /**
             * 조용히 무시하면 클라이언트가 잘못 보낸 것을 성공으로 읽는다.
             */
            it("이 세션에 없는 운동이 섞이면 막는다") {
                shouldThrow<UnknownExerciseRecordException> {
                    start().complete(
                        results = listOf(ExerciseResult(99L, completed = true, performedDurationSeconds = 10)),
                        at = at,
                    )
                }
            }

            /**
             * **여기가 조용히 사라지는 자리였다.** `associateBy` 는 중복 키에서 마지막 것만
             * 남기므로, 서로 다른 값이 두 번 오면 앞선 값이 말없이 버려지고 요청은 200 이었다
             * (이슈 #32).
             */
            it("같은 운동의 수행 결과가 서로 다른 값으로 두 번 오면 막는다") {
                shouldThrow<DuplicateExerciseRecordException> {
                    start().complete(
                        results =
                            listOf(
                                ExerciseResult(1L, completed = true, performedDurationSeconds = 120),
                                ExerciseResult(1L, completed = false, performedDurationSeconds = 0),
                            ),
                        at = at,
                    )
                }
            }

            /**
             * 값이 같아도 막는다. 같은지 다른지로 갈라 봤자 클라이언트가 잘못 보낸 것은
             * 마찬가지고, 분기를 두면 "같으면 통과" 를 규칙으로 읽는 쪽이 생긴다.
             */
            it("같은 값으로 두 번 와도 막는다") {
                shouldThrow<DuplicateExerciseRecordException> {
                    start().complete(
                        results =
                            listOf(
                                ExerciseResult(1L, completed = true, performedDurationSeconds = 120),
                                ExerciseResult(1L, completed = true, performedDurationSeconds = 120),
                            ),
                        at = at,
                    )
                }
            }

            /**
             * 중복 검사가 **이 세션에 없는 운동 검사보다 뒤**다. 없는 식별자가 중복으로 와도
             * 먼저 걸리는 것은 UNKNOWN 이다 — 어느 쪽이든 400 이라 화면은 같게 다룬다.
             */
            it("없는 운동이 중복으로 오면 UNKNOWN 이 먼저다") {
                shouldThrow<UnknownExerciseRecordException> {
                    start().complete(
                        results =
                            listOf(
                                ExerciseResult(99L, completed = true, performedDurationSeconds = 10),
                                ExerciseResult(99L, completed = true, performedDurationSeconds = 10),
                            ),
                        at = at,
                    )
                }
            }

            /**
             * 완료 요청은 재시도될 수 있다. 덮어쓰면 처음 저장한 수행 결과가 사라진다.
             */
            it("이미 완료된 세션은 기록을 덮어쓰지 않는다") {
                val once =
                    start().complete(
                        results = listOf(ExerciseResult(1L, completed = true, performedDurationSeconds = 120)),
                        at = at,
                    )
                val twice =
                    once.complete(
                        results = listOf(ExerciseResult(1L, completed = false, performedDurationSeconds = 0)),
                        at = at.plusSeconds(600),
                    )

                twice.completedAt shouldBe at
                twice.records.first { it.courseStepExerciseId == 1L }.completed shouldBe true
                twice.records.first { it.courseStepExerciseId == 1L }.performedDurationSeconds shouldBe 120
            }
        }
    })
