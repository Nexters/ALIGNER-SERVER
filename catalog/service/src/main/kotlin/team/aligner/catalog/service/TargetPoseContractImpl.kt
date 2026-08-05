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
                    bodyPartCode = it.bodyPartCode,
                    level = it.level,
                )
            }
}
