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
        /** 인가 코드를 액세스 토큰으로 바꾸는 곳. 카카오 인증 서버(kauth)라 사용자 API(kapi)와 호스트가 다르다. */
        val tokenUri: String,
        val userInfoUri: String,
        /** 앱 REST API 키. JavaScript 키가 아니다. 프론트에 두면 안 되므로 환경변수로만 넣는다. */
        val clientId: String,
        /**
         * 카카오 콘솔에서 클라이언트 시크릿은 기본 활성화라 사실상 필수다.
         * nullable 로 두지 않는 이유는, 빠뜨렸을 때 조용히 통과했다가 카카오에서 401 이 오면
         * 원인을 "인가 코드가 만료됐나" 로 반대로 짚게 되기 때문이다. 환경변수로만 넣는다.
         */
        val clientSecret: String,
        /**
         * 프론트의 인가 코드 착지 라우트. **`Kakao.Auth.authorize()` 에 넘긴 값과 완전히 일치해야 한다.**
         * 서버가 직접 리다이렉트를 받지는 않지만, 토큰 교환 요청에 같은 값을 실어야 카카오가 받아준다.
         */
        val redirectUri: String,
        val connectTimeoutMillis: Long,
        val readTimeoutMillis: Long,
    )
}
