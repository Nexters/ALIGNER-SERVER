package team.aligner.catalog.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import team.aligner.catalog.infrastructure.TargetPoseQueryRepository
import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.model.exception.TargetPoseNotFoundException
import team.aligner.catalog.model.view.TargetPoseDetailView
import team.aligner.catalog.model.view.TargetPoseSummaryView

class TargetPoseQueryServiceTest :
    DescribeSpec({
        val targetPoseQueryRepository = mockk<TargetPoseQueryRepository>()
        val targetPoseQueryService: TargetPoseQueryService = TargetPoseQueryServiceImpl(targetPoseQueryRepository)

        val targetPoseIdentity = TargetPoseIdentity.of(1L)

        beforeTest { clearMocks(targetPoseQueryRepository) }

        describe("getDetail") {
            it("자세를 돌려준다") {
                every { targetPoseQueryRepository.findDetail(targetPoseIdentity) } returns
                    TargetPoseDetailView(
                        targetPoseId = 1L,
                        name = "낙타자세",
                        imageAssetKey = "camel-pose",
                        bodyPartCode = "BACK",
                        level = 2,
                        muscles = emptyList(),
                    )

                targetPoseQueryService.getDetail(targetPoseIdentity).level shouldBe 2
            }

            it("없는 자세면 TargetPoseNotFoundException 이다") {
                every { targetPoseQueryRepository.findDetail(targetPoseIdentity) } returns null

                shouldThrow<TargetPoseNotFoundException> {
                    targetPoseQueryService.getDetail(targetPoseIdentity)
                }
            }
        }

        describe("findDetail") {
            it("없는 자세면 예외가 아니라 null 이다") {
                every { targetPoseQueryRepository.findDetail(targetPoseIdentity) } returns null

                targetPoseQueryService.findDetail(targetPoseIdentity) shouldBe null
            }
        }

        describe("getAllByBodyPartCode") {
            it("부위의 자세를 돌려준다") {
                every { targetPoseQueryRepository.findAllByBodyPartCode("BACK") } returns
                    listOf(
                        TargetPoseSummaryView(1L, "업독", "upward-facing-dog-pose", "BACK", 1),
                        TargetPoseSummaryView(2L, "낙타자세", "camel-pose", "BACK", 2),
                    )

                targetPoseQueryService.getAllByBodyPartCode("BACK").map { it.level } shouldBe listOf(1, 2)
            }

            /**
             * bodyPartCode 는 screening 소유 어휘라 catalog 가 값 집합을 검증하지 않는다.
             */
            it("자세가 없는 부위면 빈 목록이고 예외가 아니다") {
                every { targetPoseQueryRepository.findAllByBodyPartCode("NECK") } returns emptyList()

                targetPoseQueryService.getAllByBodyPartCode("NECK") shouldBe emptyList()
            }
        }
    })
