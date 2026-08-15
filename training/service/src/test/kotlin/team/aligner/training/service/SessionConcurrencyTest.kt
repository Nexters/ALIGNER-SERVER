package team.aligner.training.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.OptimisticLockingFailureException
import team.aligner.training.infrastructure.CourseProgressLookup
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepExerciseLookup
import team.aligner.training.infrastructure.CourseStepLookup
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.ExerciseDetailLookup
import team.aligner.training.infrastructure.ExerciseDetailPort
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import team.aligner.training.infrastructure.SessionRepository
import team.aligner.training.model.ExerciseResult
import team.aligner.training.model.Session
import team.aligner.training.model.SessionIdentity
import team.aligner.training.model.StepExercise
import team.aligner.training.model.exception.CourseStepNotFoundException
import java.time.Instant

/**
 * 이슈 #32 의 소유권·동시성을 고정한다.
 *
 * 두 결함이 한 파일에 있는 것은 둘 다 **세션 완료 경로의 저장 순서** 문제라서다 — 소유권은
 * 무엇을 읽는가, 낙관적 락은 무엇을 덮는가다.
 */
class SessionConcurrencyTest :
    DescribeSpec({
        val sessionRepository = mockk<SessionRepository>()
        val courseStepPort = mockk<CourseStepPort>()
        val courseProgressPort = mockk<CourseProgressPort>()
        val exerciseDetailPort = mockk<ExerciseDetailPort>()

        val owner = 1L
        val stranger = 2L

        fun service() =
            SessionServiceImpl(
                sessionRepository = sessionRepository,
                sessionAchievementQueryRepository = mockk<SessionAchievementQueryRepository>(),
                courseStepPort = courseStepPort,
                courseProgressPort = courseProgressPort,
                exerciseDetailPort = exerciseDetailPort,
            )

        fun stepLookup() =
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

        fun session() =
            Session
                .start(
                    memberId = owner,
                    courseId = 20L,
                    stepOrder = 1,
                    exercises = listOf(StepExercise(courseStepExerciseId = 51L, exerciseId = 101L, displayOrder = 1)),
                ).copy(
                    identity = SessionIdentity.of(100L),
                    startedAt = Instant.parse("2026-08-10T00:00:00Z"),
                    version = 3L,
                )

        beforeTest {
            // **목을 매 테스트마다 비운다.** spec 바깥에 선언한 목은 호출 기록이 누적되므로
            // verify(exactly = n) 이 앞선 테스트의 호출까지 센다.
            clearMocks(sessionRepository, courseStepPort, courseProgressPort, exerciseDetailPort)

            // 세션 조회는 스텝 구성을 다시 읽는다. 세션 자신의 memberId 로 부르므로 소유자다.
            every { courseStepPort.findStep(owner, 20L, 1) } returns stepLookup()
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
            every { courseProgressPort.completeSession(any(), any(), any(), any()) } returns
                CourseProgressLookup(
                    courseId = 20L,
                    completedStepCount = 1,
                    totalStepCount = 6,
                    courseCompleted = false,
                    stampAcquired = true,
                    estimatedKcal = 63,
                    targetPoseId = 3L,
                    targetPoseName = "낙타자세",
                    bodyPartCode = "PELVIS",
                    level = 3,
                    acquiredStampCount = 1,
                    requiredStampCount = 4,
                    targetPoseCompleted = false,
                )
        }

        describe("start 의 소유권") {
            /**
             * **여기가 이슈 #32 의 보안 결함이다.** 소유권 판단은 course 가 하므로, training 이
             * 검증할 것은 "인증 회원 식별자를 조회에 실어 보내는가" 다. 넘기지 않으면 course 가
             * 걸러낼 방법이 없다.
             */
            it("스텝 조회에 인증 회원 식별자를 넘긴다") {
                // 저장 어댑터가 식별자와 시작 시각을 채운다 (SessionRepositoryImpl).
                every { sessionRepository.save(any()) } answers
                    {
                        firstArg<Session>().copy(
                            identity = SessionIdentity.of(100L),
                            startedAt = Instant.parse("2026-08-10T00:00:00Z"),
                        )
                    }

                service().start(memberId = owner, command = StartSessionCommand(courseId = 20L, stepOrder = 1))

                verify(exactly = 1) { courseStepPort.findStep(owner, 20L, 1) }
            }

            it("남의 코스면 course 가 null 을 주고 404 가 된다") {
                every { courseStepPort.findStep(stranger, 20L, 1) } returns null

                shouldThrow<CourseStepNotFoundException> {
                    service().start(memberId = stranger, command = StartSessionCommand(courseId = 20L, stepOrder = 1))
                }

                verify(exactly = 0) { sessionRepository.save(any()) }
            }
        }

        describe("complete 의 낙관적 락") {
            /**
             * 두 완료 요청이 동시에 같은 `IN_PROGRESS` 세션을 읽으면 나중 저장이 실패한다.
             * 다시 읽으면 **앞선 완료가 보이므로** 두 번째 시도는 기록을 덮지 않는다.
             */
            it("충돌하면 다시 읽어 재시도하고 앞선 기록을 덮지 않는다") {
                val alreadyCompleted =
                    session()
                        .complete(
                            results = listOf(ExerciseResult(51L, completed = true, performedDurationSeconds = 120)),
                            at = Instant.parse("2026-08-10T00:15:00Z"),
                        ).copy(version = 4L)

                // 첫 읽기는 진행 중, 저장에서 충돌한다. 다시 읽으면 남이 완료해 둔 상태다.
                every { sessionRepository.findByIdentity(SessionIdentity.of(100L)) } returnsMany
                    listOf(session(), alreadyCompleted)
                every { sessionRepository.save(any()) } throws OptimisticLockingFailureException("충돌") andThenAnswer
                    { firstArg() }

                val view =
                    service().complete(
                        memberId = owner,
                        sessionId = 100L,
                        command =
                            CompleteSessionCommand(
                                listOf(ExerciseResultCommand(51L, completed = false, performedDurationSeconds = 0)),
                            ),
                    )

                // 앞선 완료의 값이 남는다. 재시도가 덮었다면 completed=false 로 뒤집힌다.
                view.exerciseRecords.single().completed shouldBe true
                view.exerciseRecords.single().performedDurationSeconds shouldBe 120
                verify(exactly = 2) { sessionRepository.findByIdentity(SessionIdentity.of(100L)) }
            }

            /**
             * **재시도에서도 push 는 한다.** course 계약이 재호출을 멱등하게 흡수하고, 그것이
             * 진행도 반영의 유일한 경로다 — 건너뛰면 첫 요청이 실패한 경우 진행도가 영구히
             * 안 오른다.
             */
            it("재시도로 판정돼도 코스에 push 한다") {
                val alreadyCompleted =
                    session()
                        .complete(results = emptyList(), at = Instant.parse("2026-08-10T00:15:00Z"))
                        .copy(version = 4L)
                every { sessionRepository.findByIdentity(SessionIdentity.of(100L)) } returns alreadyCompleted
                every { sessionRepository.save(any()) } answers { firstArg() }

                service().complete(memberId = owner, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                verify(exactly = 1) { courseProgressPort.completeSession(owner, 20L, 1, any()) }
            }

            it("두 번째도 충돌하면 그대로 올린다") {
                every { sessionRepository.findByIdentity(SessionIdentity.of(100L)) } returns session()
                every { sessionRepository.save(any()) } throws OptimisticLockingFailureException("충돌")

                shouldThrow<OptimisticLockingFailureException> {
                    service().complete(memberId = owner, sessionId = 100L, command = CompleteSessionCommand(emptyList()))
                }
            }
        }
    })
