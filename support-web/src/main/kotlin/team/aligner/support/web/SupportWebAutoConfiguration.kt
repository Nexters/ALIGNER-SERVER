package team.aligner.support.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import team.aligner.support.web.auth.AuthProperties
import team.aligner.support.web.auth.JwtTokenProvider
import team.aligner.support.web.auth.KakaoAuthController
import team.aligner.support.web.auth.KakaoUserClient
import team.aligner.support.web.auth.NimbusJwtTokenProvider
import team.aligner.support.web.auth.RestClientKakaoUserClient

/**
 * support-web 의 Bean 을 명시 등록한다.
 *
 * ComponentScan 을 쓰지 않으므로 @RestControllerAdvice·@RestController 는 스캔되지 않는다.
 * 여기에 @Bean 으로 올리지 않으면 존재하지 않는 것과 같다 (docs/architecture.md §5).
 *
 * @EnableConfigurationProperties 도 같은 이유로 필요하다. 없으면 aligner.auth.* 가 바인딩되지
 * 않고, 기동은 성공하는데 로그인만 죽는다.
 *
 * 이 클래스의 FQCN 은
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 에 있어야 한다.
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthProperties::class)
class SupportWebAutoConfiguration {
    @Bean
    fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()

    @Bean
    fun kakaoUserClient(authProperties: AuthProperties): KakaoUserClient = RestClientKakaoUserClient(authProperties)

    @Bean
    fun jwtTokenProvider(authProperties: AuthProperties): JwtTokenProvider = NimbusJwtTokenProvider(authProperties)

    /**
     * AuthMemberPort 를 요구하는 유일한 Bean 이다. member:adapter-auth 가 조립에서 빠지면
     * 여기서 기동이 실패해야 정상이다 (docs/architecture.md §9).
     */
    @Bean
    fun kakaoAuthController(
        kakaoUserClient: KakaoUserClient,
        authMemberPort: AuthMemberPort,
        jwtTokenProvider: JwtTokenProvider,
    ): KakaoAuthController = KakaoAuthController(kakaoUserClient, authMemberPort, jwtTokenProvider)
}
