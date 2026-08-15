package team.aligner.catalog.adapter.ymove

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import team.aligner.catalog.infrastructure.PoseVideoPort
import java.time.Clock
import java.time.Duration

/**
 * 이 클래스의 FQCN 은
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 에 있어야 한다.
 * 빠지면 기동은 되고 PoseVideoPort Bean 만 없다 (docs/architecture.md §5).
 *
 * @EnableConfigurationProperties 도 같은 이유로 필요하다. 없으면 aligner.ymove.* 가 바인딩되지
 * 않는다.
 */
@AutoConfiguration
@EnableConfigurationProperties(YmoveProperties::class)
class CatalogYmoveAdapterAutoConfiguration {
    /**
     * RestClient 를 여기서 조립해 넘긴다. 어댑터가 내부에서 빌더를 만들고 requestFactory 를
     * 덮어쓰면 테스트가 MockRestServiceServer 를 끼울 자리가 없다 — 목이 심어둔 팩토리를
     * 어댑터가 다시 갈아끼우기 때문이다. 타임아웃은 배선 관심사라 조립부에 두는 편이 맞다.
     * (SupportWebAutoConfiguration.kakaoUserClient 와 같은 판단이다.)
     *
     * YMove 가 느려지면 운동 상세 요청이 스레드를 잡고 있다. 짧게 끊고 videoUrl 만 비운다 —
     * 502 로 올리지 않는 근거는 PoseVideoPort 주석에 있다.
     */
    @Bean
    fun poseVideoPort(
        properties: YmoveProperties,
        restClientBuilder: RestClient.Builder,
    ): PoseVideoPort {
        val restClient =
            restClientBuilder
                .requestFactory(
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMillis))
                        setReadTimeout(Duration.ofMillis(properties.readTimeoutMillis))
                    },
                ).build()

        val adapter = RestClientPoseVideoAdapter(properties, restClient)

        // 0 이면 캐시를 끼우지 않는다. 데코레이터라 이 한 줄이 스위치다.
        if (properties.cacheTtlSeconds <= 0) {
            return adapter
        }
        return CachingPoseVideoAdapter(
            delegate = adapter,
            ttl = Duration.ofSeconds(properties.cacheTtlSeconds),
            clock = Clock.systemUTC(),
        )
    }
}
