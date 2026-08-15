package team.aligner.catalog.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.catalog.model.view.TargetPoseDetailView
import team.aligner.catalog.model.view.TargetPoseSummaryView

/**
 * 자세 상세 응답.
 *
 * imageAssetKey 는 URL 이 아니라 안정된 키다. 파일은 프론트가 정적으로 갖는다
 * (docs/domains.md §4-3).
 */
@Schema(description = "목표 자세 상세")
data class TargetPoseDetailResponse(
    @field:Schema(description = "목표 자세 식별자", example = "1")
    val targetPoseId: Long,
    @field:Schema(description = "자세 이름", example = "다운독")
    val name: String,
    @field:Schema(
        description = "자세 이미지 asset 키. URL 이 아니라 안정된 키이고 이미지 파일은 프론트가 정적으로 갖는다",
        example = "pose/down_dog",
        nullable = true,
    )
    val imageAssetKey: String?,
    @field:Schema(description = "이 자세가 겨냥하는 부위 코드. screening 소유 어휘다")
    val bodyPartCode: BodyPartCode,
    @field:Schema(description = "난이도 단계. 작을수록 쉽다", example = "1")
    val level: Int,
    @field:Schema(description = "이 자세가 쓰는 근육 목록")
    val muscles: List<MuscleResponse>,
) {
    companion object {
        fun from(view: TargetPoseDetailView): TargetPoseDetailResponse =
            TargetPoseDetailResponse(
                targetPoseId = view.targetPoseId,
                name = view.name,
                imageAssetKey = view.imageAssetKey,
                bodyPartCode = BodyPartCode.from(view.bodyPartCode),
                level = view.level,
                muscles = view.muscles.map(MuscleResponse::from),
            )
    }
}

/**
 * 온보딩 자세 그리드 응답. 근육을 싣지 않는다 — 그리드는 이름과 썸네일만 그린다.
 */
@Schema(description = "온보딩 그리드용 목표 자세 요약")
data class TargetPoseSummaryResponse(
    @field:Schema(description = "목표 자세 식별자", example = "1")
    val targetPoseId: Long,
    @field:Schema(description = "자세 이름", example = "다운독")
    val name: String,
    @field:Schema(description = "자세 이미지 asset 키. 파일은 프론트가 정적으로 갖는다", example = "pose/down_dog", nullable = true)
    val imageAssetKey: String?,
    @field:Schema(description = "이 자세가 겨냥하는 부위 코드")
    val bodyPartCode: BodyPartCode,
    @field:Schema(description = "난이도 단계. 작을수록 쉽다", example = "1")
    val level: Int,
) {
    companion object {
        fun from(view: TargetPoseSummaryView): TargetPoseSummaryResponse =
            TargetPoseSummaryResponse(
                targetPoseId = view.targetPoseId,
                name = view.name,
                imageAssetKey = view.imageAssetKey,
                bodyPartCode = BodyPartCode.from(view.bodyPartCode),
                level = view.level,
            )
    }
}
