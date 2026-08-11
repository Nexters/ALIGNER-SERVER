package team.aligner.mock

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.support.web.AuthMemberPort

/**
 * 목 Bean 을 명시 등록한다. ComponentScan 을 쓰지 않는 것은 실제 조립과 같다
 * (docs/architecture.md §5).
 *
 * 도메인 `api` 모듈의 AutoConfiguration 은 application.yml 의 `spring.autoconfigure.exclude`
 * 로 꺼둔다. 그 모듈을 의존하는 이유는 **응답 DTO 를 재사용하기 위해서**이고, 컨트롤러까지
 * 함께 올라오면 실제 서비스 Bean 이 없어 기동이 실패한다.
 */
@AutoConfiguration
class MockApiAutoConfiguration {
    /**
     * member:adapter-auth 를 대신한다. 인증 자체는 support-web 의 실제 코드가 그대로 돈다
     * (docs/architecture.md §9).
     */
    @Bean
    internal fun authMemberPort(): AuthMemberPort = MockAuthMemberAdapter()

    @Bean
    internal fun mockMemberController(): MockMemberController = MockMemberController()

    @Bean
    internal fun mockScreeningController(): MockScreeningController = MockScreeningController()

    @Bean
    internal fun mockCatalogController(): MockCatalogController = MockCatalogController()

    @Bean
    internal fun mockCourseController(): MockCourseController = MockCourseController()

    @Bean
    internal fun mockSessionController(): MockSessionController = MockSessionController()
}
