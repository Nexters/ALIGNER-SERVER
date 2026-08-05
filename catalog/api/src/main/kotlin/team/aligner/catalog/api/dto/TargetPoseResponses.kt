package team.aligner.catalog.api.dto

import team.aligner.catalog.model.view.TargetPoseDetailView
import team.aligner.catalog.model.view.TargetPoseSummaryView

/**
 * 자세 상세 응답.
 *
 * imageAssetKey 는 URL 이 아니라 안정된 키다. 파일은 프론트가 정적으로 갖는다
 * (docs/domains.md §4-3).
 */
data class TargetPoseDetailResponse(
    val targetPoseId: Long,
    val name: String,
    val imageAssetKey: String?,
    val bodyPartCode: String,
    val level: Int,
    val muscles: List<MuscleResponse>,
) {
    companion object {
        fun from(view: TargetPoseDetailView): TargetPoseDetailResponse =
            TargetPoseDetailResponse(
                targetPoseId = view.targetPoseId,
                name = view.name,
                imageAssetKey = view.imageAssetKey,
                bodyPartCode = view.bodyPartCode,
                level = view.level,
                muscles = view.muscles.map(MuscleResponse::from),
            )
    }
}

/**
 * 온보딩 자세 그리드 응답. 근육을 싣지 않는다 — 그리드는 이름과 썸네일만 그린다.
 */
data class TargetPoseSummaryResponse(
    val targetPoseId: Long,
    val name: String,
    val imageAssetKey: String?,
    val bodyPartCode: String,
    val level: Int,
) {
    companion object {
        fun from(view: TargetPoseSummaryView): TargetPoseSummaryResponse =
            TargetPoseSummaryResponse(
                targetPoseId = view.targetPoseId,
                name = view.name,
                imageAssetKey = view.imageAssetKey,
                bodyPartCode = view.bodyPartCode,
                level = view.level,
            )
    }
}
