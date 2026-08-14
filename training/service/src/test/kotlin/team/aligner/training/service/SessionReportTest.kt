package team.aligner.training.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import team.aligner.training.infrastructure.CourseProgressLookup
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepExerciseLookup
import team.aligner.training.infrastructure.CourseStepLookup
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.ExerciseDetailLookup
import team.aligner.training.infrastructure.ExerciseDetailPort
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import team.aligner.training.infrastructure.SessionRepository
import team.aligner.training.model.Session
import team.aligner.training.model.SessionIdentity
import team.aligner.training.model.StepExercise

/**
 * 완료 리포트의 소모 칼로리가 **최초 완료 시점에 고정되는지**를 고정한다.
 *
 * 이 규칙이 없으면, 몸무게를 아직 안 넣은 회원이 세션을 마쳐 null 로 남은 뒤 몸무게를
 * 입력하고 완료를 재시도했을 때 그날의 리포트에 오늘 값이 얹힌다. 화면은 그것을 "그날 태운
 * 칼로리" 로 읽는다.
 */
class SessionReportTest :
    DescribeSpec({
        val sessionRepository = mockk<SessionRepository>()
        val courseStepPort = mockk<CourseStepPort>()
        val courseProgressPort = mockk<CourseProgressPort>()
        val exerciseDetailPort = mockk<ExerciseDetailPort>()

        fun service() =
            SessionServiceImpl(
                sessionRepository = sessionRepository,
                sessionAchievementQueryRepository = mockk<SessionAchievementQueryRepository>(),
                courseStepPort = courseStepPort,
                courseProgressPort = courseProgressPort,
                exerciseDetailPort = exerciseDetailPort,
            )

        fun session() =
            Session
                .start(
                    memberId = 1L,
                    courseId = 20L,
                    stepOrder = 1,
                    exercises = listOf(StepExercise(courseStepExerciseId = 51L, exerciseId = 101L, displayOrder = 1)),
                ).copy(identity = SessionIdentity.of(100L), startedAt = java.time.Instant.parse("2026-08-10T00:00:00Z"))

        fun givenPushReturns(kcal: Int?) {
            every { courseProgressPort.completeSession(any(), any(), any(), any()) } returns
                CourseProgressLookup(
                    courseId = 20L,
                    completedStepCount = 1,
                    totalStepCount = 6,
                    courseCompleted = false,
                    stampAcquired = false,
                    estimatedKcal = kcal,
                )
        }

        beforeTest {
            every { sessionRepository.save(any()) } answers { firstArg() }
            every { courseStepPort.findStep(20L, 1) } returns
                CourseStepLookup(
                    courseId = 20L,
                    courseStepId = 31L,
                    stepOrder = 1,
                    completed = false,
                    exercises =
                        listOf(
                            CourseStepExerciseLookup(
                                courseStepExerciseId = 51L,
                                exerciseId = 101L,
                                displayOrder = 1,
                                durationSeconds = 120,
                                setCount = 1,
                            ),
                        ),
                )
            every { exerciseDetailPort.findAllByIds(any()) } returns
                listOf(
                    ExerciseDetailLookup(
                        exerciseId = 101L,
                        name = "캣카우",
                        category = "가동성 웜업",
                        defaultSetCount = 1,
                        defaultDurationSeconds = 120,
                    ),
                )
        }

        fun complete(existing: Session) {
            every { sessionRepository.findByIdentity(SessionIdentity.of(100L)) } returns existing
        }

        describe("complete 의 estimatedKcal") {
            it("최초 완료에서 course 가 계산한 값을 담는다") {
                complete(session())
                givenPushReturns(63)

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.estimatedKcal shouldBe 63
            }

            it("계산이 성립하지 않으면 null 로 남는다") {
                complete(session())
                givenPushReturns(null)

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.estimatedKcal.shouldBeNull()
            }

            /**
             * **여기가 회귀가 안 보이는 자리다.** 재시도도 200 을 내므로 값이 바뀐 것을
             * 호출부가 알아채지 못한다.
             */
            it("최초 계산이 null 이었으면 재시도에서 뒤늦게 채우지 않는다") {
                val alreadyCompleted =
                    session().complete(results = emptyList(), at = java.time.Instant.parse("2026-08-10T00:15:00Z"))
                complete(alreadyCompleted)
                // 그 사이 회원이 몸무게를 입력해 이번에는 계산이 성립한다.
                givenPushReturns(63)

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.estimatedKcal.shouldBeNull()
            }

            it("이미 담긴 값도 재시도에서 덮이지 않는다") {
                val alreadyCompleted =
                    session()
                        .complete(results = emptyList(), at = java.time.Instant.parse("2026-08-10T00:15:00Z"))
                        .withEstimatedKcal(63)
                complete(alreadyCompleted)
                givenPushReturns(120)

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.estimatedKcal shouldBe 63
            }
        }
    })
