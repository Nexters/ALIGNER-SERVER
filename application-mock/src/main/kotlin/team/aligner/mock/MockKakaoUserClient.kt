package team.aligner.mock

import team.aligner.support.web.auth.KakaoUser
import team.aligner.support.web.auth.KakaoUserClient

/**
 * **카카오를 치지 않는다.** 어떤 인가 코드로도 같은 사용자를 돌려준다.
 *
 * 이 하나만 갈아끼우면 `support-web` 의 **실제 `KakaoAuthController` 가 그대로 돈다** —
 * 토큰 발급도, 이후 요청의 검증도 진짜 코드다. 목이 로그인 컨트롤러를 따로 만들면 그만큼
 * 계약이 갈라질 자리가 생기므로 바깥 경계 하나만 끊는다.
 *
 * 프론트가 실제 카카오 로그인 왕복까지 확인하고 싶어지면 이 Bean 만 빼면 된다. 그때는
 * 카카오 앱 키가 필요하다.
 */
internal class MockKakaoUserClient : KakaoUserClient {
    override fun fetchUserByAuthorizationCode(authorizationCode: String): KakaoUser =
        KakaoUser(
            kakaoId = "mock-kakao-id",
            nickname = "요가하는 사람",
            profileImageUrl = null,
        )
}
