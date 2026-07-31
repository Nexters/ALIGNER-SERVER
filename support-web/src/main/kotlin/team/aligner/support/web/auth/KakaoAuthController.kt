package team.aligner.support.web.auth

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import team.aligner.support.web.AuthMemberPort
import team.aligner.support.web.KakaoLoginCommand

/**
 * 카카오 로그인 진입점. 이 컨트롤러만 인증 없이 열려 있다 (SecurityConfig).
 *
 * 클라이언트가 카카오 SDK 로 받은 액세스 토큰을 넘기면, 서버가 카카오에 확인한 뒤 회원을
 * 찾거나 만들고 자체 JWT 를 발급한다. 서버가 인가 코드 리다이렉트를 받는 방식은 쓰지 않는다.
 *
 * **AuthMemberPort 를 요구하는 유일한 Bean 이다.** member:adapter-auth 가 조립에서 빠지면
 * 여기서 기동이 실패해야 정상이다 (docs/architecture.md §9).
 */
@RestController
class KakaoAuthController(
    private val kakaoUserClient: KakaoUserClient,
    private val authMemberPort: AuthMemberPort,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @PostMapping("/auth/kakao")
    fun login(
        @RequestBody request: KakaoLoginRequest,
    ): KakaoLoginResponse {
        val kakaoUser = kakaoUserClient.fetchUser(request.kakaoAccessToken)

        val member =
            authMemberPort.findOrRegisterByKakao(
                KakaoLoginCommand(
                    kakaoId = kakaoUser.kakaoId,
                    nickname = kakaoUser.nickname,
                    profileImageUrl = kakaoUser.profileImageUrl,
                ),
            )

        val issued = jwtTokenProvider.issue(member.memberId)
        return KakaoLoginResponse(
            accessToken = issued.accessToken,
            expiresIn = issued.expiresIn,
        )
    }
}

data class KakaoLoginRequest(
    val kakaoAccessToken: String,
)

data class KakaoLoginResponse(
    val accessToken: String,
    val expiresIn: Long,
)
