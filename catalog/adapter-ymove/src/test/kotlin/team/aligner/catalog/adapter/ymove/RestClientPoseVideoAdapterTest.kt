package team.aligner.catalog.adapter.ymove

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

/**
 * 실제 YMove 를 치지 않는다. 월 고유 운동 상한이 있어 테스트가 실 API 를 반복해서 부르면 안 된다.
 *
 * 응답 본문은 2026-08-15 에 `GET /exercises/camel-pose` 로 받은 실제 형태를 줄인 것이다.
 * **`data` 봉투는 문서에 없고 실측으로 확인했다** — 손으로 지어낸 형태로 테스트하면
 * 그 사실이 테스트에 남지 않는다.
 */
class RestClientPoseVideoAdapterTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var adapter: RestClientPoseVideoAdapter

    private val properties =
        YmoveProperties(
            baseUrl = BASE_URL,
            apiKey = API_KEY,
            connectTimeoutMillis = 2000,
            readTimeoutMillis = 3000,
            cacheTtlSeconds = 0,
        )

    @BeforeEach
    fun `어댑터를 새로 만든다`() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        adapter = RestClientPoseVideoAdapter(properties, builder.build())
    }

    @Test
    fun `slug 로 재생 URL 을 읽고 API 키를 헤더로 보낸다`() {
        server
            .expect(requestTo("$BASE_URL/exercises/camel-pose"))
            // 쿼리 파라미터(?api_key=)로 보내면 URL 이 액세스 로그·프록시에 남는다. 헤더여야 한다.
            .andExpect(header("X-API-Key", API_KEY))
            .andRespond(withSuccess(playbackBody("https://vz-x.b-cdn.net/v/play_720p.mp4?token=t"), MediaType.APPLICATION_JSON))

        val result = adapter.findPlayback(listOf("camel-pose"))

        result.shouldContainKey("camel-pose")
        result.getValue("camel-pose").videoUrl shouldBe "https://vz-x.b-cdn.net/v/play_720p.mp4?token=t"
        server.verify()
    }

    @Test
    fun `slug 가 비어 있으면 YMove 를 치지 않는다`() {
        adapter.findPlayback(emptyList()).shouldBeEmpty()

        // expect 를 하나도 걸지 않았으므로 호출이 있었다면 verify 가 깨진다.
        server.verify()
    }

    @Test
    fun `중복 slug 는 한 번만 부른다`() {
        server
            .expect(ExpectedCount.once(), requestTo("$BASE_URL/exercises/camel-pose"))
            .andRespond(withSuccess(playbackBody("https://cdn/v.mp4"), MediaType.APPLICATION_JSON))

        adapter.findPlayback(listOf("camel-pose", "camel-pose")).size shouldBe 1

        server.verify()
    }

    /**
     * 우리 seed 가 YMove 보다 앞서갈 수 있다. 없는 slug 는 예외가 아니라 "없음" 이다
     * (docs/domains.md §6).
     */
    @Test
    fun `404 는 예외가 아니라 빈 결과다`() {
        server
            .expect(requestTo("$BASE_URL/exercises/unknown-pose"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("""{"error":"not found"}"""))

        adapter.findPlayback(listOf("unknown-pose")).shouldBeEmpty()
    }

    /**
     * YMove 장애를 502 로 올리지 않는다. videoUrl 이 null 인 화면은 이미 프론트 계약에 있고,
     * 근육맵·음성 큐·주의사항은 우리 DB 라 살아 있다 (docs/domains.md §4-3-1).
     */
    @Test
    fun `5xx 는 예외를 던지지 않고 빈 결과가 된다`() {
        server
            .expect(requestTo("$BASE_URL/exercises/camel-pose"))
            .andRespond(withServerError())

        adapter.findPlayback(listOf("camel-pose")).shouldBeEmpty()
    }

    @Test
    fun `영상 필드가 빠진 응답은 빈 결과다`() {
        // 월 상한을 넘으면 videoUrl 이 null 이 아니라 **키 자체가 사라진다.**
        server
            .expect(requestTo("$BASE_URL/exercises/camel-pose"))
            .andRespond(
                withSuccess(
                    """{"data":{"slug":"camel-pose","title":"Camel pose"},"_warning":{"reason":"monthly_exercise_cap"}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        adapter.findPlayback(listOf("camel-pose")).shouldBeEmpty()
    }

    @Test
    fun `한 자세가 실패해도 나머지는 살아서 돌아온다`() {
        server
            .expect(requestTo("$BASE_URL/exercises/camel-pose"))
            .andRespond(withSuccess(playbackBody("https://cdn/camel.mp4"), MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("$BASE_URL/exercises/wheel-pose"))
            .andRespond(withServerError())

        val result = adapter.findPlayback(listOf("camel-pose", "wheel-pose"))

        result.keys.toList() shouldContainExactly listOf("camel-pose")
    }

    private fun playbackBody(videoUrl: String) =
        """
        {
          "data": {
            "slug": "camel-pose",
            "title": "Camel pose",
            "videoUrl": "$videoUrl",
            "videoHlsUrl": "https://vz-x.b-cdn.net/v/playlist.m3u8?token=t",
            "thumbnailUrl": "https://exercise-api.ymove.app/api/v2/thumbnail/v?library=1",
            "videoDurationSecs": null,
            "videos": [{ "tag": "white-background", "orientation": "portrait", "isPrimary": true }]
          }
        }
        """.trimIndent()

    private companion object {
        const val BASE_URL = "https://exercise-api.ymove.app/api/v2"
        const val API_KEY = "dummy-api-key"
    }
}
