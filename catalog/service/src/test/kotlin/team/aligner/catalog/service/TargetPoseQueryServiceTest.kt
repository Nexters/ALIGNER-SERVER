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

        describe("getAll") {
            it("부위를 주면 그 부위의 자세를 돌려준다") {
                every { targetPoseQueryRepository.findAll("BACK") } returns
                    listOf(
                        TargetPoseSummaryView(1L, "업독", "upward-facing-dog-pose", "BACK", 1),
                        TargetPoseSummaryView(2L, "낙타자세", "camel-pose", "BACK", 2),
                    )

                targetPoseQueryService.getAll("BACK").map { it.level } shouldBe listOf(1, 2)
            }

            /**
             * 온보딩 그리드가 부위를 먼저 묻지 않고 핀포즈 전체를 펼친다 (docs/domains.md §4-2).
             * null 을 그대로 port 에 넘겨야 전체 조회가 성립한다.
             */
            it("부위를 생략하면 null 을 그대로 넘겨 전체를 돌려준다") {
                every { targetPoseQueryRepository.findAll(null) } returns
                    listOf(
                        TargetPoseSummaryView(1L, "낙타자세", "camel-pose", "BACK", 2),
                        TargetPoseSummaryView(2L, "보트자세", "boat-pose", "ABDOMEN", 1),
                    )

                targetPoseQueryService.getAll(null).map { it.bodyPartCode } shouldBe listOf("BACK", "ABDOMEN")
            }

            /**
             * bodyPartCode 는 screening 소유 어휘라 catalog 가 값 집합을 검증하지 않는다.
             */
            it("자세가 없는 부위면 빈 목록이고 예외가 아니다") {
                every { targetPoseQueryRepository.findAll("NECK") } returns emptyList()

                targetPoseQueryService.getAll("NECK") shouldBe emptyList()
            }
        }
    })
