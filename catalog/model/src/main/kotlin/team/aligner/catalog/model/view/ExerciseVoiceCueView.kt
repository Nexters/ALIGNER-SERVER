package team.aligner.catalog.model.view

/**
 * 세션 재생 중 나가는 음성 큐 하나. 한글 번역본이며 catalog 가 소유한다
 * (docs/domains.md §4-3-1).
 *
 * offsetSeconds 는 타임코드가 확정되기 전까지 null 이다. null 이면 displayOrder 순차 재생으로
 * 읽는다. 확정되면 seed UPDATE 로 값만 채우므로 이 타입은 바뀌지 않는다 (§4-3).
 *
 * ExerciseDetailView 에만 실린다. 목록 조회에 실으면 스텝 수만큼 조인이 붙는다.
 */
data class ExerciseVoiceCueView(
    val displayOrder: Int,
    val offsetSeconds: Int?,
    val content: String,
)
