package team.aligner.catalog.model.view

/**
 * 세션 재생 중 나가는 음성 큐 하나. 한글 번역본이며 catalog 가 소유한다
 * (docs/domains.md §4-3-1).
 *
 * 큐는 순간이 아니라 구간이다. 핀포즈가 "40 초 × 3" 처럼 유지 구간을 갖고 세션 플레이어가
 * 카운트다운을 그리므로 끝나는 시각이 화면에 필요하다.
 *
 * 두 offset 은 타임코드가 확정되기 전까지 null 이다. null 이면 displayOrder 순차 재생으로
 * 읽는다. 확정되면 seed UPDATE 로 값만 채우므로 이 타입은 바뀌지 않는다 (§4-3).
 * endOffsetSeconds 는 유지 구간이 없는 큐에서 null 로 남는다.
 *
 * ExerciseDetailView 에만 실린다. 목록 조회에 실으면 스텝 수만큼 조인이 붙는다.
 */
data class ExerciseVoiceCueView(
    val displayOrder: Int,
    val startOffsetSeconds: Int?,
    val endOffsetSeconds: Int?,
    val content: String,
)
