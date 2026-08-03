package team.aligner.catalog.model.view

/**
 * 온보딩 자세 그리드에 쓰는 목표 자세 요약.
 *
 * 자세 그리드는 클라이언트가 catalog API 로 직접 그린다 (docs/domains.md §4-2).
 * 덕분에 screening 이 catalog 를 의존하지 않는다.
 *
 * 근육을 싣지 않는다. 그리드는 이름과 썸네일만 그린다.
 */
data class TargetPoseSummaryView(
    val targetPoseId: Long,
    val name: String,
    val imageUrl: String?,
    val bodyPartCode: String,
    val level: Int,
)
