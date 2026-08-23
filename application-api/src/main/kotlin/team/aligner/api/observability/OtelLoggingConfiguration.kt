package team.aligner.api.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

/**
 * Logback 이벤트를 OpenTelemetry SDK 로 전달하는 애펜더 부착.
 *
 * Spring Boot 는 OTLP logs export 자동 구성(exporter 빈)까지 제공하지만
 * Logback 애펜더는 직접 붙여야 한다 — 없으면 어떤 로그도 SDK 에 도달하지
 * 않는다. 이 프로젝트는 ComponentScan 을 쓰지 않으므로(AlignerApplication
 * 문서 참조) AutoConfiguration.imports 로 등록한다.
 *
 * logback.xml 대신 프로그래밍 방식으로 root logger 에 붙이는 이유: 선언적
 * 설정 파일을 추가하면 Spring Boot 가 yml(logging.structured.format 등)로
 * 제공하던 기본 console 포맷을 덮어써서 ECS 출력이 깨진다.
 *
 * 수신처는 application.yml 의 management.opentelemetry.logging.export.otlp.endpoint
 * (클러스터 otel-collector → Loki /otlp)다.
 */
@AutoConfiguration
class OtelLoggingConfiguration {
    @Bean
    fun openTelemetryAppenderInstaller(openTelemetry: OpenTelemetry): InitializingBean =
        InitializingBean {
            val loggerContext = LoggerFactory.getILoggerFactory() as ch.qos.logback.classic.LoggerContext
            val otelAppender = OpenTelemetryAppender()
            otelAppender.context = loggerContext
            otelAppender.start()
            loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(otelAppender)
            OpenTelemetryAppender.install(openTelemetry)
        }
}
