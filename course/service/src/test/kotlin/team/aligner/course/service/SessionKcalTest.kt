package team.aligner.course.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import team.aligner.course.infrastructure.CauseLookupPort
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.ExerciseCatalogEntry
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.model.Course
import team.aligner.course.model.CourseIdentity
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.CourseStep
import team.aligner.course.model.CourseStepExercise
import team.aligner.course.model.CourseStepStatus
import java.math.BigDecimal

/**
 * 세션 완료가 돌려주는 **이번 세션의 소모 칼로리**를 고정한다.
 *
 * 계산이 course 에 있는 이유가 이 테스트다 — MET(catalog)과 몸무게(member)를 이미 읽는 쪽이
 * 여기라 port 를 새로 뚫지 않아도 된다 (docs/domains.md §4-3).
 *
 * **모르면 0 이 아니라 null 이다.** 0 kcal 은 "운동량 없음" 이라 "계산할 수 없음" 과 다르고
 * 화면이 둘을 구분해서 그린다.
 */
class SessionKcalTest :
    DescribeSpec({
        val courseRepository = mockk<CourseRepository>()
        val stampRepository = mockk<StampRepository>(relaxed = true)
        val exerciseCatalogPort = mockk<ExerciseCatalogPort>()
        val memberBodyPort = mockk<MemberBodyPort>()

        fun service() =
            CourseCommandServiceImpl(
                courseRepository = courseRepository,
                courseTemplateRepository = mockk<CourseTemplateRepository>(),
                stampRepository = stampRepository,
                exerciseCatalogPort = exerciseCatalogPort,
                memberBodyPort = memberBodyPort,
                causeLookupPort = mockk<CauseLookupPort>(),
                // 리포트 헤더용 자세 조회는 이 테스트의 관심사가 아니다. catalog 에 자세가
                // 없어도 칼로리 계산은 그대로여야 하므로 빈 결과로 둔다.
                targetPoseCatalogPort = mockk<TargetPoseCatalogPort>(relaxed = true),
            )

        /** 스텝 2 개짜리 코스. 스텝 운동 식별자는 51·52, 운동 식별자는 101·102 다. */
        fun course() =
            Course(
                identity = CourseIdentity.of(20L),
                memberId = 1L,
                templateId = 1L,
                targetPoseId = 3L,
                causeCode = "WEAK_BACK",
                status = CourseStatus.IN_PROGRESS,
                steps =
                    (1..2).map { order ->
                        CourseStep(
                            identity = 30L + order,
                            stepOrder = order,
                            status = CourseStepStatus.NOT_STARTED,
                            completedAt = null,
                            exercises =
                                listOf(
                                    CourseStepExercise(
                                        identity = 50L + order,
                                        exerciseId = 100L + order,
                                        displayOrder = 1,
                                        durationSeconds = 120,
                                        setCount = 1,
                                    ),
                                ),
                        )
                    },
                createdAt = null,
                completedAt = null,
            )

        beforeTest {
            every { courseRepository.findByIdentity(CourseIdentity.of(20L)) } returns course()
            every { courseRepository.save(any()) } answers { firstArg() }
        }

        fun givenCatalog(metValue: BigDecimal?) {
            every { exerciseCatalogPort.findAllByIds(any()) } answers {
                firstArg<List<Long>>().map {
                    ExerciseCatalogEntry(
                        exerciseId = it,
                        name = "캣카우",
                        imageAssetKey = null,
                        thumbnailUrl = null,
                        category = "가동성 웜업",
                        defaultSetCount = 1,
                        defaultDurationSeconds = 120,
                        metValue = metValue,
                    )
                }
            }
        }

        describe("completeStep 의 estimatedKcal") {
            it("실측 수행 시간으로 계산한다") {
                givenCatalog(BigDecimal("3.00"))
                every { memberBodyPort.findWeightKg(1L) } returns 60

                val result =
                    service().completeStep(
                        memberId = 1L,
                        courseId = 20L,
                        stepOrder = 1,
                        performedExercises = listOf(PerformedExercise(courseStepExerciseId = 51L, performedDurationSeconds = 120)),
                    )

                // 3.00 × 3.5 × 60 ÷ 200 × 2 분 = 6.3 → 6
                result.estimatedKcal shouldBe 6
            }

            /**
             * 몸무게를 아직 받지 않은 회원이 흔하다. 그때 0 을 내리면 화면이 "안 움직였다" 로
             * 읽는다.
             */
            it("몸무게를 모르면 null 이다") {
                givenCatalog(BigDecimal("3.00"))
                every { memberBodyPort.findWeightKg(1L) } returns null

                val result =
                    service().completeStep(
                        memberId = 1L,
                        courseId = 20L,
                        stepOrder = 1,
                        performedExercises = listOf(PerformedExercise(courseStepExerciseId = 51L, performedDurationSeconds = 120)),
                    )

                result.estimatedKcal.shouldBeNull()
            }

            it("MET 이 없으면 null 이다") {
                givenCatalog(null)
                every { memberBodyPort.findWeightKg(1L) } returns 60

                val result =
                    service().completeStep(
                        memberId = 1L,
                        courseId = 20L,
                        stepOrder = 1,
                        performedExercises = listOf(PerformedExercise(courseStepExerciseId = 51L, performedDurationSeconds = 120)),
                    )

                result.estimatedKcal.shouldBeNull()
            }

            /**
             * **기본 수행 시간으로 메우지 않는다.** 리포트는 실제로 얼마나 움직였는지를 말하는
             * 화면이라, 모르는 값을 예상치로 채우면 다른 뜻이 된다.
             */
            it("수행 시간을 모르면 null 이다") {
                givenCatalog(BigDecimal("3.00"))
                every { memberBodyPort.findWeightKg(1L) } returns 60

                val result =
                    service().completeStep(
                        memberId = 1L,
                        courseId = 20L,
                        stepOrder = 1,
                        performedExercises = listOf(PerformedExercise(courseStepExerciseId = 51L, performedDurationSeconds = null)),
                    )

                result.estimatedKcal.shouldBeNull()
            }

            it("수행 기록이 없으면 catalog 도 member 도 조회하지 않는다") {
                val result =
                    service().completeStep(memberId = 1L, courseId = 20L, stepOrder = 1, performedExercises = emptyList())

                result.estimatedKcal.shouldBeNull()
                result.completedStepCount shouldBe 1
            }

            it("이 코스에 없는 스텝 운동 식별자는 계산에서 빠진다") {
                givenCatalog(BigDecimal("3.00"))
                every { memberBodyPort.findWeightKg(1L) } returns 60

                val result =
                    service().completeStep(
                        memberId = 1L,
                        courseId = 20L,
                        stepOrder = 1,
                        performedExercises =
                            listOf(PerformedExercise(courseStepExerciseId = 999L, performedDurationSeconds = 120)),
                    )

                result.estimatedKcal.shouldBeNull()
            }
        }
    })
