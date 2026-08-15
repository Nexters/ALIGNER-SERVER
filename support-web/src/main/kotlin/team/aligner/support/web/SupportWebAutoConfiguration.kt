package team.aligner.support.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import team.aligner.support.web.auth.AuthProperties
import team.aligner.support.web.auth.JwtTokenProvider
import team.aligner.support.web.auth.KakaoAuthController
import team.aligner.support.web.auth.KakaoUserClient
import team.aligner.support.web.auth.NimbusJwtTokenProvider
import team.aligner.support.web.auth.RestClientKakaoUserClient
import java.time.Duration

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

    /**
     * RestClient 를 여기서 조립해 넘긴다. 클라이언트가 내부에서 빌더를 만들고 requestFactory 를
     * 덮어쓰면 테스트가 MockRestServiceServer 를 끼울 자리가 없다 — 목이 심어둔 팩토리를
     * 클라이언트가 다시 갈아끼우기 때문이다. 타임아웃은 배선 관심사라 조립부에 두는 편이 맞다.
     *
     * 카카오가 느려지면 로그인 요청이 스레드를 잡고 있다. 짧게 끊고 502 로 알린다.
     */
    @Bean
    fun kakaoUserClient(
        authProperties: AuthProperties,
        restClientBuilder: RestClient.Builder,
    ): KakaoUserClient {
        val restClient =
            restClientBuilder
                .requestFactory(
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(Duration.ofMillis(authProperties.kakao.connectTimeoutMillis))
                        setReadTimeout(Duration.ofMillis(authProperties.kakao.readTimeoutMillis))
                    },
                ).build()

        return RestClientKakaoUserClient(authProperties, restClient)
    }

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
