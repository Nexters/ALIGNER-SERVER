package team.aligner.course.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.model.Course
import team.aligner.course.model.CourseIdentity
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.CourseStep
import team.aligner.course.model.CourseStepExercise
import team.aligner.course.model.CourseStepStatus

/**
 * `training` 이 세션을 열 때 읽는 스텝 조회의 **소유권**을 고정한다.
 *
 * 이 조건이 없던 동안 다른 회원이 `courseId` 를 추측해 남의 코스 구성을 읽고 그 코스로 세션까지
 * 만들 수 있었다 (이슈 #32). 다른 조회가 전부 `memberId` 를 조건에 넣는데 이 경로만 빠져
 * 있었다.
 */
class CourseStepContractTest :
    DescribeSpec({
        val courseRepository = mockk<CourseRepository>()

        fun contract() = CourseStepContractImpl(courseRepository = courseRepository)

        val owner = 1L
        val stranger = 2L

        fun course() =
            Course(
                identity = CourseIdentity.of(20L),
                memberId = owner,
                templateId = 10L,
                targetPoseId = 3L,
                causeCode = "THORACIC_STIFFNESS",
                status = CourseStatus.IN_PROGRESS,
                steps =
                    listOf(
                        CourseStep(
                            identity = 31L,
                            stepOrder = 1,
                            status = CourseStepStatus.NOT_STARTED,
                            completedAt = null,
                            exercises =
                                listOf(
                                    CourseStepExercise(
                                        identity = 51L,
                                        exerciseId = 101L,
                                        displayOrder = 1,
                                        durationSeconds = 120,
                                        setCount = 1,
                                    ),
                                ),
                        ),
                    ),
                createdAt = null,
                completedAt = null,
            )

        beforeTest {
            every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns course()
        }

        describe("findStep") {
            it("자기 코스의 스텝은 구성을 그대로 내린다") {
                val step = contract().findStep(memberId = owner, courseId = 20L, stepOrder = 1)

                step.shouldNotBeNull()
                step.courseStepId shouldBe 31L
                step.stepOrder shouldBe 1
                step.completed shouldBe false
                step.exercises.single().courseStepExerciseId shouldBe 51L
                step.exercises.single().durationSeconds shouldBe 120
            }

            /**
             * **여기가 이슈 #32 의 보안 결함이다.** 남의 코스와 없는 코스를 같은 null 로
             * 돌려준다 — 구분해서 알려주면 존재 여부가 새어나간다.
             */
            it("남의 코스면 스텝이 있어도 null 이다") {
                contract().findStep(memberId = stranger, courseId = 20L, stepOrder = 1).shouldBeNull()
            }

            it("없는 코스는 null 이다") {
                every { courseRepository.findByIdentity(CourseIdentity.of(99L)) } returns null

                contract().findStep(memberId = owner, courseId = 99L, stepOrder = 1).shouldBeNull()
            }

            it("없는 스텝 순서는 null 이다") {
                contract().findStep(memberId = owner, courseId = 20L, stepOrder = 9).shouldBeNull()
            }
        }
    })
