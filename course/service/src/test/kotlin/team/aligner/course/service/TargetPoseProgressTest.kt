package team.aligner.course.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseSkeleton
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.TargetPoseCatalogEntry
import team.aligner.course.infrastructure.TargetPoseCatalogPort

/**
 * 「자세 도전 현황」이 **회원이 시작한 코스가 아니라 서비스가 제공하는 핀포즈 전체**를 펼친다는
 * 것을 고정한다. 코스는 추천이라 아직 시작하지 않은 자세도 화면에 나온다.
 */
class TargetPoseProgressTest :
    DescribeSpec({
        val courseQueryRepository = mockk<CourseQueryRepository>()
        val targetPoseCatalogPort = mockk<TargetPoseCatalogPort>()

        fun service() =
            CourseQueryServiceImpl(
                courseQueryRepository = courseQueryRepository,
                targetPoseCatalogPort = targetPoseCatalogPort,
                exerciseCatalogPort = mockk<ExerciseCatalogPort>(),
                memberBodyPort = mockk<MemberBodyPort>(),
            )

        /** 등 2 개, 복부 1 개. catalog 가 (부위, 레벨) 순으로 정렬해 준 상태다. */
        fun poses() =
            listOf(
                TargetPoseCatalogEntry(1L, "낙타자세", "pose/camel", "BACK", 1),
                TargetPoseCatalogEntry(2L, "휠", "pose/wheel", "BACK", 2),
                TargetPoseCatalogEntry(3L, "보트자세", "pose/boat", "ABDOMEN", 1),
            )

        fun skeleton(
            courseId: Long,
            targetPoseId: Long,
            completed: Boolean,
            completedStepCount: Int,
        ) = CourseSkeleton(
            courseId = courseId,
            targetPoseId = targetPoseId,
            templateName = "코스",
            recommendationReason = null,
            completed = completed,
            completedStepCount = completedStepCount,
            totalStepCount = 4,
            currentStepOrder = null,
            steps = emptyList(),
        )

        describe("getTargetPoseProgress") {
            it("코스를 하나도 시작하지 않아도 자세 전체가 나온다") {
                every { targetPoseCatalogPort.findAll() } returns poses()
                every { courseQueryRepository.findAllCourseSkeletons(1L) } returns emptyList()

                val result = service().getTargetPoseProgress(memberId = 1L, completedOnly = null)

                result.targetPoses.map { it.targetPoseName } shouldBe listOf("낙타자세", "휠", "보트자세")
                result.totalCount shouldBe 3
                result.inProgressCount shouldBe 0
                result.completedCount shouldBe 0
            }

            it("시작하지 않은 자세는 코스 값이 0 이 아니라 null 이다") {
                // 0/4 는 "시작했는데 아직 한 스텝도 안 함" 이고 null 은 "아직 열지 않음" 이다.
                // 화면이 둘을 다르게 그리므로 서버가 섞으면 안 된다.
                every { targetPoseCatalogPort.findAll() } returns poses()
                every { courseQueryRepository.findAllCourseSkeletons(1L) } returns emptyList()

                val item = service().getTargetPoseProgress(memberId = 1L, completedOnly = null).targetPoses.first()

                item.courseId.shouldBeNull()
                item.completedStepCount.shouldBeNull()
                item.totalStepCount.shouldBeNull()
                item.completed shouldBe false
            }

            it("시작한 자세에만 진행도가 붙는다") {
                every { targetPoseCatalogPort.findAll() } returns poses()
                every { courseQueryRepository.findAllCourseSkeletons(1L) } returns
                    listOf(
                        skeleton(courseId = 20L, targetPoseId = 1L, completed = true, completedStepCount = 4),
                        skeleton(courseId = 21L, targetPoseId = 2L, completed = false, completedStepCount = 1),
                    )

                val result = service().getTargetPoseProgress(memberId = 1L, completedOnly = null)

                result.totalCount shouldBe 3
                result.completedCount shouldBe 1
                result.inProgressCount shouldBe 1
                result.targetPoses.map { it.courseId } shouldBe listOf(20L, 21L, null)
                result.targetPoses.map { it.completedStepCount } shouldBe listOf(4, 1, null)
            }

            it("부위와 레벨을 실어 화면이 섹션을 그릴 수 있다") {
                every { targetPoseCatalogPort.findAll() } returns poses()
                every { courseQueryRepository.findAllCourseSkeletons(1L) } returns emptyList()

                val result = service().getTargetPoseProgress(memberId = 1L, completedOnly = null)

                result.targetPoses.map { it.bodyPartCode } shouldBe listOf("BACK", "BACK", "ABDOMEN")
                result.targetPoses.map { it.level } shouldBe listOf(1, 2, 1)
            }

            it("걸러도 집계는 전체 기준이다") {
                // 칩 세 개가 언제나 함께 보인다. 걸러진 목록으로 세면 나머지 칩을 그릴 수 없다.
                every { targetPoseCatalogPort.findAll() } returns poses()
                every { courseQueryRepository.findAllCourseSkeletons(1L) } returns
                    listOf(skeleton(courseId = 20L, targetPoseId = 1L, completed = true, completedStepCount = 4))

                val result = service().getTargetPoseProgress(memberId = 1L, completedOnly = true)

                result.targetPoses.map { it.targetPoseId } shouldBe listOf(1L)
                result.totalCount shouldBe 3
                result.completedCount shouldBe 1
            }

            it("completed=false 는 완성하지 않은 전부다 — 시작하지 않은 자세를 포함한다") {
                every { targetPoseCatalogPort.findAll() } returns poses()
                every { courseQueryRepository.findAllCourseSkeletons(1L) } returns
                    listOf(skeleton(courseId = 20L, targetPoseId = 1L, completed = true, completedStepCount = 4))

                val result = service().getTargetPoseProgress(memberId = 1L, completedOnly = false)

                result.targetPoses.map { it.targetPoseId } shouldBe listOf(2L, 3L)
            }

            it("자세 seed 가 없으면 빈 목록이고 집계는 0 이다") {
                every { targetPoseCatalogPort.findAll() } returns emptyList()
                every { courseQueryRepository.findAllCourseSkeletons(1L) } returns emptyList()

                val result = service().getTargetPoseProgress(memberId = 1L, completedOnly = null)

                result.targetPoses shouldBe emptyList()
                result.totalCount shouldBe 0
            }
        }
    })
