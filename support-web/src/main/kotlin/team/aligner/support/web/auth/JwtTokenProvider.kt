package team.aligner.support.web.auth

/**
 * 자체 액세스 토큰 발급·검증.
 *
 * 리프레시 토큰을 두지 않는다. 만료되면 클라이언트가 카카오 SDK 로 토큰을 다시 받아
 * POST /auth/kakao 를 재호출한다 — 리프레시 토큰이 할 일을 카카오가 이미 하고 있다.
 * 그 대가로 발급된 토큰을 서버가 폐기할 수단이 없다.
 *
 * 토큰에는 memberId 원시값만 싣는다. 이 코드가 member 도메인을 몰라야 하기 때문이다.
 */
interface JwtTokenProvider {
    fun issue(memberId: Long): IssuedToken

    /**
     * 유효하지 않으면 null 을 돌려준다.
     *
     * 예외를 던지지 않는 이유는 필터가 토큰 없는 요청과 깨진 토큰을 똑같이 통과시켜야 하기
     * 때문이다. 401 판정은 필터체인이 한다.
     */
    fun parseMemberId(token: String): Long?
}

data class IssuedToken(
    val accessToken: String,
    val expiresIn: Long,
)
