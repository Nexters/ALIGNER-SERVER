package team.aligner.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import team.aligner.support.web.auth.JwtTokenProvider

@Testcontainers
@SpringBootTest(
    classes = [AlignerApplication::class, TracingProbeController::class],
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

    @Autowired
    private lateinit var restClientBuilder: RestClient.Builder

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
    fun `실제 요청 처리 내부에서 32자리 traceId 와 16자리 spanId 가 생성되며 X-Request-ID 와 완벽히 일치한다`() {
        val token = jwtTokenProvider.issue(memberId = 42L).accessToken

        val response =
            rest
                .get()
                .uri("/test/tracing/probe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .toEntity(object : ParameterizedTypeReference<Map<String, String>>() {})

        response.statusCode.value() shouldBe 200
        val body = response.body!!
        val mdcTraceId = body["traceId"]
        val mdcSpanId = body["spanId"]
        val mdcMemberId = body["memberId"]
        val headerTraceId = response.headers.getFirst("X-Request-ID")

        mdcTraceId shouldMatch "^[0-9a-f]{32}$".toRegex()
        mdcSpanId shouldMatch "^[0-9a-f]{16}$".toRegex()
        mdcMemberId shouldBe "42"
        headerTraceId shouldBe mdcTraceId
    }

    @Test
    fun `인증된 API 요청 시에도 유효한 32자리 traceId 가 X-Request-ID 로 반환된다`() {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken

        val (statusCode, traceId) =
            rest
                .get()
                .uri("/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange { _, clientResponse ->
                    clientResponse.statusCode.value() to clientResponse.headers.getFirst("X-Request-ID")
                }

        // DB 에 회원이 존재하지 않으므로 404 가 반환되나, 인증 통과 후 정상적인 TraceId 가 생성되어 반환됨
        statusCode shouldBe 404
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

    @Test
    fun `인증된 사용자의 존재하지 않는 경로(404) 요청 시에도 X-Request-ID 헤더가 정상적으로 반환된다`() {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken

        val (statusCode, traceId) =
            rest
                .get()
                .uri("/non-existent-api-path")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange { _, clientResponse ->
                    clientResponse.statusCode.value() to clientResponse.headers.getFirst("X-Request-ID")
                }

        statusCode shouldBe 404
        traceId shouldMatch "^[0-9a-f]{32}$".toRegex()
    }

    @Test
    fun `서버 내부 예외(5xx) 발생 시에도 X-Request-ID 가 응답 헤더에 보존된다`() {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken

        val (statusCode, traceId) =
            rest
                .get()
                .uri("/test/tracing/error-500")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange { _, clientResponse ->
                    clientResponse.statusCode.value() to clientResponse.headers.getFirst("X-Request-ID")
                }

        statusCode shouldBe 500
        traceId shouldMatch "^[0-9a-f]{32}$".toRegex()
    }

    @Test
    fun `외부에서 주입된 W3C traceparent 헤더가 전달되면 해당 traceId 를 이어받는다`() {
        val parentTraceId = "4bf92f3577b34da6a3ce929d0e0e4736"
        val parentSpanId = "00f067aa0ba902b7"
        val incomingTraceparent = "00-$parentTraceId-$parentSpanId-01"

        val response =
            rest
                .get()
                .uri("/actuator/health")
                .header("traceparent", incomingTraceparent)
                .retrieve()
                .toBodilessEntity()

        response.statusCode.value() shouldBe 200
        val responseTraceId = response.headers.getFirst("X-Request-ID")
        responseTraceId shouldBe parentTraceId
    }

    @Test
    fun `클라이언트가 보낸 임의의 X-Request-ID 는 traceId 를 덮어쓰지 않고 무시된다`() {
        val arbitraryClientId = "arbitrary-custom-uuid-99999"

        val response =
            rest
                .get()
                .uri("/actuator/health")
                .header("X-Request-ID", arbitraryClientId)
                .retrieve()
                .toBodilessEntity()

        val responseTraceId = response.headers.getFirst("X-Request-ID")
        responseTraceId shouldMatch "^[0-9a-f]{32}$".toRegex()
        responseTraceId shouldNotBe arbitraryClientId
    }

    @Test
    fun `Spring Boot auto-configured RestClient 는 아웃바운드 호출 시 traceparent 헤더를 자동 전파한다`() {
        val builder = restClientBuilder.clone()
        val mockServer = MockRestServiceServer.bindTo(builder).build()

        mockServer
            .expect(requestTo("https://api.external.com/test"))
            .andExpect(header("traceparent", org.hamcrest.Matchers.matchesRegex("^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON))

        val client = builder.baseUrl("https://api.external.com").build()
        client
            .get()
            .uri("/test")
            .retrieve()
            .toBodilessEntity()

        mockServer.verify()
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}

@RestController
class TracingProbeController {
    @GetMapping("/test/tracing/probe")
    fun probe(): Map<String, String> =
        mapOf(
            "traceId" to (MDC.get("traceId") ?: ""),
            "spanId" to (MDC.get("spanId") ?: ""),
            "memberId" to (MDC.get("memberId") ?: ""),
        )

    @GetMapping("/test/tracing/error-500")
    fun error(): Map<String, String> = throw IllegalStateException("Forced 500 error for integration testing")
}
