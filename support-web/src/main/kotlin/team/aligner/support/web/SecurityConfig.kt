package team.aligner.support.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
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
@AutoConfiguration(before = [ServletWebSecurityAutoConfiguration::class])
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtTokenProvider: JwtTokenProvider,
        objectMapper: ObjectMapper,
    ): SecurityFilterChain =
        http
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
