package team.aligner.member.adapter.auth

import team.aligner.member.contract.KakaoMemberCommand
import team.aligner.member.contract.MemberAuthContract
import team.aligner.support.web.AuthMemberPort
import team.aligner.support.web.AuthenticatedMember
import team.aligner.support.web.KakaoLoginCommand

/**
 * 웹 인증 port 를 회원 도메인 계약에 연결한다.
 *
 * 이 어댑터가 있어야 support-web 이 member:service·member:model 을 직접 의존하지 않는다
 * (docs/architecture.md §9). 변환만 하고 판단을 두지 않는다.
 */
internal class AuthMemberAdapter(
    private val memberAuthContract: MemberAuthContract,
) : AuthMemberPort {
    override fun findOrRegisterByKakao(command: KakaoLoginCommand): AuthenticatedMember {
        val response =
            memberAuthContract.findOrRegisterByKakao(
                KakaoMemberCommand(
                    kakaoId = command.kakaoId,
                    nickname = command.nickname,
                    profileImageUrl = command.profileImageUrl,
                ),
            )
        return AuthenticatedMember(memberId = response.memberId)
    }
}
