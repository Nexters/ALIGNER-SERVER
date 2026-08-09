package team.aligner.member.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.member.infrastructure.MemberRepository
import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.exception.MemberNotFoundException
import java.time.Instant

interface MemberCommandService {
    fun findOrRegisterByKakao(command: RegisterKakaoMemberCommand): Member

    fun updateProfile(
        memberIdentity: MemberIdentity,
        command: UpdateMemberProfileCommand,
    ): Member

    fun withdraw(memberIdentity: MemberIdentity)
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
     *
     * 탈퇴한 회원은 조회되지 않으므로(MemberJdbcRepository) 같은 카카오 계정이 다시 가입하면
     * 새 회원이 만들어진다. 이전 기록은 이어지지 않는다 (Member.withdraw).
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
        val member = findActive(memberIdentity)
        return memberRepository.save(
            member.changeProfile(
                nickname = command.nickname,
                heightCm = command.heightCm,
                weightKg = command.weightKg,
                experienceLevel = command.experienceLevel,
                reinforcement = command.reinforcement,
            ),
        )
    }

    /**
     * 탈퇴. 행을 지우지 않고 카카오 식별자만 지운다 (Member.withdraw).
     *
     * 이미 탈퇴한 회원은 조회되지 않아 404 다. 같은 요청을 두 번 보내면 두 번째가 404 인데,
     * 첫 요청으로 이미 목적이 달성된 상태라 화면은 성공과 같게 다뤄도 된다.
     *
     * 시각을 여기서 만들어 넘긴다. 도메인이 시계를 직접 읽으면 테스트에서 고정할 수 없다.
     */
    override fun withdraw(memberIdentity: MemberIdentity) {
        val member = findActive(memberIdentity)
        memberRepository.save(member.withdraw(at = Instant.now()))
    }

    private fun findActive(memberIdentity: MemberIdentity): Member =
        memberRepository.findByMemberIdentity(memberIdentity)
            ?: throw MemberNotFoundException()
}
