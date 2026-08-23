package team.aligner.mock

import team.aligner.support.web.AuthMemberPort
import team.aligner.support.web.AuthenticatedMember
import team.aligner.support.web.KakaoLoginCommand

/**
 * **현재 Bean 으로 등록하지 않는다.** 이 port 를 요구하던 실제 `KakaoAuthController` 를 목이
 * 쓰지 않기 때문이다 (MockAuthController 가 카카오를 치지 않고 토큰만 낸다).
 *
 * 지우지 않고 남기는 이유는, 프론트가 **실제 카카오 로그인 왕복까지 확인**하고 싶어질 때
 * `SupportWebAutoConfiguration` 의 exclude 를 풀고 이것만 Bean 으로 올리면 되기 때문이다.
 * 그때는 카카오 앱 키가 필요하다.
 */
internal class MockAuthMemberAdapter : AuthMemberPort {
    override fun findOrRegisterByKakao(command: KakaoLoginCommand): AuthenticatedMember =
        AuthenticatedMember(memberId = MockFixtures.MEMBER_ID)
}
