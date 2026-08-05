package team.aligner.catalog.model.view

import team.aligner.catalog.model.MuscleRole

/**
 * 자세·운동에 딸린 근육 하나. 운동 가이드의 부위 탭과 근육맵에 쓴다.
 *
 * highlightAssetKey 는 근육맵 이미지 식별자다. 실제 파일은 정적 asset 이고 DB 에는 키만
 * 둔다 (docs/domains.md §4-3).
 *
 * bodyPartCode 는 screening 소유 어휘를 값으로 받은 것이다. catalog 에 타입을 만들지 않는다.
 */
data class MuscleView(
    val muscleCode: String,
    val name: String,
    val bodyPartCode: String,
    val highlightAssetKey: String?,
    val role: MuscleRole,
    val displayOrder: Int,
)
