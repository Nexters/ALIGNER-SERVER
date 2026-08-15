package team.aligner.catalog.infrastructure

/**
 * YMove 에서 재생 URL 을 읽는 out-port (docs/domains.md §4-3-1).
 *
 * **여기서 얻는 것은 `videoUrl` 하나뿐이다.** 나머지는 전부 다른 곳이 갖는다.
 * - 음성 큐 대본 — 우리가 번역해 `catalog.exercise_voice_cue` 로 소유한다
 * - 썸네일 — YMove 의 `thumbnailUrl` 은 서명도 만료도 없어 `catalog.exercise` 컬럼에 저장한다
 * - `videoDurationSecs` — YMove 가 `null` 로 준다. 실측으로 확인했다
 *
 * 목록 조회에서 자세마다 따로 부르지 않도록 **slug 리스트를 한 번에 받는다.** YMove 가 월 고유
 * 운동 상한을 걸고 있어 성능뿐 아니라 과금 경계이기도 하다.
 *
 * **없는 slug 는 조용히 빠진다.** 예외를 던지지 않는 것은 ExerciseContract.findAllByIds 와
 * 같은 이유다 — 우리 seed 가 YMove 보다 앞서갈 수 있다.
 *
 * **YMove 장애도 빈 맵이다.** 호출부는 장애와 "그 slug 가 없음" 을 구분하지 않는다. 어느 쪽이든
 * 화면은 `videoUrl = null` 로 그려지고, 그 화면은 이미 프론트 계약에 있다.
 */
interface PoseVideoPort {
    fun findPlayback(ymoveSlugs: List<String>): Map<String, PoseVideoPlayback>
}

/**
 * 필드가 하나뿐인데도 타입을 두는 이유는 port 시그니처에서 `Map<String, String>` 이 "slug →
 * 무엇" 인지 말해주지 않기 때문이다. HLS 재생 URL(`videoHlsUrl`)이 붙을 자리이기도 하다 —
 * 지금 만들지 않는 것은 소비자가 없어서다 (docs/architecture.md §3).
 */
data class PoseVideoPlayback(
    val videoUrl: String,
)
