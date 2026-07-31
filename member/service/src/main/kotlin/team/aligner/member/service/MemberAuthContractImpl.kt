package team.aligner.member.service

import team.aligner.member.contract.AuthenticatedMemberResponse
import team.aligner.member.contract.KakaoMemberCommand
import team.aligner.member.contract.MemberAuthContract

/**
 * 계약 구현체. internal 로 대상 도메인 service 에 둔다 (docs/architecture.md §7).
 *
 * 위임만 하고 로직을 두지 않는다. 여기에 판단이 생기면 계약 소비자마다 다른 동작을 하게 된다.
 */
internal class MemberAuthContractImpl(
    private val memberCommandService: MemberCommandService,
) : MemberAuthContract {
    override fun findOrRegisterByKakao(command: KakaoMemberCommand): AuthenticatedMemberResponse {
        val member =
            memberCommandService.findOrRegisterByKakao(
                RegisterKakaoMemberCommand(
                    kakaoId = command.kakaoId,
                    nickname = command.nickname,
                    profileImageUrl = command.profileImageUrl,
                ),
            )
        val memberIdentity =
            requireNotNull(member.memberIdentity) {
                "저장된 회원에 식별자가 없다"
            }
        return AuthenticatedMemberResponse(memberId = memberIdentity.value)
    }
}
