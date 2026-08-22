package team.aligner.support.web.logging

import io.kotest.matchers.string.shouldContain
import jakarta.servlet.Filter
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import team.aligner.support.web.auth.JwtTokenProvider
import team.aligner.support.web.bootstrap.MDC_INSPECT_PATH
import team.aligner.support.web.bootstrap.PROTECTED_PATH
import team.aligner.support.web.bootstrap.SupportWebTestApplication

private const val ALLOWED_ORIGIN = "http://localhost:5173"

@ExtendWith(OutputCaptureExtension::class)
@SpringBootTest(
    classes = [SupportWebTestApplication::class],
    properties = [
        "aligner.web.cors.allowed-origins=$ALLOWED_ORIGIN",
        "aligner.web.cors.max-age-seconds=3600",
        "aligner.auth.jwt.secret=logging-test-signing-key-1234567890-abcdef",
        "aligner.auth.jwt.expiration-seconds=3600",
        "aligner.auth.jwt.issuer=aligner",
        "aligner.auth.kakao.token-uri=https://kauth.kakao.com/oauth/token",
        "aligner.auth.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me",
        "aligner.auth.kakao.client-id=dummy-rest-api-key",
        "aligner.auth.kakao.client-secret=dummy-client-secret",
        "aligner.auth.kakao.redirect-uri=$ALLOWED_ORIGIN/oauth/kakao",
        "aligner.auth.kakao.connect-timeout-millis=2000",
        "aligner.auth.kakao.read-timeout-millis=3000",
    ],
)
class RequestLoggingFilterTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private lateinit var springSecurityFilterChain: Filter

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        val registration = context.getBean("requestLoggingFilter") as FilterRegistrationBean<RequestLoggingFilter>
        val requestLoggingFilter = registration.filter!!
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters<DefaultMockMvcBuilder>(requestLoggingFilter, springSecurityFilterChain)
                .build()
        MDC.clear()
    }

    @Test
    fun `인증된 요청 후 익명 요청 시 memberId MDC leak 이 발생하지 않는다`() {
        val token = jwtTokenProvider.issue(memberId = 99L).accessToken

        mockMvc
            .perform(
                get(MDC_INSPECT_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.memberId").value("99"))

        // 후속 익명 요청 시 401 반환 및 이전 memberId 누수 없음 검증
        mockMvc
            .perform(get(PROTECTED_PATH))
            .andExpect(status().isUnauthorized)

        assertNull(MDC.get("memberId"))
    }

    @Test
    fun `인증된 actuator 요청 후 동일 스레드 익명 요청 시 memberId MDC 누수가 발생하지 않는다`() {
        val token = jwtTokenProvider.issue(memberId = 77L).accessToken

        // 1. 토큰을 싣고 /actuator/health 요청 (로그 suppress 여부와 무관하게 MDC cleanup 이 완주되어야 함)
        mockMvc
            .perform(
                get("/actuator/health")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isNotFound)

        // 2. 후속 익명 요청 시 이전 memberId 가 스레드 풀에 잔류하지 않음을 검증
        mockMvc
            .perform(get(PROTECTED_PATH))
            .andExpect(status().isUnauthorized)

        assertNull(MDC.get("memberId"))
    }

    @Test
    fun `인증된 요청 시 JWT 필터가 memberId 를 MDC 에 넣고 컨트롤러 종료 후 MDC 가 정리된다`() {
        val token = jwtTokenProvider.issue(memberId = 42L).accessToken

        mockMvc
            .perform(
                get(MDC_INSPECT_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.memberId").value("42"))

        // 요청 처리 완료 후 MDC.clear() 가 수행되어 스레드에 잔여 데이터가 남지 않아야 함
        assertNull(MDC.get("memberId"))
    }

    @Test
    fun `MDC 에 traceId 가 존재할 때 200 OK 응답 헤더에 X-Request-ID 가 설정된다`() {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken

        // Micrometer 가 MDC 에 traceId 를 바인딩한 상황을 모사
        MDC.put(RequestLoggingFilter.TRACE_ID, "trace-abc-123")

        mockMvc
            .perform(
                get(PROTECTED_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(header().string(RequestLoggingFilter.X_REQUEST_ID, "trace-abc-123"))

        assertNull(MDC.get("memberId"))
    }

    @Test
    fun `인증 실패(401) 시에도 traceId 가 있으면 X-Request-ID 가 응답에 남고 401 로그가 기록된다`(output: CapturedOutput) {
        MDC.put(RequestLoggingFilter.TRACE_ID, "trace-unauthorized-401")

        mockMvc
            .perform(get(PROTECTED_PATH))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string(RequestLoggingFilter.X_REQUEST_ID, "trace-unauthorized-401"))

        output.all.shouldContain("HTTP GET $PROTECTED_PATH status=401")
        assertNull(MDC.get("memberId"))
    }

    @Test
    fun `존재하지 않는 경로(404) 요청 시에도 X-Request-ID 가 반환되고 404 로그가 기록된다`(output: CapturedOutput) {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken
        MDC.put(RequestLoggingFilter.TRACE_ID, "trace-not-found-404")

        mockMvc
            .perform(
                get("/non-existent-slice-endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isNotFound)
            .andExpect(header().string(RequestLoggingFilter.X_REQUEST_ID, "trace-not-found-404"))

        output.all.shouldContain("HTTP GET /** status=404")
        assertNull(MDC.get("memberId"))
    }

    @Test
    fun `CORS 응답에 X-Request-ID 가 Access-Control-Expose-Headers 로 노출된다`() {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken

        mockMvc
            .perform(
                get(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Request-ID"))
    }

    @Test
    fun `경로 변수가 포함된 API 호출 시 route 패턴이 실제 Access Log 에 정확히 기록된다`(output: CapturedOutput) {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken
        MDC.put(RequestLoggingFilter.TRACE_ID, "trace-item-456")

        mockMvc
            .perform(
                get("/test/items/456")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("456"))
            .andExpect(header().string(RequestLoggingFilter.X_REQUEST_ID, "trace-item-456"))

        output.all.shouldContain("HTTP GET /test/items/{id} status=200")
        assertNull(MDC.get("memberId"))
    }

    @Test
    fun `서버 내부 예외(5xx) 발생 시에도 X-Request-ID 가 응답에 남고 MDC 가 안전하게 복원된다`(output: CapturedOutput) {
        val token = jwtTokenProvider.issue(memberId = 1L).accessToken
        MDC.put(RequestLoggingFilter.TRACE_ID, "trace-error-500")

        mockMvc
            .perform(
                get("/test/error-500")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
            ).andExpect(status().isInternalServerError)
            .andExpect(header().string(RequestLoggingFilter.X_REQUEST_ID, "trace-error-500"))

        output.all.shouldContain("HTTP GET /test/error-500 status=500")
        assertNull(MDC.get("memberId"))
    }
}
