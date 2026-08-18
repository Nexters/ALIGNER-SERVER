package team.aligner.course.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseSkeleton
import team.aligner.course.infrastructure.CourseStepExerciseSkeleton
import team.aligner.course.infrastructure.CourseStepSkeleton
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.CourseTemplateSkeleton
import team.aligner.course.infrastructure.ExerciseCatalogEntry
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.TargetPoseCatalogEntry
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.infrastructure.TargetPoseStampCount
import team.aligner.course.infrastructure.TemplateStepExerciseSkeleton
import team.aligner.course.model.exception.InProgressCourseNotFoundException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 홈의 「내일 운동 미리보기」를 고정한다.
 *
 * 규칙은 하나다 — **오늘의 코스를 완주했을 때만, 같은 부위에서 아직 4 번 완수하지 못한 자세
 * 중 하나를 무작위로** 보여준다. 방금 완주한 자세도 후보다. 자세 하나를 완성하려면 같은
 * 코스를 4 번 완주해야 하므로 같은 자세가 다시 나오는 것이 정상 루프다.
 *
 * 무작위라 "무엇이 나오는가" 를 단언할 수 없다. **후보 집합이 맞는지**를 본다 — 난수를
 * 고정하고 여러 번 돌려 나온 자세가 규칙을 벗어나지 않는 것으로 확인한다.
 */
