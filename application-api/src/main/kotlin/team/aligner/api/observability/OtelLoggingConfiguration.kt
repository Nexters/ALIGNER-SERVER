package team.aligner.api.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Logback 이벤트를 OpenTelemetry SDK 로 전달하는 애펜더 설치.
 *
 * Spring Boot 는 OTLP logs export 자동 구성(exporter 빈)을 제공하지만
 * Logback 애펜더는 직접 설치해야 한다 — 설치 전에 찍힌 로그는 SDK 에
 * 도달하지 않는다. install() 은 컨텍스트 준비 후 최초 1회면 충분하며,
 * 이후 모든 logger 가 trace_id/span_id 를 자동 부착해 내보낸다.
 *
 * 수신처는 application.yml 의 management.opentelemetry.logging.export.otlp.endpoint
 * (클러스터 otel-collector → Loki /otlp)다.
 */
@Configuration(proxyBeanMethods = false)
class OtelLoggingConfiguration {

    @Bean
    fun openTelemetryAppenderInstaller(openTelemetry: OpenTelemetry): InitializingBean =
        InitializingBean { OpenTelemetryAppender.install(openTelemetry) }
}
