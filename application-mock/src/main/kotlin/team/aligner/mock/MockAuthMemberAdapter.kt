package team.aligner.mock

import team.aligner.support.web.AuthMemberPort
import team.aligner.support.web.AuthenticatedMember
import team.aligner.support.web.KakaoLoginCommand

/**
 * 회원 조회·가입만 고정으로 대신한다. **인증 자체는 진짜다.**
 *
 * 카카오 왕복도, JWT 발급·검증도 support-web 의 실제 코드가 그대로 돈다. DB 가 없어
 * 회원을 저장할 수 없으므로 이 자리만 갈아끼운다 — `member:adapter-auth` 를 대신한다
 * (docs/architecture.md §9).
 *
 * 어떤 카카오 계정으로 로그인해도 같은 회원이 된다. 목에서 회원을 구분할 이유가 없다.
 */
internal class MockAuthMemberAdapter : AuthMemberPort {
    override fun findOrRegisterByKakao(command: KakaoLoginCommand): AuthenticatedMember =
        AuthenticatedMember(memberId = MockFixtures.MEMBER_ID)
}
