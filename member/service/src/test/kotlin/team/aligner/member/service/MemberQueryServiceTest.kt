package team.aligner.member.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.exception.MemberNotFoundException
import team.aligner.member.model.view.MemberProfileView

class MemberQueryServiceTest :
    DescribeSpec({
        val memberQueryRepository = mockk<MemberQueryRepository>()
        val memberQueryService: MemberQueryService = MemberQueryServiceImpl(memberQueryRepository)

        val memberIdentity = MemberIdentity.of(1L)

        describe("getProfile") {
            it("프로필을 돌려준다") {
                every { memberQueryRepository.findProfile(memberIdentity) } returns
                    profileView(nickname = "강혁", heightCm = 170, weightKg = 60)

                val profile = memberQueryService.getProfile(memberIdentity)

                profile.memberId shouldBe 1L
                profile.nickname shouldBe "강혁"
                profile.heightCm shouldBe 170
                profile.weightKg shouldBe 60
            }

            /**
             * 온보딩을 끝내지 않은 회원이다. 프론트가 이 null 들을 보고 온보딩으로 보낸다.
             */
            it("온보딩 전이면 신체 정보와 강화 설정이 전부 null 이다") {
                every { memberQueryRepository.findProfile(memberIdentity) } returns profileView()

                val profile = memberQueryService.getProfile(memberIdentity)

                profile.heightCm shouldBe null
                profile.weightKg shouldBe null
                profile.experienceLevel shouldBe null
                profile.reinforcementBodyPartCode shouldBe null
                profile.reinforcementLevel shouldBe null
            }

            it("닉네임이 비어 있어도 그대로 돌려준다") {
                every { memberQueryRepository.findProfile(memberIdentity) } returns
                    profileView(nickname = null)

                memberQueryService.getProfile(memberIdentity).nickname shouldBe null
            }

            it("없는 회원이면 MemberNotFoundException 이다") {
                every { memberQueryRepository.findProfile(memberIdentity) } returns null

                shouldThrow<MemberNotFoundException> {
                    memberQueryService.getProfile(memberIdentity)
                }
            }
        }
    }) {
    private companion object {
        fun profileView(
            nickname: String? = "강혁",
            heightCm: Int? = null,
            weightKg: Int? = null,
        ) = MemberProfileView(
            memberId = 1L,
            nickname = nickname,
            profileImageUrl = null,
            heightCm = heightCm,
            weightKg = weightKg,
            experienceLevel = null,
            reinforcementBodyPartCode = null,
            reinforcementLevel = null,
        )
    }
}
