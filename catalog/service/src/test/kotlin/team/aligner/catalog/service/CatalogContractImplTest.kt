package team.aligner.catalog.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.model.view.ExerciseSummaryView
import team.aligner.catalog.model.view.TargetPoseDetailView
import java.math.BigDecimal

/**
 * 계약 구현체는 위임과 변환만 한다. 필드가 누락되면 소비 도메인이 조용히 null 을 받으므로
 * 매핑을 통째로 확인한다.
 */
class CatalogContractImplTest :
    DescribeSpec({
        val exerciseQueryService = mockk<ExerciseQueryService>()
        val targetPoseQueryService = mockk<TargetPoseQueryService>()
        val exerciseContract = ExerciseContractImpl(exerciseQueryService)
        val targetPoseContract = TargetPoseContractImpl(targetPoseQueryService)

        beforeTest { clearMocks(exerciseQueryService, targetPoseQueryService) }

        describe("ExerciseContractImpl.findAllByIds") {
            it("모든 필드를 빠짐없이 옮긴다") {
                every { exerciseQueryService.getAll(listOf(ExerciseIdentity.of(1L))) } returns
                    listOf(
                        ExerciseSummaryView(
                            exerciseId = 1L,
                            name = "캣카우",
                            imageAssetKey = "exercise/cat-cow",
                            thumbnailUrl = "https://ymove.test/cat-cow.jpg",
                            defaultSetCount = 1,
                            defaultRepCount = 12,
                            defaultDurationSeconds = 120,
                            metValue = BigDecimal("2.30"),
                            difficulty = "하",
                            category = "가동성 웜업",
                        ),
                    )

                val response = exerciseContract.findAllByIds(listOf(1L)).single()

                response.exerciseId shouldBe 1L
                response.name shouldBe "캣카우"
                response.imageAssetKey shouldBe "exercise/cat-cow"
                response.defaultSetCount shouldBe 1
                response.defaultRepCount shouldBe 12
                response.defaultDurationSeconds shouldBe 120
                response.metValue shouldBe BigDecimal("2.30")
                response.difficulty shouldBe "하"
                response.category shouldBe "가동성 웜업"
            }

            it("빈 목록을 그대로 흘려보낸다") {
                every { exerciseQueryService.getAll(emptyList()) } returns emptyList()

                exerciseContract.findAllByIds(emptyList()) shouldBe emptyList()
            }
        }

        describe("TargetPoseContractImpl.findById") {
            it("모든 필드를 빠짐없이 옮긴다") {
                every { targetPoseQueryService.findDetail(TargetPoseIdentity.of(1L)) } returns
                    TargetPoseDetailView(1L, "낙타자세", "camel-pose", null, "BACK", 2, emptyList())

                val response = targetPoseContract.findById(1L)!!

                response.targetPoseId shouldBe 1L
                response.name shouldBe "낙타자세"
                response.imageAssetKey shouldBe "camel-pose"
                response.bodyPartCode shouldBe "BACK"
                response.level shouldBe 2
            }

            it("없는 자세면 예외가 아니라 null 이다") {
                every { targetPoseQueryService.findDetail(TargetPoseIdentity.of(99L)) } returns null

                targetPoseContract.findById(99L) shouldBe null
            }
        }
    })
