package team.aligner.course.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import team.aligner.course.model.CourseTemplate
import team.aligner.course.model.CourseTemplateStep
import team.aligner.course.model.exception.ScreeningRequiredException

/**
 * **코스는 추천이지 처방이 아니다.**
 *
 * 온보딩에서 한 번 제안할 뿐이고, 그 뒤 「자세 도전 현황」이 핀포즈 전체를 펼치면 회원은 진단에
 * 없던 부위도 눌러 시작한다. 그래서 진단 결과에 없는 부위를 거절하던 검증을 없앴다 — 화면에
 * 보이는 자세가 시작되지 않으면 안 된다.
 *
 * 진단을 **한 번도 하지 않은** 회원은 여전히 막는다. 부위 선택의 문제가 아니라 온보딩을 건너뛴
 * 것이라 화면이 온보딩으로 되돌려야 한다.
 */
class CourseRecommendationTest :
    DescribeSpec({
        val courseRepository = mockk<CourseRepository>()
        val courseTemplateRepository = mockk<CourseTemplateRepository>()
        val causeLookupPort = mockk<CauseLookupPort>()
        val targetPoseCatalogPort = mockk<TargetPoseCatalogPort>()

        fun service() =
            CourseCommandServiceImpl(
                courseRepository = courseRepository,
                courseTemplateRepository = courseTemplateRepository,
                stampRepository = mockk<StampRepository>(relaxed = true),
                exerciseCatalogPort = mockk<ExerciseCatalogPort>(),
                memberBodyPort = mockk<MemberBodyPort>(),
                causeLookupPort = causeLookupPort,
                targetPoseCatalogPort = targetPoseCatalogPort,
            )

        fun template() =
            CourseTemplate(
                templateId = 1L,
                targetPoseId = 3L,
                name = "낙타자세 정복하기",
                recommendationReason = null,
                steps = listOf(CourseTemplateStep(stepOrder = 1, exercises = emptyList())),
            )

        beforeTest {
            every { targetPoseCatalogPort.findByBodyPartCodeAndLevel("BACK", 1) } returns
                TargetPoseCatalogEntry(3L, "낙타자세", "pose/camel", null, "BACK", 1)
            every { courseTemplateRepository.findByTargetPoseId(3L) } returns template()
            every { courseRepository.findByMemberIdAndTargetPoseId(1L, 3L) } returns null
        }

        describe("recommend") {
            it("진단 결과에 없는 부위도 코스를 만든다") {
                // 예전에는 여기서 400 BODY_PART_NOT_IN_SCREENING 이 났다.
                every { causeLookupPort.findLatestCauses(1L) } returns
                    listOf(CauseLookup(causeCode = "WEAK_CORE", bodyPartCode = "ABDOMEN", rank = 1))

                val saved = slot<Course>()
                every { courseRepository.save(capture(saved)) } answers
                    { saved.captured.copy(identity = CourseIdentity.of(20L)) }

                service().recommend(1L, RecommendCourseCommand(bodyPartCode = "BACK", level = 1)) shouldBe
                    CourseIdentity.of(20L)

                // 맞는 원인이 없으면 스냅샷은 비운다. 저장을 막지는 않는다.
                saved.captured.causeCode.shouldBeNull()
            }

            it("진단 결과에 있는 부위면 원인을 스냅샷으로 남긴다") {
                every { causeLookupPort.findLatestCauses(1L) } returns
                    listOf(CauseLookup(causeCode = "THORACIC_STIFFNESS", bodyPartCode = "BACK", rank = 1))

                val saved = slot<Course>()
                every { courseRepository.save(capture(saved)) } answers
                    { saved.captured.copy(identity = CourseIdentity.of(20L)) }

                service().recommend(1L, RecommendCourseCommand(bodyPartCode = "BACK", level = 1))

                saved.captured.causeCode shouldBe "THORACIC_STIFFNESS"
            }

            it("진단을 한 번도 하지 않았으면 여전히 막는다") {
                every { causeLookupPort.findLatestCauses(1L) } returns emptyList()

                shouldThrow<ScreeningRequiredException> {
                    service().recommend(1L, RecommendCourseCommand(bodyPartCode = "BACK", level = 1))
                }
            }
        }
    })
