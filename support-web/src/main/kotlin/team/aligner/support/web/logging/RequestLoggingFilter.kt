package team.aligner.support.web.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping
import java.util.concurrent.TimeUnit

/**
 * HTTP 요청/응답 생명주기 및 관측 컨텍스트를 소유하는 서블릿 필터.
 *
 * Spring Boot 의 ServerHttpObservationFilter (HIGHEST_PRECEDENCE + 1) 뒤에서 실행되어
 * 이미 활성화된 Micrometer / OpenTelemetry traceId / spanId context 를 활용한다.
 *
 * 주요 역할:
 * 1. X-Request-ID 응답 헤더 전파 (MDC traceId 바인딩)
 * 2. 톰캣 스레드 풀 오염 방지를 위한 MDC cleanup 및 Outer context snapshot 복원
 * 3. /actuator 등 운영성 노이즈 경로를 제외한 단일 HTTP Access Log 기록 (route 기반 표준화)
 */
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val previousMdc = MDC.getCopyOfContextMap()
        val startedAt = System.nanoTime()

        try {
            MDC.get(TRACE_ID)?.let { response.setHeader(X_REQUEST_ID, it) }
            filterChain.doFilter(request, response)
        } finally {
            if (!isNoiseEndpoint(request)) {
                val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                val route = resolveRoute(request)
                log.info(
                    "HTTP {} {} status={} durationMs={}",
                    request.method,
                    route,
                    response.status,
                    durationMs,
                )
            }
            MDC.clear()
            previousMdc?.let(MDC::setContextMap)
        }
    }

    private fun resolveRoute(request: HttpServletRequest): String =
        (request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String)
            ?: request.requestURI

    private fun isNoiseEndpoint(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return path == "/actuator" || path.startsWith("/actuator/")
    }

    companion object {
        const val X_REQUEST_ID = "X-Request-ID"
        const val TRACE_ID = "traceId"
    }
}
