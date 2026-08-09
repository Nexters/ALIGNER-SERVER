package team.aligner.member.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.view.MemberProfileView

/**
 * course 가 칼로리 계산에 쓰는 계약이다 (docs/domains.md §3).
 */
class MemberBodyContractImplTest :
    DescribeSpec({
        val memberQueryService = mockk<MemberQueryService>()
        val memberBodyContract = MemberBodyContractImpl(memberQueryService)

        val memberIdentity = MemberIdentity.of(1L)

        describe("findBody") {
            it("몸무게를 돌려준다") {
                every { memberQueryService.findProfile(memberIdentity) } returns profileView(weightKg = 60)

                val body = memberBodyContract.findBody(1L).let(::requireNotNull)

                body.memberId shouldBe 1L
                body.weightKg shouldBe 60
            }

            /**
             * 온보딩에서 몸무게를 아직 받지 않은 회원이다. 0 으로 바꾸지 않는다 —
             * 0 kcal("운동량 없음")과 "모름"은 화면에서 다르게 그려야 한다.
             */
            it("몸무게가 없으면 0 이 아니라 null 이다") {
                every { memberQueryService.findProfile(memberIdentity) } returns profileView(weightKg = null)

                memberBodyContract
                    .findBody(1L)
                    .let(::requireNotNull)
                    .weightKg
                    .shouldBeNull()
            }

            /**
             * 탈퇴한 회원은 조회에서 걸러진다. 예외가 아니라 null 이어야 호출부가 흐름 제어에
             * 예외를 쓰지 않는다.
             */
            it("없거나 탈퇴한 회원이면 예외가 아니라 null 이다") {
                every { memberQueryService.findProfile(memberIdentity) } returns null

                memberBodyContract.findBody(1L).shouldBeNull()
            }
        }
    }) {
    private companion object {
        fun profileView(weightKg: Int?) =
            MemberProfileView(
                memberId = 1L,
                nickname = "강혁",
                profileImageUrl = null,
                heightCm = 170,
                weightKg = weightKg,
                experienceLevel = null,
                reinforcementBodyPartCode = null,
                reinforcementLevel = null,
            )
    }
}
