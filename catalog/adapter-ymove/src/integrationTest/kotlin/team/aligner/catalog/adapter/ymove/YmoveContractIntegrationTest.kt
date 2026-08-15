package team.aligner.catalog.adapter.ymove

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.web.client.RestClient

/**
 * **실제 YMove 를 친다.** 검증 대상이 우리 플로우가 아니라 YMove 계약이다.
 *
 * `YMOVE_API_KEY` 가 없으면 통째로 건너뛴다 — CI 는 키 없이 초록이어야 한다. 로컬에서
 * `YMOVE_API_KEY=<키> ./gradlew :catalog:adapter-ymove:integrationTest` 로 돌린다.
 *
 * **상한을 쓰지 않는다.** 월 고유 운동 상한은 "만져본 운동의 가짓수" 로 세고 같은 운동은 몇 번을
 * 부르든 1 이다. 여기서 쓰는 자세는 seed 에 이미 들어간 29 개 안이라 몇 번을 돌려도 총량이
 * 오르지 않는다. **범위 밖 slug 를 여기에 추가하면 그 전제가 깨진다.**
 *
 * 스텁 테스트(RestClientPoseVideoAdapterTest)가 응답 형태를 고정해 두었으므로, 이 테스트는
 * **그 형태가 실제와 여전히 같은지**만 본다. 둘 중 하나가 깨지면 YMove 가 계약을 바꾼 것이다.
 *
 * 조립은 AutoConfiguration 을 통한다. 그래야 실제 기동과 같은 경로(타임아웃·캐시 데코레이터
 * 포함)를 지나고, internal 구현체에 손대지 않아도 된다.
 */
@EnabledIfEnvironmentVariable(named = "YMOVE_API_KEY", matches = ".+")
class YmoveContractIntegrationTest {
    private val port =
        CatalogYmoveAdapterAutoConfiguration().poseVideoPort(
            YmoveProperties(
                baseUrl = "https://exercise-api.ymove.app/api/v2",
                apiKey = System.getenv("YMOVE_API_KEY"),
                connectTimeoutMillis = 5000,
                readTimeoutMillis = 10000,
                // 캐시를 끈다. 캐시가 켜져 있으면 두 번째 단언이 네트워크가 아니라 맵을 본다.
                cacheTtlSeconds = 0,
            ),
            RestClient.builder(),
        )

    @Test
    fun `실제 YMove 가 재생 URL 을 돌려준다`() {
        val result = port.findPlayback(listOf(CAMEL))

        val playback = result[CAMEL] ?: error("$CAMEL 의 재생 URL 을 받지 못했다")
        // 서명된 Bunny CDN URL 이다. 값 자체는 47 시간 만료라 단언하지 않고 형태만 본다.
        playback.videoUrl shouldStartWith "https://"
        playback.videoUrl shouldContain "expires="
    }

    /**
     * 좌우가 갈리는 자세도 seed 가 고른 쪽 slug 로 실제 응답이 오는지 본다.
     * 호랑이 자세는 left 가 **다른 동작**이라 slug 를 잘못 고르면 잘못된 영상이 재생된다.
     */
    @Test
    fun `seed 가 고른 좌우 slug 가 실제로 존재한다`() {
        val slugs = listOf(TIGER_RIGHT, FIRE_LOG_LEFT)

        port.findPlayback(slugs).keys shouldContainExactlyInAnyOrder slugs
    }

    /**
     * 우리 seed 가 YMove 보다 앞서갈 수 있다. 없는 slug 는 예외가 아니라 빠진다 —
     * 스텁 테스트가 404 로 고정한 동작이 실제 서버에서도 같은지 확인한다.
     */
    @Test
    fun `없는 slug 는 예외가 아니라 빠진다`() {
        val result = port.findPlayback(listOf(CAMEL, "존재하지-않는-자세-slug"))

        result.keys shouldBe setOf(CAMEL)
    }

    private companion object {
        // 전부 catalog seed 의 29 개 안이다. 상한을 새로 쓰지 않는다.
        const val CAMEL = "camel-pose"
        const val TIGER_RIGHT = "tiger-pose-right"
        const val FIRE_LOG_LEFT = "fire-log-pose-left"
    }
}
