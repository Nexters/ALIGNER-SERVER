package team.aligner.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import team.aligner.support.web.auth.JwtTokenProvider

@Testcontainers
@SpringBootTest(
    classes = [AlignerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestPropertySource(
    properties = [
        "aligner.auth.jwt.secret=dummy-secret-for-integration-test-32bytes",
        "aligner.auth.kakao.client-id=dummy-client-id",
        "aligner.auth.kakao.client-secret=dummy-client-secret",
        "aligner.ymove.api-key=dummy-api-key",
    ],
)
class TracingCorrelationIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val rest: RestClient by lazy { RestClient.create("http://localhost:$port") }

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    fun `실제 런타임에서 Spring Boot Observation 이 traceId 를 생성하고 X-Request-ID 로 반환된다`() {
        val response =
            rest
                .get()
                .uri("/actuator/health")
                .retrieve()
                .toBodilessEntity()

        response.statusCode.value() shouldBe 200
        val traceId = response.headers.getFirst("X-Request-ID")
        traceId shouldMatch "^[0-9a-f]{32}$".toRegex()
    }

    @Test
    fun `인증된 API 요청 시에도 유효한 32자리 traceId 가 X-Request-ID 로 반환된다`() {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken

        val (_, traceId) =
            rest
                .get()
                .uri("/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange { _, clientResponse ->
                    clientResponse.statusCode.value() to clientResponse.headers.getFirst("X-Request-ID")
                }

        traceId shouldMatch "^[0-9a-f]{32}$".toRegex()
    }

    @Test
    fun `인증 실패(401) 요청 시에도 X-Request-ID 헤더가 정상적으로 반환된다`() {
        val (statusCode, traceId) =
            rest
                .get()
                .uri("/members/me")
                .exchange { _, clientResponse ->
                    clientResponse.statusCode.value() to clientResponse.headers.getFirst("X-Request-ID")
                }

        statusCode shouldBe 401
        traceId shouldMatch "^[0-9a-f]{32}$".toRegex()
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}
