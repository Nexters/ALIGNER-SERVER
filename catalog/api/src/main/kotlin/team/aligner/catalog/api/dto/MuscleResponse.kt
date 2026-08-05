package team.aligner.catalog.api.dto

import team.aligner.catalog.model.view.MuscleView

/**
 * highlightAssetKey 는 URL 이 아니라 안정된 키다. 근육맵 이미지 파일은 프론트가 정적으로
 * 갖고 서버는 매핑만 내린다 (docs/domains.md §4-3).
 *
 * role 은 STRETCH(신장) 또는 STRENGTHEN(강화) 문자열로 나간다.
 */
data class MuscleResponse(
    val muscleCode: String,
    val name: String,
    val bodyPartCode: String,
    val highlightAssetKey: String?,
    val role: String,
    val displayOrder: Int,
) {
    companion object {
        fun from(view: MuscleView): MuscleResponse =
            MuscleResponse(
                muscleCode = view.muscleCode,
                name = view.name,
                bodyPartCode = view.bodyPartCode,
                highlightAssetKey = view.highlightAssetKey,
                role = view.role.name,
                displayOrder = view.displayOrder,
            )
    }
}
