package team.aligner.support.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import team.aligner.support.core.CommonErrorCode
import team.aligner.support.web.auth.JwtAuthenticationFilter
import team.aligner.support.web.auth.JwtTokenProvider
import tools.jackson.databind.ObjectMapper

/**
 * 도메인 횡단 보안 설정.
 *
 * 인증은 자체 JWT 다. 프론트가 카카오에서 받은 인가 코드를 POST /auth/kakao 로 넘기면 서버가
 * 액세스 토큰으로 교환·확인 후 JWT 를 발급하고, 이후 요청은 Authorization: Bearer 로 온다.
 *
 * **서버가 리다이렉트를 받지 않고 JWT 를 쿠키로도 주지 않는다.** 그래서 아래 csrf.disable() 과
 * STATELESS 가 안전하다 — 브라우저가 자동으로 실어 보내는 인증 수단이 없기 때문이다. 쿠키
 * 인증을 도입하면 CSRF 대응과 CORS 설계가 함께 따라온다 (이슈 #12).
 *
 * 프론트가 다른 오리진이라 CORS 를 함께 켠다. 근거는 corsConfigurationSource 에 적었다 (이슈 #17).
 *
 * 기본값은 `authenticated()` 로 닫아둔다. 이 클래스는 AutoConfiguration.imports 에
 * 등록돼 프로덕션 경로에도 그대로 실리므로, 공개 경로는 메서드까지 한정해 하나씩 명시한다.
 *
 * Pod 이중화 전제이므로 세션을 쓰지 않는다 (AGENTS.md §4 배포 구성).
 *
 * `before` 가 없으면 Boot 기본 체인이 이긴다. auto-configuration 은 클래스명 알파벳순으로 먼저
 * 정렬되므로 org.springframework... 가 team.aligner... 보다 앞선다. 그 시점에 우리
 * SecurityFilterChain 은 아직 등록 전이라 ServletWebSecurityAutoConfiguration 의
 * `@ConditionalOnDefaultWebSecurity` 가 통과해 기본 체인이 함께 뜨고, 기본 체인의
 * `@Order(BASIC_AUTH_ORDER)` 가 우리 것(순서 미지정)보다 앞서 매칭된다. 결과적으로 아래 설정이
 * 전부 죽는다.
 */
