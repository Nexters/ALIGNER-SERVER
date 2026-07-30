package team.aligner.support.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

/**
 * support-web 의 Bean 을 명시 등록한다.
 *
 * ComponentScan 을 쓰지 않으므로 @RestControllerAdvice·@RestController 는 스캔되지 않는다.
 * 여기에 @Bean 으로 올리지 않으면 존재하지 않는 것과 같다 (docs/architecture.md §5).
 *
 * 이 클래스의 FQCN 은
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 에 있어야 한다.
 */
@AutoConfiguration
class SupportWebAutoConfiguration {
    @Bean
    fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()
}
