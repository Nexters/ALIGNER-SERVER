package team.aligner.support.web.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class NimbusJwtTokenProviderTest :
    DescribeSpec({
        fun properties(
            secret: String = TEST_SIGNING_KEY,
            expirationSeconds: Long = 3600,
        ) = AuthProperties(
            jwt = AuthProperties.Jwt(secret = secret, expirationSeconds = expirationSeconds, issuer = "aligner"),
            kakao =
                AuthProperties.Kakao(
                    userInfoUri = "https://kapi.kakao.com/v2/user/me",
                    connectTimeoutMillis = 2000,
                    readTimeoutMillis = 3000,
                ),
        )

        describe("issue / parseMemberId") {
            it("발급한 토큰에서 memberId 를 되읽는다") {
                val provider = NimbusJwtTokenProvider(properties())

                val issued = provider.issue(42L)

                issued.expiresIn shouldBe 3600
                provider.parseMemberId(issued.accessToken) shouldBe 42L
            }

            it("다른 시크릿으로 서명된 토큰은 받지 않는다") {
                val issuer = NimbusJwtTokenProvider(properties(secret = TEST_SIGNING_KEY))
                val verifier = NimbusJwtTokenProvider(properties(secret = OTHER_SIGNING_KEY))

                val issued = issuer.issue(42L)

                verifier.parseMemberId(issued.accessToken) shouldBe null
            }

            it("만료된 토큰은 받지 않는다") {
                // 2 시간 전에 1 시간짜리로 발급하면 1 시간 전에 만료된 토큰이 된다.
                // NimbusJwtDecoder 의 기본 시계 오차 허용이 60 초라 그보다 넉넉히 벌린다.
                val twoHoursAgo = Clock.fixed(Instant.now().minusSeconds(7200), ZoneOffset.UTC)
                val issuer = NimbusJwtTokenProvider(properties(expirationSeconds = 3600), twoHoursAgo)
                val verifier = NimbusJwtTokenProvider(properties(expirationSeconds = 3600))

                val issued = issuer.issue(42L)

                verifier.parseMemberId(issued.accessToken) shouldBe null
            }

            it("본문이 변조된 토큰은 받지 않는다") {
                val provider = NimbusJwtTokenProvider(properties())
                val issued = provider.issue(42L)

                // payload 한 글자만 바꿔도 서명이 맞지 않는다.
                val parts = issued.accessToken.split(".")
                val tampered = parts[0] + "." + parts[1].dropLast(1) + "X." + parts[2]

                tampered shouldNotBe issued.accessToken
                provider.parseMemberId(tampered) shouldBe null
            }

            it("JWT 형식이 아닌 문자열도 예외 없이 null 이다") {
                val provider = NimbusJwtTokenProvider(properties())

                provider.parseMemberId("이건토큰이아니다") shouldBe null
            }

            it("HS256 미만 길이의 시크릿이면 생성 시점에 막는다") {
                // 이 검사가 없으면 기동은 성공하고 첫 로그인에서 500 이 난다.
                shouldThrow<IllegalArgumentException> {
                    NimbusJwtTokenProvider(properties(secret = "짧은키"))
                }
            }
        }
    }) {
    private companion object {
        // HS256 이라 32 바이트 이상이어야 한다.
        const val TEST_SIGNING_KEY = "aligner-test-signing-1234567890-abcdef"
        const val OTHER_SIGNING_KEY = "aligner-other-signing-0987654321-fedcba"
    }
}
