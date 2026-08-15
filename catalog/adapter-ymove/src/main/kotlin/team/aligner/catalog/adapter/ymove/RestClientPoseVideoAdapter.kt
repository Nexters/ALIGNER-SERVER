package team.aligner.catalog.adapter.ymove

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient
import team.aligner.catalog.infrastructure.PoseVideoPlayback
import team.aligner.catalog.infrastructure.PoseVideoPort

/**
 * YMove Exercise API 로 재생 URL 을 읽는다.
 *
 * **slug 마다 `GET /exercises/{slug}` 를 한 번씩 부른다.** 특정 slug 여러 개를 한 요청으로 받는
 * 방법이 API 에 없다 — 목록(`GET /exercises`)은 `exerciseType` 같은 분류 필터만 받고, 거기에
 * `includeVideos=true` 를 걸면 **응답에 담긴 운동 전부가 월 상한에 카운트된다.** 요가 전량이
 * 84 개라 우리가 쓰지 않는 47 개까지 상한을 먹는다. 그래서 단건 조회 N 번이 더 싸다.
 *
 * 호출 수가 아니라 **만져본 운동의 가짓수**가 과금 단위이고(같은 운동은 몇 번을 부르든 1),
 * 우리 자세는 고정 집합이라 N 번 부르는 것 자체는 상한을 올리지 않는다. HTTP 왕복은
 * CachingPoseVideoAdapter 가 흡수한다.
 *
 * 응답을 DTO 가 아니라 Map 으로 읽는다. 실제로 뽑는 값이 `data.videoUrl` 하나뿐이고, 이 모듈은
 * aligner.kotlin-boot 이라 jackson-module-kotlin·kotlin-reflect 가 없다. data class 로 받으려면
 * 의존성 둘을 더해야 하는데 (build-logic/aligner.boot-mvc 참고) 얻는 것에 비해 비싸다.
 * RestClientKakaoUserClient 가 같은 판단을 했다.
 */
internal class RestClientPoseVideoAdapter(
    private val properties: YmoveProperties,
    private val restClient: RestClient,
) : PoseVideoPort {
    override fun findPlayback(ymoveSlugs: List<String>): Map<String, PoseVideoPlayback> {
        // 빈 리스트에 HTTP 를 쏘지 않는다. ExerciseQueryServiceImpl.getAll 이 빈 리스트에
        // DB 를 치지 않는 것과 같은 규율이다.
        if (ymoveSlugs.isEmpty()) {
            return emptyMap()
        }

        return ymoveSlugs
            .distinct()
            .mapNotNull { slug -> fetch(slug)?.let { slug to it } }
            .toMap()
    }

    /**
     * 찾지 못했거나 YMove 가 죽었으면 `null` 이다. 호출부는 둘을 구분하지 않는다 —
     * 어느 쪽이든 화면은 `videoUrl = null` 로 그려진다 (PoseVideoPort 주석).
     */
    private fun fetch(slug: String): PoseVideoPlayback? {
        val body =
            try {
                restClient
                    .get()
                    .uri("${properties.baseUrl}$EXERCISE_PATH$slug")
                    .header(API_KEY_HEADER, properties.apiKey)
                    .retrieve()
                    // 404 는 우리 seed 가 YMove 보다 앞서간 것이다. 예외가 아니라 없음이다.
                    // 빈 핸들러가 곧 "던지지 않는다" 이고, 그 뒤 data 가 없어 null 이 된다.
                    .onStatus({ it.value() == NOT_FOUND_STATUS }) { _, _ -> }
                    .body(object : ParameterizedTypeReference<Map<String, Any?>>() {})
            } catch (exception: RuntimeException) {
                // 타임아웃·연결 실패·5xx·역직렬화 실패를 전부 여기서 접는다. 조용히 null 이
                // 되면 연동이 죽은 것을 아무도 모르므로 WARN 을 남긴다.
                log.warn("YMove 재생 URL 조회 실패. slug={}", slug, exception)
                return null
            } ?: return null

        warnIfMonthlyCapExceeded(slug, body)

        @Suppress("UNCHECKED_CAST")
        val data = body[DATA_FIELD] as? Map<String, Any?> ?: return null
        val videoUrl = data[VIDEO_URL_FIELD] as? String ?: return null

        return PoseVideoPlayback(videoUrl = videoUrl)
    }

    /**
     * 월 고유 운동 상한을 넘으면 **새 운동에서 영상 필드가 통째로 빠진다.** 이미 조회한 운동은
     * URL 을 유지하므로 증상이 자세마다 다르게 나타난다.
     *
     * 장애가 아니라 과금 신호라 실패 로그와 같은 자리에 묻히면 안 된다.
     */
    private fun warnIfMonthlyCapExceeded(
        slug: String,
        body: Map<String, Any?>,
    ) {
        @Suppress("UNCHECKED_CAST")
        val reason = (body[WARNING_FIELD] as? Map<String, Any?>)?.get(REASON_FIELD)
        if (reason == MONTHLY_EXERCISE_CAP) {
            log.warn("YMove 월 고유 운동 상한을 넘었다. 새 자세의 영상이 내려오지 않는다. slug={}", slug)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(RestClientPoseVideoAdapter::class.java)

        const val EXERCISE_PATH = "/exercises/"
        const val API_KEY_HEADER = "X-API-Key"
        const val NOT_FOUND_STATUS = 404

        // 목록·단건 모두 응답이 `data` 봉투에 싸여 온다. 문서에 없고 실측으로 확인했다.
        const val DATA_FIELD = "data"
        const val VIDEO_URL_FIELD = "videoUrl"
        const val WARNING_FIELD = "_warning"
        const val REASON_FIELD = "reason"
        const val MONTHLY_EXERCISE_CAP = "monthly_exercise_cap"
    }
}
