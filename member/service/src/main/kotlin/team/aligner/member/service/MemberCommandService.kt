package team.aligner.member.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.member.infrastructure.MemberRepository
import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.exception.MemberNotFoundException

interface MemberCommandService {
    fun findOrRegisterByKakao(command: RegisterKakaoMemberCommand): Member

    fun updateProfile(
        memberIdentity: MemberIdentity,
        command: UpdateMemberProfileCommand,
    ): Member
}

/**
 * `@Transactional` 은 **클래스에** 붙인다. kotlin-spring(allopen)이 클래스에 붙은 어노테이션만
 * 보고 open 을 매기기 때문이다. 메서드에만 붙이면 클래스가 final 로 남고, Boot 의
 * proxy-target-class 기본값(true)이 CGLIB 을 고르므로 Bean 생성 시점에
 * "Could not generate CGLIB subclass" 로 기동이 실패한다.
 *
 * 빌드로는 드러나지 않는다. 단위 테스트는 impl 을 직접 생성하고 통합 테스트는 이 모듈을
 * 로딩하지 않는다.
 */
@Transactional
internal class MemberCommandServiceImpl(
    private val memberRepository: MemberRepository,
) : MemberCommandService {
    /**
     * 이미 가입한 카카오 계정이면 저장하지 않고 그대로 돌려준다.
     *
     * 재로그인마다 카카오 닉네임·프로필 이미지로 덮어쓰지 않는다. 회원이 우리 서비스에서
     * 바꾼 닉네임이 다음 로그인에 되돌아가면 안 되기 때문이다.
     */
    override fun findOrRegisterByKakao(command: RegisterKakaoMemberCommand): Member =
        memberRepository.findByKakaoId(command.kakaoId)
            ?: memberRepository.save(
                Member.register(
                    kakaoId = command.kakaoId,
                    nickname = command.nickname,
                    profileImageUrl = command.profileImageUrl,
                ),
            )

    override fun updateProfile(
        memberIdentity: MemberIdentity,
        command: UpdateMemberProfileCommand,
    ): Member {
        val member =
            memberRepository.findByMemberIdentity(memberIdentity)
                ?: throw MemberNotFoundException()
        return memberRepository.save(member.changeProfile(command.nickname))
    }
}
