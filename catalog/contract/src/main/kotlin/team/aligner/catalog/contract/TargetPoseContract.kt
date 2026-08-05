package team.aligner.catalog.contract

/**
 * course 가 catalog 에 요구하는 목표 자세 조회 계약. 통합 전용이라 좁게 만든다
 * (docs/architecture.md §7).
 *
 * 구현체는 internal 로 catalog:service 에 두고 Bean 도 거기서 등록한다.
 *
 * findCheckpoints 가 없다. 자세 포인트를 만들지 않기로 했다 (docs/domains.md §4-3).
 */
interface TargetPoseContract {
    fun findById(targetPoseId: Long): TargetPoseResponse?
}

/**
 * level 은 부위 안에서의 단계다. 부위별로 1 → 2 → 3 선형이고 분기하지 않으므로
 * course.course_template.unlock_required_target_pose_id 한 컬럼으로 사다리가 표현된다
 * (docs/domains.md §7-2).
 *
 * imageAssetKey 는 URL 이 아니라 안정된 키다. 파일은 프론트가 정적으로 갖는다 (§4-3).
 */
data class TargetPoseResponse(
    val targetPoseId: Long,
    val name: String,
    val imageAssetKey: String?,
    val bodyPartCode: String,
    val level: Int,
)
