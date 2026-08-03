package team.aligner.support.web.auth

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * ComponentScan 이 없으므로 @EnableConfigurationProperties 로 등록해야 바인딩된다.
 * 빠지면 기동은 성공하고 로그인만 죽는다 (SupportWebAutoConfiguration 참고).
 */
@ConfigurationProperties(prefix = "aligner.auth")
data class AuthProperties(
    val jwt: Jwt,
    val kakao: Kakao,
) {
    data class Jwt(
        /** HS256 키다. 32 바이트 미만이면 기동 시 예외가 난다. 환경변수로만 넣는다. */
        val secret: String,
        val expirationSeconds: Long,
        val issuer: String,
    )

    data class Kakao(
        val userInfoUri: String,
        val connectTimeoutMillis: Long,
        val readTimeoutMillis: Long,
    )
}
