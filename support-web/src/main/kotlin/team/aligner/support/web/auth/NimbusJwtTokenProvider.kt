package team.aligner.support.web.auth

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Clock
import javax.crypto.spec.SecretKeySpec

/**
 * HS256 대칭키 서명. 발급자와 검증자가 같은 서버라 비대칭키를 쓸 이유가 없다.
 *
 * secret 이 32 바이트 미만이면 생성자에서 예외가 난다. 기동 시점에 드러나는 편이
 * 첫 로그인 요청에서 드러나는 것보다 낫다.
 */
internal class NimbusJwtTokenProvider(
    private val properties: AuthProperties,
    // 만료 동작을 테스트에서 확인하려면 발급 시각을 옮길 수 있어야 한다. 음수 만료로는
    // 만료 토큰을 만들 수 없다 — 인코더가 expiresAt < issuedAt 을 거부한다.
    private val clock: Clock = Clock.systemUTC(),
) : JwtTokenProvider {
    private val secretKey =
        SecretKeySpec(properties.jwt.secret.toByteArray(), MAC_ALGORITHM).also {
            // Nimbus 의 MACSigner 는 encode() 시점에 lazy 생성돼서, 이 검사가 없으면 짧은 키로도
            // 기동이 성공하고 첫 로그인에서 500 이 난다.
            require(it.encoded.size >= MIN_SECRET_BYTES) {
                "aligner.auth.jwt.secret 은 HS256 이라 ${MIN_SECRET_BYTES}바이트 이상이어야 한다"
            }
        }

    private val encoder = NimbusJwtEncoder(ImmutableSecret(secretKey))

    private val decoder =
        NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

    override fun issue(memberId: Long): IssuedToken {
        val issuedAt = clock.instant()
        val expiresAt = issuedAt.plusSeconds(properties.jwt.expirationSeconds)
        val claims =
            JwtClaimsSet
                .builder()
                .issuer(properties.jwt.issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(memberId.toString())
                .build()

        val token =
            encoder
                .encode(
                    JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims,
                    ),
                ).tokenValue

        return IssuedToken(accessToken = token, expiresIn = properties.jwt.expirationSeconds)
    }

    override fun parseMemberId(token: String): Long? =
        try {
            // 서명·만료 검증은 decoder 가 한다. sub 가 숫자가 아니면 우리가 발급한 토큰이 아니다.
            decoder.decode(token).subject?.toLongOrNull()
        } catch (exception: JwtException) {
            null
        }

    private companion object {
        const val MAC_ALGORITHM = "HmacSHA256"
        const val MIN_SECRET_BYTES = 32
    }
}
