package team.aligner.mock

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import team.aligner.support.web.AuthMemberPort
import team.aligner.support.web.auth.KakaoUserClient

/**
 * 목 Bean 을 명시 등록한다. ComponentScan 을 쓰지 않는 것은 실제 조립과 같다
 * (docs/architecture.md §5).
 *
 * 도메인 `api` 모듈의 AutoConfiguration 은 application.yml 의 `spring.autoconfigure.exclude`
 * 로 꺼둔다. 그 모듈을 의존하는 이유는 **응답 DTO 를 재사용하기 위해서**이고, 컨트롤러까지
 * 함께 올라오면 실제 서비스 Bean 이 없어 기동이 실패한다.
 *
 * **인증은 바깥 경계 하나만 끊는다.** `support-web` 을 통째로 끄지 않고 `KakaoUserClient` 만
 * 목으로 갈아끼우므로, 실제 `KakaoAuthController` 가 그대로 돌고 토큰 발급·검증·CORS·오류
 * 포맷이 모두 진짜다. 목이 로그인 컨트롤러를 따로 만들면 그만큼 계약이 갈라질 자리가 생긴다.
 */
@AutoConfiguration
class MockApiAutoConfiguration {
    /**
     * **카카오 HTTP 호출만 끊는다.** `@Primary` 로 실제 `RestClientKakaoUserClient` 대신
     * 주입되고, 그 위의 `KakaoAuthController` 는 실제 코드가 그대로 돈다.
     */
    @Bean
    @Primary
    internal fun mockKakaoUserClient(): KakaoUserClient = MockKakaoUserClient()

    /**
     * member:adapter-auth 를 대신한다. DB 가 없어 회원을 저장할 수 없으므로 이 자리만
     * 고정 회원으로 갈아끼운다 (docs/architecture.md §9).
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