class TomorrowCoursePreviewTest :
    DescribeSpec({
        val courseQueryRepository = mockk<CourseQueryRepository>()
        val targetPoseCatalogPort = mockk<TargetPoseCatalogPort>()
        val exerciseCatalogPort = mockk<ExerciseCatalogPort>()
        val memberBodyPort = mockk<MemberBodyPort>()

        val seoul = ZoneId.of("Asia/Seoul")

        /** 2026-08-15 12:00 KST. 그날의 시작은 2026-08-14T15:00Z 다. */
        val now: Instant = Instant.parse("2026-08-15T03:00:00Z")
        val startOfToday: Instant = Instant.parse("2026-08-14T15:00:00Z")

        fun service(at: Instant = now) =
            CourseQueryServiceImpl(
                courseQueryRepository = courseQueryRepository,
                // 이 화면은 템플릿을 읽지 않는다. 운영 목록 조회만 쓰는 의존이다.
                courseTemplateRepository = mockk<CourseTemplateRepository>(),
                targetPoseCatalogPort = targetPoseCatalogPort,
                exerciseCatalogPort = exerciseCatalogPort,
                memberBodyPort = memberBodyPort,
                clock = Clock.fixed(at, seoul),
            )

        /** 등 3 개, 복부 1 개. */
        fun poses() =
            listOf(
                TargetPoseCatalogEntry(1L, "낙타자세", "pose/camel", null, "BACK", 1),
                TargetPoseCatalogEntry(2L, "휠", "pose/wheel", null, "BACK", 2),
                TargetPoseCatalogEntry(3L, "비둘기자세", "pose/pigeon", null, "BACK", 3),
                TargetPoseCatalogEntry(4L, "보트자세", "pose/boat", null, "ABDOMEN", 1),
            )

        fun exercise(
            exerciseId: Long,
            durationSeconds: Int?,
            setCount: Int?,
        ) = CourseStepExerciseSkeleton(
            courseStepExerciseId = exerciseId + 100,
            exerciseId = exerciseId,
            displayOrder = 1,
            durationSeconds = durationSeconds,
            setCount = setCount,
        )

        fun skeleton(
            courseId: Long,
            targetPoseId: Long,
            completed: Boolean,
        ) = CourseSkeleton(
            courseId = courseId,
            targetPoseId = targetPoseId,
            templateName = "낙타자세 정복하기",
            recommendationReason = "등과 골반 근육 강화에 집중해 보세요",
            completed = completed,
            completedStepCount = if (completed) 2 else 1,
            totalStepCount = 2,
            currentStepOrder = if (completed) null else 2,
            steps =
                listOf(
                    CourseStepSkeleton(1L, 1, true, null, listOf(exercise(10L, 120, 1))),
                    CourseStepSkeleton(2L, 2, completed, null, listOf(exercise(11L, null, null))),
                ),
        )

        beforeTest {
            // 호출 기록까지 지운다. mock 이 spec 하나를 함께 쓰므로 지우지 않으면
            // verify(exactly = 0) 이 앞 테스트의 호출을 본다.
            clearMocks(courseQueryRepository, targetPoseCatalogPort, exerciseCatalogPort, memberBodyPort)

            every { targetPoseCatalogPort.findAllByIds(listOf(1L)) } returns listOf(poses().first())
            every { targetPoseCatalogPort.findAll() } returns poses()
            every { memberBodyPort.findWeightKg(1L) } returns 60
            every { exerciseCatalogPort.findAllByIds(any()) } returns
                listOf(
                    ExerciseCatalogEntry(10L, "캣카우", "exercise/cat-cow", null, "가동성 웜업", 1, 60, BigDecimal("2.5")),
                    ExerciseCatalogEntry(11L, "브릿지", "exercise/bridge", null, "강화", 2, 90, BigDecimal("3.0")),
                )
            every { courseQueryRepository.findStampCounts(1L) } returns emptyList()
        }

        describe("getTodayCourse") {
            it("진행 중인 코스에는 미리보기가 없다") {
                // 오늘 할 일이 남아 있는데 내일 것을 먼저 보여줄 자리가 화면에 없다.
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = false)

                val result = service().getTodayCourse(1L)

                result.completed shouldBe false
                result.tomorrowPreview.shouldBeNull()
                verify(exactly = 0) { targetPoseCatalogPort.findAll() }
            }

            it("완주하면 완료 상태와 미리보기가 함께 나온다") {
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)
                every { courseQueryRepository.findCourseSkeletonByTargetPoseId(1L, any()) } returns null
                every { courseQueryRepository.findTemplateSkeleton(any()) } returns
                    CourseTemplateSkeleton(
                        targetPoseId = 2L,
                        templateName = "휠 정복하기",
                        recommendationReason = "등 신전에 집중해 보세요",
                        totalStepCount = 2,
                        exercises =
                            listOf(
                                TemplateStepExerciseSkeleton(10L, 120, 1),
                                TemplateStepExerciseSkeleton(11L, null, null),
                            ),
                    )

                val preview = service().getTodayCourse(1L).tomorrowPreview.shouldNotBeNull()

                preview.name shouldBe "휠 정복하기"
                preview.totalStepCount shouldBe 2
                preview.exerciseCount shouldBe 2
                // 세트는 override 1 + catalog 기본값 2 다.
                preview.totalSetCount shouldBe 3
                // 시간은 override 120 + catalog 기본값 90 이다.
                preview.estimatedDurationSeconds shouldBe 210
            }

            it("같은 부위에서만 고르고 4 번 완수한 자세는 빼며 방금 완주한 자세는 남긴다") {
                // 낙타자세(1)를 방금 완주했고 휠(2)은 이미 4 번 완수했다.
                // 남는 후보는 낙타자세와 비둘기자세뿐이고, 다른 부위인 보트자세(4)는 나오지 않는다.
                //
                // 무작위라 "무엇이 나오는가" 는 단언할 수 없다. 날짜를 40 일 옮겨 가며 뽑아
                // **나온 자세가 후보 집합을 벗어나지 않고 둘 다 나온다**는 것으로 확인한다.
                every { courseQueryRepository.findTodayCourseSkeleton(1L, any()) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)
                every { courseQueryRepository.findStampCounts(1L) } returns
                    listOf(
                        TargetPoseStampCount(targetPoseId = 1L, acquiredStampCount = 1),
                        TargetPoseStampCount(targetPoseId = 2L, acquiredStampCount = 4),
                    )
                every { courseQueryRepository.findCourseSkeletonByTargetPoseId(1L, any()) } returns null
                every { courseQueryRepository.findTemplateSkeleton(any()) } answers
                    {
                        CourseTemplateSkeleton(firstArg(), "코스", null, 1, listOf(TemplateStepExerciseSkeleton(10L, 60, 1)))
                    }

                val picked =
                    (0L until 40L)
                        .map { days -> service(at = now.plus(days, ChronoUnit.DAYS)) }
                        .map { service ->
                            service
                                .getTodayCourse(1L)
                                .tomorrowPreview
                                .shouldNotBeNull()
                                .targetPoseId
                        }.distinct()

                picked shouldContainExactlyInAnyOrder listOf(1L, 3L)
            }

            it("같은 날 다시 조회해도 같은 자세가 나온다") {
                // 홈을 다시 불러올 때마다 카드가 바뀌면 회원이 이것을 "내일 할 운동" 으로 읽지 못한다.
                every { courseQueryRepository.findTodayCourseSkeleton(1L, any()) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)
                every { courseQueryRepository.findCourseSkeletonByTargetPoseId(1L, any()) } returns null
                every { courseQueryRepository.findTemplateSkeleton(any()) } answers
                    {
                        CourseTemplateSkeleton(firstArg(), "코스", null, 1, listOf(TemplateStepExerciseSkeleton(10L, 60, 1)))
                    }

                // 같은 날의 다른 시각으로 세 번 조회한다.
                val picked =
                    listOf(now, now.plus(1, ChronoUnit.HOURS), now.plus(5, ChronoUnit.HOURS))
                        .map { at ->
                            service(at)
                                .getTodayCourse(1L)
                                .tomorrowPreview
                                .shouldNotBeNull()
                                .targetPoseId
                        }

                picked.distinct().size shouldBe 1
            }

            it("이미 시작한 자세는 템플릿이 아니라 회원의 코스에서 센다") {
                // 코스 스텝은 추천 시점에 복사된다. 템플릿 seed 가 나중에 바뀌어도 회원이
                // 내일 실제로 수행할 것은 복사본이다.
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)
                every { targetPoseCatalogPort.findAll() } returns listOf(poses().first())
                every { courseQueryRepository.findCourseSkeletonByTargetPoseId(1L, 1L) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)

                val preview = service().getTodayCourse(1L).tomorrowPreview.shouldNotBeNull()

                preview.targetPoseId shouldBe 1L
                preview.name shouldBe "낙타자세 정복하기"
                preview.exerciseCount shouldBe 2
                verify(exactly = 0) { courseQueryRepository.findTemplateSkeleton(any()) }
            }

            it("그 부위를 모두 완수했으면 미리보기가 없다") {
                // 미리보기 하나 때문에 완료 화면 전체를 실패시키지 않는다.
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)
                every { courseQueryRepository.findStampCounts(1L) } returns
                    listOf(1L, 2L, 3L).map { TargetPoseStampCount(targetPoseId = it, acquiredStampCount = 4) }

                val result = service().getTodayCourse(1L)

                result.completed shouldBe true
                result.tomorrowPreview.shouldBeNull()
            }

            it("코스 템플릿 seed 가 없는 자세를 골랐으면 미리보기가 없다") {
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)
                every { courseQueryRepository.findCourseSkeletonByTargetPoseId(1L, any()) } returns null
                every { courseQueryRepository.findTemplateSkeleton(any()) } returns null

                service().getTodayCourse(1L).tomorrowPreview.shouldBeNull()
            }

            it("catalog 에 오늘의 자세가 없으면 부위를 몰라 미리보기가 없다") {
                // 도메인 간 FK 가 없어 course seed 가 catalog 보다 앞서갈 수 있다.
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = true)
                every { targetPoseCatalogPort.findAllByIds(listOf(1L)) } returns emptyList()

                val result = service().getTodayCourse(1L)

                result.completed shouldBe true
                result.tomorrowPreview.shouldBeNull()
            }

            it("코스가 하나도 없으면 404 그대로다") {
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns null

                shouldThrow<InProgressCourseNotFoundException> { service().getTodayCourse(1L) }
            }

            it("오늘의 경계를 한국 시간으로 잡는다") {
                // 서버가 UTC 로 떠도 회원의 하루는 KST 다. 12:00 KST 는 그날 00:00 KST 이후다.
                every { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) } returns
                    skeleton(courseId = 20L, targetPoseId = 1L, completed = false)

                service().getTodayCourse(1L)

                verify { courseQueryRepository.findTodayCourseSkeleton(1L, startOfToday) }
            }
        }
    })
