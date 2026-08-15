package team.aligner.course.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.dao.OptimisticLockingFailureException
import team.aligner.course.infrastructure.CauseLookup
import team.aligner.course.infrastructure.CauseLookupPort
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.infrastructure.TargetPoseCatalogEntry
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.model.Course
import team.aligner.course.model.CourseIdentity
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.CourseStep
import team.aligner.course.model.CourseStepStatus
import team.aligner.course.model.Stamp
import java.time.Instant

/**
 * 완료 리포트의 **"파이어로그 N / 4회"** 를 고정한다.
 *
 * 도장은 자세당 하나가 아니라 **완주마다 하나**다. 코스를 한 번 완주하면 하나가 붙고, 도전
 * 현황에서 같은 자세를 다시 누르면 코스가 처음 상태로 열려 다음 회차가 시작된다. 4 개를
 * 채우면 그 자세를 완성한 것이다.
 *
 * 재도전 경로가 없으면 두 번째 도장을 붙일 방법이 아예 없다는 것이 이 테스트의 요점이다.
 */
class TargetPoseStampTest :
    DescribeSpec({
        val courseRepository = mockk<CourseRepository>()
        val stampRepository = mockk<StampRepository>()
        val causeLookupPort = mockk<CauseLookupPort>()
        val targetPoseCatalogPort = mockk<TargetPoseCatalogPort>()

        fun service() =
            CourseCommandServiceImpl(
                courseRepository = courseRepository,
                courseTemplateRepository = mockk<CourseTemplateRepository>(),
                stampRepository = stampRepository,
                exerciseCatalogPort = mockk<ExerciseCatalogPort>(),
                memberBodyPort = mockk<MemberBodyPort>(),
                causeLookupPort = causeLookupPort,
                targetPoseCatalogPort = targetPoseCatalogPort,
            )

        /** 스텝 2 개짜리 코스. `completedSteps` 개만 완료된 상태로 만든다. */
        fun course(
            completedSteps: Int,
            attemptNo: Int = 1,
        ) = Course(
            identity = CourseIdentity.of(20L),
            memberId = 1L,
            templateId = 1L,
            targetPoseId = 3L,
            causeCode = "PELVIC_TILT",
            status = if (completedSteps == 2) CourseStatus.COMPLETED else CourseStatus.IN_PROGRESS,
            steps =
                (1..2).map { order ->
                    CourseStep(
                        identity = 30L + order,
                        stepOrder = order,
                        status = if (order <= completedSteps) CourseStepStatus.COMPLETED else CourseStepStatus.NOT_STARTED,
                        completedAt = if (order <= completedSteps) Instant.parse("2026-08-10T00:00:00Z") else null,
                        exercises = emptyList(),
                    )
                },
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
            completedAt = if (completedSteps == 2) Instant.parse("2026-08-10T00:00:00Z") else null,
            attemptNo = attemptNo,
            version = 1L,
        )

        beforeTest {
            // mock 은 spec 하나를 함께 쓴다. 지우지 않으면 앞 테스트의 호출이 남아
            // verify(exactly = 0) 이 통과하지 못한다.
            clearMocks(courseRepository, stampRepository, causeLookupPort, targetPoseCatalogPort)
            every { courseRepository.save(any()) } answers { firstArg() }
            every { targetPoseCatalogPort.findAllByIds(listOf(3L)) } returns
                listOf(TargetPoseCatalogEntry(3L, "낙타자세", "pose/camel", "PELVIS", 3))
        }

        describe("completeStep 의 도장") {
            it("마지막 스텝을 끝내면 이번 회차의 도장이 붙는다") {
                every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns course(completedSteps = 1)
                val stamp = slot<Stamp>()
                every { stampRepository.saveIfAbsent(capture(stamp)) } returns true
                every { stampRepository.countAcquired(1L, 3L) } returns 1

                val result = service().completeStep(1L, 20L, 2, emptyList())

                stamp.captured.attemptNo shouldBe 1
                result.stampAcquired shouldBe true
                result.acquiredStampCount shouldBe 1
                result.requiredStampCount shouldBe 4
                result.targetPoseCompleted shouldBe false
            }

            it("리포트 헤더용 자세 정보를 함께 싣는다") {
                // 완료 직후에 화면이 코스와 카탈로그를 다시 부르지 않아도 되도록 한다.
                every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns course(completedSteps = 0)
                every { stampRepository.countAcquired(1L, 3L) } returns 0

                val result = service().completeStep(1L, 20L, 1, emptyList())

                result.targetPoseId shouldBe 3L
                result.targetPoseName shouldBe "낙타자세"
                result.bodyPartCode shouldBe "PELVIS"
                result.level shouldBe 3
            }

            it("네 번째 완주에서 자세를 완성한다") {
                every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns
                    course(completedSteps = 1, attemptNo = 4)
                val stamp = slot<Stamp>()
                every { stampRepository.saveIfAbsent(capture(stamp)) } returns true
                every { stampRepository.countAcquired(1L, 3L) } returns 4

                val result = service().completeStep(1L, 20L, 2, emptyList())

                stamp.captured.attemptNo shouldBe 4
                result.targetPoseCompleted shouldBe true
            }

            it("네 번째 완료 요청을 재시도해도 도장이 다섯 번째로 붙지 않는다") {
                // 화면은 이 조합을 "이미 완성" 으로 읽는다. 완성 축하를 두 번 띄우지 않게
                // stampAcquired 가 false 로 남는 것이 요점이다.
                every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns
                    course(completedSteps = 2, attemptNo = 4)
                every { stampRepository.saveIfAbsent(any()) } returns false
                every { stampRepository.countAcquired(1L, 3L) } returns 4

                val result = service().completeStep(1L, 20L, 2, emptyList())

                result.courseCompleted shouldBe true
                result.stampAcquired shouldBe false
                result.acquiredStampCount shouldBe 4
                result.targetPoseCompleted shouldBe true
            }

            it("아직 남은 스텝이 있으면 도장을 세지만 붙이지는 않는다") {
                // 리포트는 매번 파이어로그를 그리므로 개수는 항상 실린다.
                every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns course(completedSteps = 0)
                every { stampRepository.countAcquired(1L, 3L) } returns 2

                val result = service().completeStep(1L, 20L, 1, emptyList())

                result.courseCompleted shouldBe false
                result.stampAcquired shouldBe false
                result.acquiredStampCount shouldBe 2
                verify(exactly = 0) { stampRepository.saveIfAbsent(any()) }
            }
        }

        describe("recommend 의 재도전") {
            beforeTest {
                every { causeLookupPort.findLatestCauses(1L) } returns
                    listOf(CauseLookup(causeCode = "PELVIC_TILT", bodyPartCode = "PELVIS", rank = 1))
                every { targetPoseCatalogPort.findByBodyPartCodeAndLevel("PELVIS", 3) } returns
                    TargetPoseCatalogEntry(3L, "낙타자세", "pose/camel", "PELVIS", 3)
            }

            it("완주한 코스를 다시 누르면 스텝이 초기화되고 회차가 오른다") {
                every { courseRepository.findByMemberIdAndTargetPoseId(1L, 3L) } returns course(completedSteps = 2)
                every { stampRepository.countAcquired(1L, 3L) } returns 1
                val saved = slot<Course>()
                every { courseRepository.save(capture(saved)) } answers { saved.captured }

                val identity = service().recommend(1L, RecommendCourseCommand(bodyPartCode = "PELVIS", level = 3))

                // 재도전은 새 코스가 아니라 같은 코스의 다음 회차다.
                identity shouldBe CourseIdentity.of(20L)
                saved.captured.attemptNo shouldBe 2
                saved.captured.status shouldBe CourseStatus.IN_PROGRESS
                saved.captured.completedStepCount shouldBe 0
                saved.captured.completedAt shouldBe null
            }

            it("진행 중인 코스는 초기화하지 않는다") {
                // 홈에서 돌아온 회원이 도전 현황에서 같은 자세를 다시 눌러도 하던 자리에서
                // 이어가야 한다.
                every { courseRepository.findByMemberIdAndTargetPoseId(1L, 3L) } returns course(completedSteps = 1)

                service().recommend(1L, RecommendCourseCommand(bodyPartCode = "PELVIS", level = 3))

                verify(exactly = 0) { courseRepository.save(any()) }
            }

            it("저장이 충돌하면 최신 상태를 다시 읽어 한 번 더 연다") {
                // 충돌이 곧 "다른 쪽이 이미 열었다" 는 아니다. 완료 push 재시도가 같은 코스를
                // 다시 저장해 버전만 올린 경우에도 충돌한다 — 그때 성공으로 삼으면 회원이
                // 다시 눌렀는데 코스는 완주 상태 그대로다.
                every { courseRepository.findByMemberIdAndTargetPoseId(1L, 3L) } returns course(completedSteps = 2)
                every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns course(completedSteps = 2)
                every { stampRepository.countAcquired(1L, 3L) } returns 1
                val saved = mutableListOf<Course>()
                every { courseRepository.save(capture(saved)) } throws
                    OptimisticLockingFailureException("충돌") andThenAnswer { saved.last() }

                service().recommend(1L, RecommendCourseCommand(bodyPartCode = "PELVIS", level = 3))

                // 두 번째 저장이 실제로 다시 연 코스여야 한다.
                saved.last().status shouldBe CourseStatus.IN_PROGRESS
                saved.last().attemptNo shouldBe 2
            }

            it("도장을 다 채운 자세는 다시 열지 않는다") {
                // 4 개가 상한이라 다시 열어도 더 붙을 도장이 없다.
                every { courseRepository.findByMemberIdAndTargetPoseId(1L, 3L) } returns course(completedSteps = 2)
                every { stampRepository.countAcquired(1L, 3L) } returns 4

                service().recommend(1L, RecommendCourseCommand(bodyPartCode = "PELVIS", level = 3))

                verify(exactly = 0) { courseRepository.save(any()) }
            }
        }
    })
