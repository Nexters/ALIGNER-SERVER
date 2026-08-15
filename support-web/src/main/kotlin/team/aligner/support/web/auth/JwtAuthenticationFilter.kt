package team.aligner.support.web.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import team.aligner.support.web.AlignerPrincipal

/**
 * Authorization 헤더의 Bearer 토큰을 AlignerPrincipal 로 바꿔 SecurityContext 에 넣는다.
 *
 * **토큰이 없거나 깨졌어도 예외를 던지지 않고 통과시킨다.** 인증 없이 열어둔 경로가 있고,
 * 401 판정은 필터체인의 authorizeHttpRequests 가 한다. 여기서 던지면 permitAll 경로까지 막힌다.
 *
 * @Bean 으로 등록하지 않는다. Boot 의 서블릿 필터 자동 등록이 시큐리티 체인 밖에서 한 번 더
 * 실행시킨다. SecurityConfig 가 직접 생성해 체인에만 넣는다.
 */
internal class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        resolveToken(request)
            ?.let { jwtTokenProvider.parseMemberId(it) }
            ?.let { memberId ->
                val principal = AlignerPrincipal(memberId = memberId)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(principal, null, emptyList())
            }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? =
        request
            .getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.takeIf { it.isNotBlank() }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
