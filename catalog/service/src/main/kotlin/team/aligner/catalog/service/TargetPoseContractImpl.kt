package team.aligner.catalog.service

import team.aligner.catalog.contract.TargetPoseContract
import team.aligner.catalog.contract.TargetPoseResponse
import team.aligner.catalog.model.TargetPoseIdentity

/**
 * 계약 구현체. internal 로 대상 도메인 service 에 둔다 (docs/architecture.md §7).
 *
 * 위임과 변환만 하고 판단을 두지 않는다.
 *
 * 없는 자세를 null 로 돌려주는 것은 계약이 그렇게 정해져 있어서다(docs/domains.md §3).
 * 존재 검증이 필요한 쪽은 course 이고, 도메인 간 FK 가 없어 그 판단을 호출부가 한다(§6).
 */
internal class TargetPoseContractImpl(
    private val targetPoseQueryService: TargetPoseQueryService,
) : TargetPoseContract {
    override fun findById(targetPoseId: Long): TargetPoseResponse? =
        targetPoseQueryService
            .findDetail(TargetPoseIdentity.of(targetPoseId))
            ?.let {
                TargetPoseResponse(
                    targetPoseId = it.targetPoseId,
                    name = it.name,
                    imageAssetKey = it.imageAssetKey,
                    thumbnailUrl = it.thumbnailUrl,
                    bodyPartCode = it.bodyPartCode,
                    level = it.level,
                )
            }

    /**
     * 부위 없이 전체를 읽는다. getAll 이 이미 (부위, 레벨, 식별자) 순으로 정렬해 준다.
     */
    override fun findAll(): List<TargetPoseResponse> =
        targetPoseQueryService
            .getAll(null)
            .map {
                TargetPoseResponse(
                    targetPoseId = it.targetPoseId,
                    name = it.name,
                    imageAssetKey = it.imageAssetKey,
                    thumbnailUrl = it.thumbnailUrl,
                    bodyPartCode = it.bodyPartCode,
                    level = it.level,
                )
            }

    /**
     * 부위 목록 조회를 재사용해 레벨로 거른다. 전용 SQL 을 만들지 않는 이유는 한 부위의
     * 자세가 레벨 1~3 로 몇 개뿐이라 걸러낼 양이 작기 때문이다.
     *
     * 둘 이상 걸리면 식별자가 가장 작은 것을 고른다. getAll 이 (부위, 레벨, 식별자) 순으로
     * 정렬해 돌려주므로 first() 가 곧 그 값이다 — 추천이 호출마다 다른 자세를 고르지 않게
     * 하려는 것이다.
     */
    override fun findByBodyPartCodeAndLevel(
        bodyPartCode: String,
        level: Int,
    ): TargetPoseResponse? =
        targetPoseQueryService
            .getAll(bodyPartCode)
            .firstOrNull { it.level == level }
            ?.let {
                TargetPoseResponse(
                    targetPoseId = it.targetPoseId,
                    name = it.name,
                    imageAssetKey = it.imageAssetKey,
                    thumbnailUrl = it.thumbnailUrl,
                    bodyPartCode = it.bodyPartCode,
                    level = it.level,
                )
            }
}
