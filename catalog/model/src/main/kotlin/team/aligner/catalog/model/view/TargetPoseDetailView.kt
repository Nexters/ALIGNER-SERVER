package team.aligner.catalog.model.view

/**
 * 목표 자세 상세 화면을 위한 읽기 모델.
 *
 * level 은 부위 안에서의 단계다. 부위별로 1 → 2 → 3 선형이고 분기하지 않는다
 * (docs/domains.md §7-2). YMove 가 요가 콘텐츠 전량을 beginner 로 태깅해 변별력이 없으므로
 * 우리가 감수로 부여한 값이다 (§4-3-1).
 *
 * imageUrl 은 우리 seed 컬럼이라 YMove 없이도 그릴 수 있다. 재생 URL 은 여기 없다.
 */
data class TargetPoseDetailView(
    val targetPoseId: Long,
    val name: String,
    val imageUrl: String?,
    val bodyPartCode: String,
    val level: Int,
    val muscles: List<MuscleView>,
)
