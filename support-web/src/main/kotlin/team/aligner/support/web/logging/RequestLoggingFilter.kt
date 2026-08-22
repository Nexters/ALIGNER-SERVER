package team.aligner.support.web.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.TimeUnit

/**
 * HTTP 요청/응답 생명주기를 감싸고 분산 추적 ID(X-Request-ID) 전파 및 Access Log 를 기록한다.
 *
 * Spring Boot 의 ServerHttpObservationFilter (HIGHEST_PRECEDENCE + 1) 뒤에서 실행되어
 * 이미 활성화된 Micrometer traceId / spanId context 를 활용한다.
 *
 * 요청 완료 후 MDC.clear() 로 톰캣 스레드 풀 오염을 방지하되,
 * 필터 진입 전 snapshot 을 복원하여 Micrometer 의 outer scope 를 보존한다.
 */
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    public override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI.startsWith("/actuator")

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
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            log.info(
                "HTTP {} {} status={} durationMs={}",
                request.method,
                request.requestURI,
                response.status,
                durationMs,
            )
            MDC.clear()
            previousMdc?.let(MDC::setContextMap)
        }
    }

    companion object {
        const val X_REQUEST_ID = "X-Request-ID"
        const val TRACE_ID = "traceId"
    }
}
