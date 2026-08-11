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

    /**
     * 부위와 레벨로 목표 자세를 찾는다. course 의 처방 입력이 (강화 부위, 난이도)이고
     * **난이도가 곧 레벨**이라 이 조회가 처방의 첫 걸음이다 (docs/domains.md §4-4).
     *
     * 둘 이상 걸리면 `targetPoseId` 가 가장 작은 것을 돌려준다. catalog 스키마가
     * (부위, 레벨) 유니크를 강제하지 않아 이론상 여러 개가 나올 수 있는데, 처방이
     * 호출마다 다른 자세를 고르면 안 되므로 순서를 고정한다.
     */
    fun findByBodyPartCodeAndLevel(
        bodyPartCode: String,
        level: Int,
    ): TargetPoseResponse?
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
