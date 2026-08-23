package team.aligner.api.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

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
 *
 * dev·prod 에서만 활성화한다. 프로필 없는 컨텍스트(로컬 순수 유닛 테스트,
 * CI 통합 테스트)에서는 endpoint 가 도달 불가능한 클러스터 DNS 여서 export
 * 재시도 로그가 다시 애펜더로 들어오는 루프가 생기고, 이는 테스트 워커가
 * 끝나지 않는 형태로 나타났다(런 32635013590 참조).
 */
@AutoConfiguration
@Profile("dev", "prod")
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
