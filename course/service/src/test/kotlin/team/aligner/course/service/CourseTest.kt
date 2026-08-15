package team.aligner.course.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import team.aligner.course.model.Course
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.CourseStepStatus
import team.aligner.course.model.CourseTemplate
import team.aligner.course.model.CourseTemplateStep
import team.aligner.course.model.CourseTemplateStepExercise
import team.aligner.course.model.exception.CourseStepNotFoundException
import team.aligner.course.model.exception.EmptyCourseTemplateException
import java.time.Instant

/**
 * 애그리거트의 추천·진행도·멱등성을 고정한다.
 *
 * 진행도를 SQL 집계가 아니라 도메인에 둔 이유가 이 테스트다 — 컨테이너 없이 확인할 수 있다.
 */
class CourseTest :
    DescribeSpec({
        val at = Instant.parse("2026-08-09T00:00:00Z")

        fun template(stepCount: Int) =
            CourseTemplate(
                templateId = 1L,
                targetPoseId = 3L,
                name = "낙타자세 정복하기",
                recommendationReason = "등과 골반 근육 강화에 집중해 보세요",
                steps =
                    (1..stepCount).map { order ->
                        CourseTemplateStep(
                            stepOrder = order,
                            exercises =
                                listOf(
                                    CourseTemplateStepExercise(
                                        exerciseId = order.toLong(),
                                        displayOrder = 1,
                                        durationSeconds = 120,
                                        setCount = 1,
                                    ),
                                ),
                        )
                    },
            )

        fun recommend(stepCount: Int = 4) = Course.recommend(memberId = 1L, template = template(stepCount), causeCode = "WEAK_BACK")

        describe("recommend") {
            it("템플릿 스텝을 그대로 복사하고 진행 중으로 시작한다") {
                val course = recommend()

                course.status shouldBe CourseStatus.IN_PROGRESS
                course.totalStepCount shouldBe 4
                course.completedStepCount shouldBe 0
                course.currentStepOrder shouldBe 1
                course.causeCode shouldBe "WEAK_BACK"
                course.steps.map { it.stepOrder } shouldBe listOf(1, 2, 3, 4)
            }

            /**
             * 스텝이 없으면 진행도의 분모가 0 이 되어 "다 했는데 완성이 아닌" 코스가 남는다.
             */
            it("스텝이 없는 템플릿은 추천하지 않는다") {
                shouldThrow<EmptyCourseTemplateException> { recommend(stepCount = 0) }
            }
        }

        describe("completeStep") {
            it("스텝을 완료하면 진행도가 오르고 다음 스텝을 가리킨다") {
                val course = recommend().completeStep(stepOrder = 1, at = at)

                course.completedStepCount shouldBe 1
                course.currentStepOrder shouldBe 2
                course.status shouldBe CourseStatus.IN_PROGRESS
                course.completedAt.shouldBeNull()
            }

            it("순서와 무관하게 완료할 수 있다") {
                val course =
                    recommend()
                        .completeStep(stepOrder = 3, at = at)
                        .completeStep(stepOrder = 1, at = at)

                course.completedStepCount shouldBe 2
                // 남은 것 중 가장 앞선 스텝이 다음이다.
                course.currentStepOrder shouldBe 2
            }

            /**
             * training 이 세션 완료를 push 하는데 그 요청이 재시도될 수 있다. 진행도가 두 번
             * 오르면 안 된다 (docs/domains.md §7-8).
             */
            it("같은 스텝을 두 번 완료해도 진행도가 두 번 오르지 않는다") {
                val once = recommend().completeStep(stepOrder = 1, at = at)
                val twice = once.completeStep(stepOrder = 1, at = at.plusSeconds(60))

                twice.completedStepCount shouldBe 1
                // 완료 시각도 처음 것이 유지된다.
                twice.steps.first { it.stepOrder == 1 }.completedAt shouldBe at
            }

            it("마지막 스텝을 완료하면 코스가 완성된다") {
                var course = recommend(stepCount = 2)
                course = course.completeStep(stepOrder = 1, at = at)
                course.status shouldBe CourseStatus.IN_PROGRESS

                course = course.completeStep(stepOrder = 2, at = at)

                course.status shouldBe CourseStatus.COMPLETED
                course.completedStepCount shouldBe 2
                course.currentStepOrder.shouldBeNull()
                course.completedAt.shouldNotBeNull() shouldBe at
            }

            it("완성된 코스에 같은 스텝이 다시 들어와도 완료 시각이 밀리지 않는다") {
                val completed =
                    recommend(stepCount = 1)
                        .completeStep(stepOrder = 1, at = at)
                val again = completed.completeStep(stepOrder = 1, at = at.plusSeconds(600))

                again.completedAt shouldBe at
            }

            it("없는 스텝이면 막는다") {
                shouldThrow<CourseStepNotFoundException> {
                    recommend().completeStep(stepOrder = 99, at = at)
                }
            }

            it("완료한 스텝의 상태가 실제로 바뀐다") {
                val course = recommend().completeStep(stepOrder = 2, at = at)

                course.steps.first { it.stepOrder == 2 }.status shouldBe CourseStepStatus.COMPLETED
                course.steps.first { it.stepOrder == 1 }.status shouldBe CourseStepStatus.NOT_STARTED
            }
        }
    })
