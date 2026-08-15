package team.aligner.catalog.adapter.ymove

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * ComponentScan 이 없으므로 @EnableConfigurationProperties 로 등록해야 바인딩된다.
 * 빠지면 기동은 성공하고 영상만 안 나온다 (CatalogYmoveAdapterAutoConfiguration 참고).
 */
@ConfigurationProperties(prefix = "aligner.ymove")
data class YmoveProperties(
    /** `https://exercise-api.ymove.app/api/v2`. 끝에 `/` 를 붙이지 않는다. */
    val baseUrl: String,
    /**
     * `X-API-Key` 헤더로 보낸다. 쿼리 파라미터(`?api_key=`)도 되지만 **URL 은 액세스 로그·프록시·
     * 리퍼러에 남는다.** 헤더만 쓴다. DB_PASSWORD · JWT_SECRET 과 같이 환경변수로만 넣는다.
     */
    val apiKey: String,
    val connectTimeoutMillis: Long,
    val readTimeoutMillis: Long,
    /**
     * 재생 URL 캐시 TTL. **0 이면 캐시를 끈다.**
     *
     * YMove 문서는 재생 URL 을 "저장하거나 캐시하지 말라" 고 하지만 우리는 캐시한다. 근거는
     * CachingPoseVideoAdapter 주석에 있다. 만료(실측 47 시간)보다 훨씬 짧게 두는 것이 전제라
     * 이 값을 시간 단위로 올리면 안 된다.
     */
    val cacheTtlSeconds: Long,
)
