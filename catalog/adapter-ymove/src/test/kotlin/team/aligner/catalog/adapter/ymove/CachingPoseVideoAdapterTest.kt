package team.aligner.catalog.adapter.ymove

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import team.aligner.catalog.infrastructure.PoseVideoPlayback
import team.aligner.catalog.infrastructure.PoseVideoPort
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * `Clock` 을 주입받게 만든 이유가 여기다 — 만료를 `Thread.sleep` 없이 검증한다.
 */
class CachingPoseVideoAdapterTest {
    private val ttl = Duration.ofMinutes(30)

    @Test
    fun `두 번째 조회는 delegate 를 부르지 않는다`() {
        val delegate = RecordingPort(mapOf("camel-pose" to "https://cdn/camel.mp4"))
        val caching = CachingPoseVideoAdapter(delegate, ttl, MutableClock(NOW))

        caching.findPlayback(listOf("camel-pose"))
        caching.findPlayback(listOf("camel-pose"))

        delegate.calls shouldContainExactly listOf(listOf("camel-pose"))
    }

    @Test
    fun `TTL 이 지나면 다시 부른다`() {
        val delegate = RecordingPort(mapOf("camel-pose" to "https://cdn/camel.mp4"))
        val clock = MutableClock(NOW)
        val caching = CachingPoseVideoAdapter(delegate, ttl, clock)

        caching.findPlayback(listOf("camel-pose"))
        clock.now = NOW.plus(ttl).plusSeconds(1)
        caching.findPlayback(listOf("camel-pose"))

        delegate.calls.size shouldBe 2
    }

    /**
     * 전부-아니면-전무로 나누면 코스 상세처럼 여러 자세를 그리는 화면에서 하나만 만료돼도
     * 전체를 다시 받게 되어 캐시가 사실상 안 먹는다.
     */
    @Test
    fun `일부만 캐시에 있으면 없는 것만 delegate 에 넘긴다`() {
        val delegate =
            RecordingPort(
                mapOf(
                    "camel-pose" to "https://cdn/camel.mp4",
                    "wheel-pose" to "https://cdn/wheel.mp4",
                ),
            )
        val caching = CachingPoseVideoAdapter(delegate, ttl, MutableClock(NOW))

        caching.findPlayback(listOf("camel-pose"))
        val result = caching.findPlayback(listOf("camel-pose", "wheel-pose"))

        result.keys shouldBe setOf("camel-pose", "wheel-pose")
        delegate.calls shouldContainExactly listOf(listOf("camel-pose"), listOf("wheel-pose"))
    }

    /**
     * 장애를 TTL 만큼 고정하면 YMove 가 살아난 뒤에도 30 분간 영상이 안 나온다.
     */
    @Test
    fun `못 받은 slug 는 캐시하지 않는다`() {
        val delegate = RecordingPort(emptyMap())
        val caching = CachingPoseVideoAdapter(delegate, ttl, MutableClock(NOW))

        caching.findPlayback(listOf("camel-pose")).shouldBeEmpty()
        caching.findPlayback(listOf("camel-pose")).shouldBeEmpty()

        delegate.calls.size shouldBe 2
    }

    @Test
    fun `slug 가 비어 있으면 delegate 를 부르지 않는다`() {
        val delegate = RecordingPort(emptyMap())
        val caching = CachingPoseVideoAdapter(delegate, ttl, MutableClock(NOW))

        caching.findPlayback(emptyList()).shouldBeEmpty()

        delegate.calls.shouldBeEmptyList()
    }

    private fun List<*>.shouldBeEmptyList() = (size shouldBe 0)

    private class RecordingPort(
        private val urls: Map<String, String>,
    ) : PoseVideoPort {
        val calls = mutableListOf<List<String>>()

        override fun findPlayback(ymoveSlugs: List<String>): Map<String, PoseVideoPlayback> {
            calls += ymoveSlugs
            return ymoveSlugs.mapNotNull { slug -> urls[slug]?.let { slug to PoseVideoPlayback(it) } }.toMap()
        }
    }

    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun instant(): Instant = now

        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?): Clock = this
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-15T00:00:00Z")
    }
}
