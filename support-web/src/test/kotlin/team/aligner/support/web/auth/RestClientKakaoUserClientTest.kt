package team.aligner.support.web.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

/**
 * 인가 코드 플로우의 두 단계(교환 → 조회)를 MockRestServiceServer 로 검증한다 (이슈 #12).
 *
 * 실제 카카오를 치지 않으면서 **요청 본문과 헤더까지** 본다. 교환 요청의 파라미터 이름이 하나라도
 * 틀리면 카카오가 400 을 주는데, 그건 배포하고 나서야 드러나는 종류의 실수다.
 */
class RestClientKakaoUserClientTest :
    DescribeSpec({
        /**
         * MockRestServiceServer 는 빌더에 자기 requestFactory 를 심는다. 클라이언트가 빌더를 다시
         * 만지면 그게 덮여서 목이 동작하지 않는다 — 그래서 조립된 RestClient 를 받도록 했다.
         */
        fun fixture(): Pair<RestClientKakaoUserClient, MockRestServiceServer> {
            val builder = RestClient.builder()
            val server = MockRestServiceServer.bindTo(builder).build()
            val properties =
                AuthProperties(
                    jwt = AuthProperties.Jwt(secret = "x".repeat(32), expirationSeconds = 3600, issuer = "aligner"),
                    kakao = kakaoProperties(),
                )
            return RestClientKakaoUserClient(properties, builder.build()) to server
        }

        /** 카카오 문서의 필수 파라미터 4 개 + 클라이언트 시크릿 활성화 시 필수인 1 개. */
        fun expectedForm(code: String) =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", TEST_CLIENT_ID)
                add("redirect_uri", TEST_REDIRECT_URI)
                add("code", code)
                add("client_secret", TEST_CLIENT_SECRET)
            }

        fun MockRestServiceServer.respondWithToken(accessToken: String = "kakao-access-token") =
            expect(requestTo(TEST_TOKEN_URI))
                .andRespond(withSuccess("""{"access_token":"$accessToken"}""", MediaType.APPLICATION_JSON))

        describe("fetchUserByAuthorizationCode") {
            it("인가 코드를 토큰으로 교환한 뒤 그 토큰으로 사용자를 읽는다") {
                val (client, server) = fixture()

                server
                    .expect(requestTo(TEST_TOKEN_URI))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(content().formData(expectedForm("test-auth-code")))
                    .andRespond(
                        withSuccess(
                            """{"access_token":"kakao-access-token","refresh_token":"버려진다"}""",
                            MediaType.APPLICATION_JSON,
                        ),
                    )

                server
                    .expect(requestTo(TEST_USER_INFO_URI))
                    .andExpect(method(HttpMethod.GET))
                    // 교환으로 받은 토큰이 그대로 실려야 한다.
                    .andExpect(header("Authorization", "Bearer kakao-access-token"))
                    .andRespond(
                        withSuccess(
                            """
                            {"id":1234567890,
                             "kakao_account":{"profile":{"nickname":"강혁","profile_image_url":"https://img/1.png"}}}
                            """.trimIndent(),
                            MediaType.APPLICATION_JSON,
                        ),
                    )

                val user = client.fetchUserByAuthorizationCode("test-auth-code")

                user.kakaoId shouldBe "1234567890"
                user.nickname shouldBe "강혁"
                user.profileImageUrl shouldBe "https://img/1.png"
                server.verify()
            }

            it("프로필 제공에 동의하지 않으면 닉네임과 이미지가 null 이다") {
                val (client, server) = fixture()

                server.respondWithToken()
                server
                    .expect(requestTo(TEST_USER_INFO_URI))
                    .andRespond(withSuccess("""{"id":42}""", MediaType.APPLICATION_JSON))

                val user = client.fetchUserByAuthorizationCode("code")

                user.kakaoId shouldBe "42"
                user.nickname shouldBe null
                user.profileImageUrl shouldBe null
            }

            listOf(401, 400).forEach { status ->
                it("교환이 $status 이면 KAKAO_AUTH_CODE_INVALID 다") {
                    val (client, server) = fixture()

                    server
                        .expect(requestTo(TEST_TOKEN_URI))
                        .andRespond(withStatus(HttpStatusCode.valueOf(status)))

                    val exception =
                        shouldThrow<AuthenticationFailedException> {
                            client.fetchUserByAuthorizationCode("만료됐거나 이미 쓴 코드")
                        }

                    exception.errorCode shouldBe AuthErrorCode.KAKAO_AUTH_CODE_INVALID
                }
            }

            it("교환이 200 인데 access_token 이 없으면 코드 탓으로 돌리지 않는다") {
                val (client, server) = fixture()

                server
                    .expect(requestTo(TEST_TOKEN_URI))
                    .andRespond(withSuccess("""{"token_type":"bearer"}""", MediaType.APPLICATION_JSON))

                val exception =
                    shouldThrow<AuthenticationFailedException> { client.fetchUserByAuthorizationCode("code") }

                // 멀쩡한 코드로 재로그인을 반복하게 만들면 안 된다.
                exception.errorCode shouldBe AuthErrorCode.KAKAO_UNAVAILABLE
            }

            it("교환은 됐는데 사용자 조회가 401 이면 KAKAO_TOKEN_INVALID 다") {
                val (client, server) = fixture()

                server.respondWithToken()
                server
                    .expect(requestTo(TEST_USER_INFO_URI))
                    .andRespond(withStatus(HttpStatusCode.valueOf(401)))

                val exception =
                    shouldThrow<AuthenticationFailedException> { client.fetchUserByAuthorizationCode("code") }

                // 두 실패를 한 코드로 접으면 프론트가 "다시 로그인" 과 "카카오 문제" 를 구분하지 못한다.
                exception.errorCode shouldBe AuthErrorCode.KAKAO_TOKEN_INVALID
            }

            it("카카오가 5xx 면 KAKAO_UNAVAILABLE 이다") {
                val (client, server) = fixture()

                server.expect(requestTo(TEST_TOKEN_URI)).andRespond(withServerError())

                val exception =
                    shouldThrow<AuthenticationFailedException> { client.fetchUserByAuthorizationCode("code") }

                exception.errorCode shouldBe AuthErrorCode.KAKAO_UNAVAILABLE
            }

            it("200 인데 id 가 없으면 필드 경로 문제이므로 401 이 아니다") {
                val (client, server) = fixture()

                server.respondWithToken()
                server
                    .expect(requestTo(TEST_USER_INFO_URI))
                    .andRespond(withSuccess("""{"kakao_account":{}}""", MediaType.APPLICATION_JSON))

                val exception =
                    shouldThrow<AuthenticationFailedException> { client.fetchUserByAuthorizationCode("code") }

                exception.errorCode shouldBe AuthErrorCode.KAKAO_UNAVAILABLE
            }
        }
    })