@AutoConfiguration(
    beforeName = [
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
    ],
)
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties::class)
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtTokenProvider: JwtTokenProvider,
        corsConfigurationSource: CorsConfigurationSource,
        objectMapper: ObjectMapper,
    ): SecurityFilterChain =
        http
            // 소스를 이름 규약("corsConfigurationSource" Bean 탐색)에 맡기지 않고 직접 넘긴다.
            // 이름을 바꾸면 조용히 CORS 가 꺼지는 배선은 두지 않는다.
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // 메서드까지 한정해 같은 경로의 다른 메서드가 딸려 열리지 않게 한다.
                it.requestMatchers(HttpMethod.POST, PublicPaths.LOGIN).permitAll()
                // API 문서. 브라우저가 Authorization 헤더를 붙이지 못해서 닫아두면 UI 자체를
                // 열 수 없다. 근거와 끄는 방법은 PublicPaths 에 적었다.
                it.requestMatchers(HttpMethod.GET, *PublicPaths.API_DOCS).permitAll()
                it.requestMatchers(HttpMethod.GET, *PublicPaths.SWAGGER_UI).permitAll()
                // K8s 프로브 및 헬스체크. GET 메서드만 인증 없이 허용한다.
                it.requestMatchers(HttpMethod.GET, *PublicPaths.ACTUATOR).permitAll()
                it.anyRequest().authenticated()
            }
            // JwtAuthenticationFilter 를 @Bean 으로 올리지 않는다. Boot 의 서블릿 필터 자동 등록이
            // 시큐리티 체인 밖에서 한 번 더 실행시킨다.
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            ).exceptionHandling {
                // 진입점을 지정하지 않으면 인증 없는 요청이 401 이 아니라 403 으로 나간다.
                it.authenticationEntryPoint { _, response, _ ->
                    response.writeError(objectMapper, CommonErrorCode.UNAUTHORIZED)
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.writeError(objectMapper, CommonErrorCode.FORBIDDEN)
                }
            }.build()

    /**
     * 교차 출처 허용 범위.
     *
     * **preflight 를 permitAll 로 열지 않는다.** `.cors { }` 가 등록하는 CorsFilter 는 인가 필터보다
     * 앞에 있고, preflight `OPTIONS` 를 그 자리에서 200 으로 끝내고 체인을 더 태우지 않는다. 그래서
     * 인가 규칙에 닿지 않는다. requestMatchers(OPTIONS).permitAll() 을 더하면 CORS 헤더는 그대로 없는
     * 채 모든 경로의 OPTIONS 만 열려서, 문제는 그대로고 경계만 넓어진다.
     *
     * 실패 응답에도 헤더가 붙는다. CorsFilter 가 헤더를 먼저 얹고 체인을 잇기 때문에, 401 · 404 도
     * 프론트가 본문을 읽을 수 있다 — 못 읽으면 만료된 토큰이 네트워크 오류로 보인다.
     *
     * **allowCredentials 를 켜지 않는다.** 인증 수단이 `Authorization` 헤더뿐이라 필요가 없고,
     * 브라우저가 자동으로 실어 보내는 것이 없다는 전제(위 csrf.disable() 근거)가 여기서도 같다.
     * 프론트는 `credentials: 'include'` 를 쓰지 않는다.
     */
    @Bean
    fun corsConfigurationSource(corsProperties: CorsProperties): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                // 패턴이 아니라 정확한 오리진 목록이다. 서브도메인 와일드카드를 쓰면 누가 그 이름을
                // 잡았는지까지 우리가 보증해야 한다.
                allowedOrigins = corsProperties.allowedOrigins
                // 현재 서버가 실제로 제공하는 메서드만 적는다. PublicPaths 의 permitAll 을 메서드까지
                // 한정한 것과 같은 이유다.
                //
                // DELETE 는 회원탈퇴(DELETE /members/me) 하나 때문에 열었다. 그 요청도 Authorization
                // 헤더로 인증하고 allowCredentials 가 꺼져 있어, 브라우저가 제3자 사이트에서 쿠키를
                // 실어 보내 탈퇴를 유발하는 경로는 없다. PUT 은 제공하는 엔드포인트가 없어 닫아둔다.
                allowedMethods =
                    listOf(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                    )
                // 프론트가 실어 보내는 헤더도 둘뿐이다. 보호 API 는 Authorization, 로그인은
                // Content-Type 때문에 preflight 를 탄다. 여기 빠진 헤더를 프론트가 붙이면
                // 그 요청은 preflight 단계에서 403 으로 끊긴다.
                allowedHeaders = listOf(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE)
                maxAge = corsProperties.maxAgeSeconds
            }

        // 경로별로 다르게 줄 이유가 없다. 공개 경로(로그인·문서)도 같은 오리진에서만 부른다.
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", configuration) }
    }

    /**
     * 필터체인이 만드는 실패 응답도 GlobalExceptionHandler 와 같은 포맷이어야 한다.
     * 여기만 다르면 클라이언트가 에러 파싱을 두 벌 만들게 된다.
     */
    private fun jakarta.servlet.http.HttpServletResponse.writeError(
        objectMapper: ObjectMapper,
        errorCode: CommonErrorCode,
    ) {
        status = errorCode.status
        contentType = MediaType.APPLICATION_JSON_VALUE
        characterEncoding = Charsets.UTF_8.name()
        writer.write(objectMapper.writeValueAsString(ApiErrorResponse.from(errorCode)))
    }
}
