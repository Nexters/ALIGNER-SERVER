package team.aligner.training.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import team.aligner.training.infrastructure.CourseProgressLookup
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepExerciseLookup
import team.aligner.training.infrastructure.CourseStepLookup
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.ExerciseDetailLookup
import team.aligner.training.infrastructure.ExerciseDetailPort
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import team.aligner.training.infrastructure.SessionRepository
import team.aligner.training.model.PerceivedResult
import team.aligner.training.model.Session
import team.aligner.training.model.SessionCourseProgressSnapshot
import team.aligner.training.model.SessionIdentity
import team.aligner.training.model.StepExercise
import java.time.Instant

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
                ).copy(identity = SessionIdentity.of(100L), startedAt = Instant.parse("2026-08-10T00:00:00Z"))

        fun givenPushReturns(
            kcal: Int?,
            acquiredStampCount: Int = 0,
            targetPoseCompleted: Boolean = false,
        ) {
            every { courseProgressPort.completeSession(any(), any(), any(), any()) } returns
                CourseProgressLookup(
                    courseId = 20L,
                    completedStepCount = 1,
                    totalStepCount = 6,
                    courseCompleted = false,
                    stampAcquired = false,
                    estimatedKcal = kcal,
                    targetPoseId = 3L,
                    targetPoseExerciseId = 110L,
                    targetPoseName = "낙타자세",
                    bodyPartCode = "PELVIS",
                    level = 3,
                    acquiredStampCount = acquiredStampCount,
                    requiredStampCount = 4,
                    targetPoseCompleted = targetPoseCompleted,
                )
        }

        beforeTest {
            clearMocks(courseProgressPort, answers = false)
            every { sessionRepository.save(any()) } answers { firstArg() }
            every { courseStepPort.findStep(1L, 20L, 1) } returns
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

        describe("complete 의 리포트 스냅샷") {
            it("최초 완료에서 course 가 계산한 칼로리와 진행도 스냅샷을 저장하고 반환한다") {
                complete(session())
                givenPushReturns(kcal = 63, acquiredStampCount = 1)

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.estimatedKcal shouldBe 63
                val progress = view.courseProgress
                progress.shouldNotBeNull()
                progress.completedStepCount shouldBe 1
                progress.totalStepCount shouldBe 6
                progress.targetPoseName shouldBe "낙타자세"
                progress.bodyPartCode shouldBe "PELVIS"
                progress.level shouldBe 3
                progress.acquiredStampCount shouldBe 1
                progress.requiredStampCount shouldBe 4
                progress.stampAcquired shouldBe false
                progress.targetPoseCompleted shouldBe false
            }

            it("도장을 획득한 완료 세션은 stampAcquired = true 스냅샷을 저장한다") {
                complete(session())
                every { courseProgressPort.completeSession(any(), any(), any(), any()) } returns
                    CourseProgressLookup(
                        courseId = 20L,
                        completedStepCount = 6,
                        totalStepCount = 6,
                        courseCompleted = true,
                        stampAcquired = true,
                        estimatedKcal = 120,
                        targetPoseId = 3L,
                        targetPoseExerciseId = 110L,
                        targetPoseName = "낙타자세",
                        bodyPartCode = "PELVIS",
                        level = 3,
                        acquiredStampCount = 1,
                        requiredStampCount = 4,
                        targetPoseCompleted = false,
                    )

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.courseProgress?.stampAcquired shouldBe true
                view.courseProgress?.courseCompleted shouldBe true
            }

            it("계산이 성립하지 않으면 estimatedKcal 은 null 로 남는다") {
                complete(session())
                givenPushReturns(null)

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.estimatedKcal.shouldBeNull()
                view.courseProgress.shouldNotBeNull()
            }

            it("완료 재시도 시 이미 저장된 스냅샷과 칼로리가 덮어써지지 않고 그대로 반환된다") {
                val alreadySnapshot =
                    SessionCourseProgressSnapshot(
                        completedStepCount = 1,
                        totalStepCount = 6,
                        courseCompleted = false,
                        stampAcquired = true,
                        targetPoseId = 3L,
                        targetPoseExerciseId = 110L,
                        targetPoseName = "낙타자세",
                        bodyPartCode = "PELVIS",
                        level = 3,
                        acquiredStampCount = 1,
                        requiredStampCount = 4,
                        targetPoseCompleted = false,
                    )
                val alreadyCompleted =
                    session()
                        .complete(results = emptyList(), at = Instant.parse("2026-08-10T00:15:00Z"))
                        .withCompletionReport(estimatedKcal = 63, progress = alreadySnapshot)
                complete(alreadyCompleted)

                val view = service().complete(memberId = 1L, sessionId = 100L, command = CompleteSessionCommand(emptyList()))

                view.estimatedKcal shouldBe 63
                view.courseProgress?.stampAcquired shouldBe true
                view.courseProgress?.completedStepCount shouldBe 1
            }

            it("snapshot 없는 legacy 완료 세션 재시도는 course 를 다시 반영하지 않는다") {
                val legacyCompleted =
                    session().complete(
                        results = emptyList(),
                        at = Instant.parse("2026-08-10T00:15:00Z"),
                    )
                // courseProgress == null
                complete(legacyCompleted)

                val view =
                    service().complete(
                        memberId = 1L,
                        sessionId = 100L,
                        command = CompleteSessionCommand(emptyList()),
                    )

                view.courseProgress.shouldBeNull()

                verify(exactly = 0) {
                    courseProgressPort.completeSession(any(), any(), any(), any())
                }
            }
        }

        describe("getSession") {
            it("진행 중인 세션 조회 시 courseProgress 는 null 이다") {
                every { sessionRepository.findByIdentity(SessionIdentity.of(100L)) } returns session()

                val view = service().getSession(memberId = 1L, sessionId = 100L)

                view.status.name shouldBe "IN_PROGRESS"
                view.courseProgress.shouldBeNull()
            }

            it("완료된 세션 조회 시 저장된 courseProgress 스냅샷을 반환한다 (POST 완료 결과와 100% 동일)") {
                val snapshot =
                    SessionCourseProgressSnapshot(
                        completedStepCount = 1,
                        totalStepCount = 6,
                        courseCompleted = false,
                        stampAcquired = true,
                        targetPoseId = 2L,
                        targetPoseExerciseId = 110L,
                        targetPoseName = "낙타자세",
                        bodyPartCode = "BACK",
                        level = 2,
                        acquiredStampCount = 1,
                        requiredStampCount = 4,
                        targetPoseCompleted = false,
                    )
                val completed =
                    session()
                        .complete(results = emptyList(), at = Instant.parse("2026-08-10T00:15:00Z"))
                        .withCompletionReport(estimatedKcal = 63, progress = snapshot)
                every { sessionRepository.findByIdentity(SessionIdentity.of(100L)) } returns completed

                val view = service().getSession(memberId = 1L, sessionId = 100L)

                view.status.name shouldBe "COMPLETED"
                view.estimatedKcal shouldBe 63
                val progress = view.courseProgress
                progress.shouldNotBeNull()
                progress.completedStepCount shouldBe 1
                progress.totalStepCount shouldBe 6
                progress.targetPoseName shouldBe "낙타자세"
                progress.bodyPartCode shouldBe "BACK"
                progress.level shouldBe 2
                progress.acquiredStampCount shouldBe 1
                progress.requiredStampCount shouldBe 4
                progress.stampAcquired shouldBe true
                progress.targetPoseCompleted shouldBe false
            }
        }

        describe("recordPerceivedResult") {
            it("체감 기록 후 반환되는 세션 응답에도 기존 courseProgress 스냅샷이 온전히 유지된다") {
                val snapshot =
                    SessionCourseProgressSnapshot(
                        completedStepCount = 1,
                        totalStepCount = 6,
                        courseCompleted = false,
                        stampAcquired = true,
                        targetPoseId = 2L,
                        targetPoseExerciseId = 110L,
                        targetPoseName = "낙타자세",
                        bodyPartCode = "BACK",
                        level = 2,
                        acquiredStampCount = 1,
                        requiredStampCount = 4,
                        targetPoseCompleted = false,
                    )
                val completed =
                    session()
                        .complete(results = emptyList(), at = Instant.parse("2026-08-10T00:15:00Z"))
                        .withCompletionReport(estimatedKcal = 63, progress = snapshot)
                every { sessionRepository.findByIdentity(SessionIdentity.of(100L)) } returns completed

                val view =
                    service().recordPerceivedResult(
                        memberId = 1L,
                        sessionId = 100L,
                        perceivedResult = PerceivedResult.SUCCEEDED,
                    )

                view.perceivedResult shouldBe PerceivedResult.SUCCEEDED
                view.estimatedKcal shouldBe 63
                val progress = view.courseProgress
                progress.shouldNotBeNull()
                progress.completedStepCount shouldBe 1
                progress.targetPoseName shouldBe "낙타자세"
                progress.stampAcquired shouldBe true
            }
        }
    })
