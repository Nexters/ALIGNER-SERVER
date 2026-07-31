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
                    MemberProfileView(memberId = 1L, nickname = "강혁", profileImageUrl = null)

                val profile = memberQueryService.getProfile(memberIdentity)

                profile.memberId shouldBe 1L
                profile.nickname shouldBe "강혁"
            }

            it("닉네임이 비어 있어도 그대로 돌려준다") {
                every { memberQueryRepository.findProfile(memberIdentity) } returns
                    MemberProfileView(memberId = 1L, nickname = null, profileImageUrl = null)

                memberQueryService.getProfile(memberIdentity).nickname shouldBe null
            }

            it("없는 회원이면 MemberNotFoundException 이다") {
                every { memberQueryRepository.findProfile(memberIdentity) } returns null

                shouldThrow<MemberNotFoundException> {
                    memberQueryService.getProfile(memberIdentity)
                }
            }
        }
    })
