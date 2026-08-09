package team.aligner.catalog.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.catalog.model.view.MuscleView

/**
 * 하이라이트 키가 **앞·뒤 두 개**다. 세션 플레이어의 근육맵이 인체 앞면과 뒷면을 토글로
 * 보여주고 각각 근육을 칠하므로 어느 쪽 그림에 얹을 키인지가 구분돼야 한다.
 *
 * 둘 다 URL 이 아니라 안정된 키다. 근육맵 이미지 파일은 프론트가 정적으로 갖고 서버는
 * 매핑만 내린다 (docs/domains.md §4-3).
 *
 * role 은 STRETCH(신장) 또는 STRENGTHEN(강화) 문자열로 나간다.
 */
@Schema(description = "운동·자세가 쓰는 근육 하나")
data class MuscleResponse(
    @field:Schema(description = "근육 코드", example = "UPPER_TRAPEZIUS")
    val muscleCode: String,
    @field:Schema(description = "표시용 근육 이름", example = "상부 승모근")
    val name: String,
    @field:Schema(description = "이 근육이 속한 부위 코드", example = "NECK")
    val bodyPartCode: String,
    @field:Schema(
        description = "앞쪽 근육맵 하이라이트 asset 키. 뒤에만 보이는 근육이면 null 이다. URL 이 아니라 안정된 키다",
        example = "muscle/upper_trapezius_front",
        nullable = true,
    )
    val frontHighlightAssetKey: String?,
    @field:Schema(
        description = "뒤쪽 근육맵 하이라이트 asset 키. 앞에만 보이는 근육이면 null 이다",
        example = "muscle/upper_trapezius_back",
        nullable = true,
    )
    val backHighlightAssetKey: String?,
    @field:Schema(description = "이 근육을 어떻게 쓰는가", allowableValues = ["STRETCH", "STRENGTHEN"], example = "STRETCH")
    val role: String,
    @field:Schema(description = "화면 표시 순서. 작을수록 먼저다", example = "1")
    val displayOrder: Int,
) {
    companion object {
        fun from(view: MuscleView): MuscleResponse =
            MuscleResponse(
                muscleCode = view.muscleCode,
                name = view.name,
                bodyPartCode = view.bodyPartCode,
                frontHighlightAssetKey = view.frontHighlightAssetKey,
                backHighlightAssetKey = view.backHighlightAssetKey,
                role = view.role.name,
                displayOrder = view.displayOrder,
            )
    }
}
