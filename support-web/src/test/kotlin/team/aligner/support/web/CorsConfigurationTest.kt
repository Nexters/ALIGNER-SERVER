package team.aligner.support.web

import jakarta.servlet.Filter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import team.aligner.support.web.bootstrap.PROTECTED_PATH
import team.aligner.support.web.bootstrap.SupportWebTestApplication

/**
 * CORS 를 **필터체인 위에서** 확인한다 (이슈 #17).
 *
 * 러너는 Kotest 가 아니라 JUnit5 다. kotest-extensions-spring 이 버전 카탈로그에 없어 Kotest 로는
 * 컨텍스트를 띄울 수 없다 (MemberRepositoryIntegrationTest 와 같은 판단). 단언은 MockMvc 의
 * ResultMatcher 를 그대로 쓴다 — 응답 헤더 검사라 여기서는 그쪽이 읽기 쉽다.
 *
 * CorsConfiguration 객체의 필드를 읽는 단위 테스트로는 이 이슈가 고친 것을 못 잡는다. 문제는
 * "설정 값이 무엇인가" 가 아니라 **"preflight 가 인가 필터에 걸려 401 로 끝나는가"** 였고,
 * 그건 필터 순서의 문제라 컨텍스트를 띄워야만 드러난다.
 */
@SpringBootTest(
    classes = [SupportWebTestApplication::class],
    properties = [
        "aligner.web.cors.allowed-origins=$ALLOWED_ORIGIN",
        "aligner.web.cors.max-age-seconds=3600",
        "aligner.auth.jwt.secret=cors-test-signing-key-1234567890-abcdef",
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
class CorsConfigurationTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    /**
     * 필터체인을 손으로 끼운다. @AutoConfigureMockMvc 를 쓰지 않는 이유는 Boot 4 가 그 애노테이션을
     * starter-test 밖의 별도 모듈로 뺐기 때문이다 — 테스트 편의 애노테이션 하나를 위해 의존성을
     * 늘리지 않는다 (docs/architecture.md §3 · CONTRIBUTING.md §5 의 의존성 추가 기준).
     *
     * 대신 검증 대상이 무엇인지가 더 분명해진다. 이 테스트가 보는 것은 컨트롤러가 아니라
     * **springSecurityFilterChain 안의 필터 순서**다.
     */
    private lateinit var mockMvc: MockMvc

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private lateinit var securityFilterChain: Filter

    @BeforeEach
    fun setUp() {
        // addFilters 가 자기 타입을 돌려주는 제네릭이라 Kotlin 이 추론하지 못한다. 명시한다.
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters<DefaultMockMvcBuilder>(securityFilterChain)
                .build()
    }

    @Test
    fun `보호 경로의 preflight 는 토큰 없이도 통과한다`() {
        // 이 이슈 이전에는 여기가 401 이었다. preflight 는 Authorization 헤더를 달고 오지 않아서
        // anyRequest().authenticated() 에 걸렸고, 프론트는 로그인조차 할 수 없었다.
        mockMvc
            .perform(
                options(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION),
            ).andExpect(status().isOk)
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
    }

    @Test
    fun `로그인 경로의 preflight 도 통과한다`() {
        // Content-Type: application/json 이라 로그인도 simple request 가 아니다.
        mockMvc
            .perform(
                options(LOGIN_PATH)
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.CONTENT_TYPE),
            ).andExpect(status().isOk)
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
    }

    @Test
    fun `허용하지 않은 오리진의 preflight 는 막는다`() {
        mockMvc
            .perform(
                options(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"),
            ).andExpect(status().isForbidden)
            // 헤더가 없으면 브라우저가 응답을 스크립트에 넘기지 않는다. 상태 코드보다 이쪽이 본질이다.
            .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN))
    }

    @Test
    fun `허용 목록에 없는 메서드는 preflight 에서 막는다`() {
        // PUT 은 제공하는 엔드포인트가 없어 닫아뒀다. 메서드 목록이 실제 API 를 따라간다는 확인이다.
        mockMvc
            .perform(
                options(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT"),
            ).andExpect(status().isForbidden)
    }

    /**
     * 회원탈퇴(DELETE /members/me)가 브라우저에서 실제로 나가려면 preflight 가 통과해야 한다.
     * 메서드 목록에서 DELETE 가 빠지면 서버 로직이 멀쩡해도 프론트에서는 호출 자체가 막힌다.
     */
    @Test
    fun `회원탈퇴를 위해 DELETE preflight 가 통과한다`() {
        mockMvc
            .perform(
                options(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "DELETE"),
            ).andExpect(status().isOk)
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
    }

    @Test
    fun `허용 목록에 없는 요청 헤더는 preflight 에서 막는다`() {
        // 프론트가 커스텀 헤더를 붙이기 시작하면 여기서 끊긴다. 서버에 먼저 알려야 한다는 뜻이다.
        mockMvc
            .perform(
                options(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-Custom-Trace"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `토큰 없는 본요청은 401 이지만 CORS 헤더는 붙는다`() {
        // 헤더가 빠지면 브라우저가 401 본문을 막아서, 만료된 토큰이 프론트에 네트워크 오류로 보인다.
        mockMvc
            .perform(get(PROTECTED_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
    }

    @Test
    fun `자격 증명 모드를 열지 않는다`() {
        // 인증이 Authorization 헤더뿐이라 필요가 없다. 켜면 브라우저가 쿠키를 실어 보낼 수 있게 되고
        // csrf.disable() 의 전제(자동으로 실리는 인증 수단이 없다)가 깨진다.
        mockMvc
            .perform(
                options(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"),
            ).andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_CREDENTIALS))
    }

    @Test
    fun `오리진이 없는 요청은 CORS 와 무관하게 다뤄진다`() {
        // 같은 오리진 요청과 서버 간 호출이다. 여기에 CORS 헤더가 붙으면 설정이 새고 있는 것이다.
        mockMvc
            .perform(get(PROTECTED_PATH))
            .andExpect(status().isUnauthorized)
            .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN))
    }

    @Test
    fun `포트가 다르면 다른 오리진이다`() {
        // 스킴·호스트·포트가 완전히 일치해야 한다. 프론트 개발 서버 포트가 바뀌면 여기서 막힌다.
        mockMvc
            .perform(
                options(PROTECTED_PATH)
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"),
            ).andExpect(status().isForbidden)
    }
}

/** 프론트 개발 서버 오리진. application.yml 의 기본값과 같은 값이다. */
const val ALLOWED_ORIGIN = "http://localhost:5173"

private const val LOGIN_PATH = "/auth/kakao"
private const val ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method"
private const val ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers"
private const val ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
private const val ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials"
