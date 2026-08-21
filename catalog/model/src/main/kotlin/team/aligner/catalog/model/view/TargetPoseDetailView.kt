package team.aligner.catalog.model.view

/**
 * 목표 자세 상세 화면을 위한 읽기 모델.
 *
 * level 은 부위 안에서의 단계다. 부위별로 1 → 2 → 3 선형이고 분기하지 않는다
 * (docs/domains.md §7-2). YMove 가 요가 콘텐츠 전량을 beginner 로 태깅해 변별력이 없으므로
 * 우리가 감수로 부여한 값이다 (§4-3-1).
 *
 * imageAssetKey 는 URL 이 아니라 안정된 키다. 이미지 파일은 프론트가 정적으로 갖고 서버는
 * 매핑만 내린다 (§4-3). 재생 URL 은 여기 없다.
 */
data class TargetPoseDetailView(
    val targetPoseId: Long,
    val name: String,
    val imageAssetKey: String?,
    /** 영상 포스터 프레임. imageAssetKey 는 키이고 이쪽은 YMove 자산의 URL 이다. */
    val thumbnailUrl: String?,
    /**
     * 같은 자세의 catalog.exercise 행 식별자. **targetPoseId 와 다른 값이다** — 핀포즈는
     * 코스 스텝으로 재생되면서 동시에 코스의 목표라 두 테이블에 각각 행을 갖는다
     * (ddl/002-create-target-pose.sql). 영상·음성 큐·MET 은 exercise 쪽에만 있으므로
     * 재생이 필요한 화면은 이 값을 쓴다.
     *
     * 두 행은 ymove_slug 로 잇는다. slug 가 없거나 짝이 없으면 null 이다.
     */
    val exerciseId: Long?,
    val bodyPartCode: String,
    val level: Int,
    val muscles: List<MuscleView>,
)
