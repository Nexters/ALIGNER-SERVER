package team.aligner.catalog.adapter.ymove

import team.aligner.catalog.infrastructure.PoseVideoPlayback
import team.aligner.catalog.infrastructure.PoseVideoPort
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 재생 URL 을 짧은 TTL 로 캐시한다.
 *
 * **YMove 문서는 재생 URL 을 "저장하거나 캐시하지 말라" 고 한다. 그럼에도 캐시하는 근거는 셋이다.**
 *
 * 1. **만료보다 훨씬 짧다.** `videoUrl` 의 만료를 47 시간으로 실측했다. 30 분 TTL 이면 죽은 URL 을
 *    사용자에게 줄 수 있는 창이 없다 — 문서 권고가 막으려는 실질적 위험이 발생하지 않는다.
 * 2. **세션 플레이어가 같은 운동을 반복 조회한다.** 앱 재진입 복구 경로가
 *    `GET /catalog/exercises/{id}` 를 다시 부른다 (docs/user-flow.md). 캐시가 없으면 재진입마다
 *    외부 호출이다.
 * 3. **`GET /exercises/{slug}` 는 월 고유 운동 상한에 카운트된다.** 캐시가 곧 상한 방어다.
 *
 * TTL 은 설정으로 빠져 있어 문제가 생기면 0 으로 끈다 (YmoveProperties.cacheTtlSeconds).
 *
 * 캐시를 RestClientPoseVideoAdapter 안에 섞지 않고 데코레이터로 둔 것은, 그래야 어댑터를 캐시
 * 없이 테스트할 수 있고 캐시를 끄는 것이 조립 한 줄이 되기 때문이다.
 */
internal class CachingPoseVideoAdapter(
    private val delegate: PoseVideoPort,
    private val ttl: Duration,
    private val clock: Clock,
) : PoseVideoPort {
    /**
     * 크기 상한을 두지 않는다. 키가 `ymove_slug` 이고 그 값은 감수로 고정된 자세 집합에서만
     * 나오므로 (docs/context/routine-content.md) 원소 수가 콘텐츠 개수를 넘지 않는다.
     * **호출부가 임의 문자열을 넣을 수 있게 되면 이 전제가 깨진다.**
     */
    private val cache = ConcurrentHashMap<String, Entry>()

    override fun findPlayback(ymoveSlugs: List<String>): Map<String, PoseVideoPlayback> {
        if (ymoveSlugs.isEmpty()) {
            return emptyMap()
        }

        val now = clock.instant()
        val hits = mutableMapOf<String, PoseVideoPlayback>()
        val misses = mutableListOf<String>()

        // 전부-아니면-전무로 나누지 않는다. 코스 상세처럼 여러 자세를 그리는 화면에서 하나만
        // 만료돼도 전체를 다시 받게 되어 캐시가 사실상 안 먹는다.
        for (slug in ymoveSlugs.distinct()) {
            val entry = cache[slug]
            if (entry != null && now.isBefore(entry.expiresAt)) {
                hits[slug] = entry.playback
            } else {
                if (entry != null) {
                    cache.remove(slug, entry)
                }
                misses += slug
            }
        }

        if (misses.isEmpty()) {
            return hits
        }

        val fetched = delegate.findPlayback(misses)
        val expiresAt = now.plus(ttl)
        // 못 받은 slug 는 캐시하지 않는다. 장애를 TTL 만큼 고정하면 복구가 그만큼 늦는다.
        fetched.forEach { (slug, playback) -> cache[slug] = Entry(playback, expiresAt) }

        return hits + fetched
    }

    private data class Entry(
        val playback: PoseVideoPlayback,
        val expiresAt: Instant,
    )
}
