package team.aligner.catalog.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.MuscleRole
import team.aligner.catalog.model.exception.ExerciseNotFoundException
import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseSummaryView
import team.aligner.catalog.model.view.ExerciseVoiceCueView
import team.aligner.catalog.model.view.MuscleView
import java.math.BigDecimal

class ExerciseQueryServiceTest :
    DescribeSpec({
        val exerciseQueryRepository = mockk<ExerciseQueryRepository>()
        val exerciseQueryService: ExerciseQueryService = ExerciseQueryServiceImpl(exerciseQueryRepository)

        val exerciseIdentity = ExerciseIdentity.of(1L)

        beforeTest { clearMocks(exerciseQueryRepository) }

        describe("getDetail") {
            it("근육과 음성 큐를 함께 돌려준다") {
                every { exerciseQueryRepository.findDetail(exerciseIdentity) } returns
                    ExerciseDetailView(
                        exerciseId = 1L,
                        name = "낙타자세",
                        defaultSetCount = 3,
                        defaultRepCount = null,
                        defaultDurationSeconds = 120,
                        metValue = BigDecimal("3.00"),
                        difficulty = "하",
                        category = "핀포즈",
                        cautionNote = "목을 뒤로 완전히 젖히지 마세요",
                        muscles =
                            listOf(
                                MuscleView("ERECTOR_SPINAE", "척추기립근", "BACK", null, "erector-spinae-back", MuscleRole.STRENGTHEN, 1),
                                MuscleView("ILIOPSOAS", "장요근", "PELVIS", "iliopsoas-front", null, MuscleRole.STRETCH, 2),
                            ),
                        voiceCues =
                            listOf(
                                ExerciseVoiceCueView(1, null, null, "무릎을 골반 너비로 벌리세요"),
                                ExerciseVoiceCueView(2, 35, 75, "명치를 천장으로 끌어올리고 40 초 유지하세요"),
                            ),
                    )

                val detail = exerciseQueryService.getDetail(exerciseIdentity)

                detail.name shouldBe "낙타자세"
                detail.muscles.map { it.role } shouldBe listOf(MuscleRole.STRENGTHEN, MuscleRole.STRETCH)
                detail.category shouldBe "핀포즈"
                // 척추기립근은 뒤에만, 장요근은 앞에만 보인다. 세션 플레이어가 앞·뒤 그림을
                // 따로 칠하므로 반대쪽은 null 로 남아야 한다.
                detail.muscles.map { it.frontHighlightAssetKey } shouldBe listOf(null, "iliopsoas-front")
                detail.muscles.map { it.backHighlightAssetKey } shouldBe listOf("erector-spinae-back", null)
                detail.voiceCues.map { it.startOffsetSeconds } shouldBe listOf(null, 35)
                detail.voiceCues.map { it.endOffsetSeconds } shouldBe listOf(null, 75)
            }

            it("없는 운동이면 ExerciseNotFoundException 이다") {
                every { exerciseQueryRepository.findDetail(exerciseIdentity) } returns null

                shouldThrow<ExerciseNotFoundException> {
                    exerciseQueryService.getDetail(exerciseIdentity)
                }
            }
        }

        describe("getAll") {
            it("찾은 것만 돌려준다") {
                val identities = listOf(ExerciseIdentity.of(1L), ExerciseIdentity.of(99L))
                every { exerciseQueryRepository.findAllByIdentities(identities) } returns
                    listOf(ExerciseSummaryView(1L, "낙타자세", 3, null, 120, BigDecimal("3.00"), "하", "핀포즈"))

                val summaries = exerciseQueryService.getAll(identities)

                summaries.map { it.exerciseId } shouldBe listOf(1L)
            }

            /**
             * IN (:ids) 에 빈 리스트를 넘기면 SQL 이 깨진다. member 에 없던 위험이라 회귀를 막는다.
             */
            it("빈 목록이면 DB 를 치지 않는다") {
                exerciseQueryService.getAll(emptyList()) shouldBe emptyList()

                verify(exactly = 0) { exerciseQueryRepository.findAllByIdentities(any()) }
            }
        }
    })
