package team.aligner.catalog.infrastructure

import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.model.view.TargetPoseDetailView
import team.aligner.catalog.model.view.TargetPoseSummaryView

/**
 * 읽기 out-port. 화면 하나에 대응하는 조회만 둔다 (docs/architecture.md §4).
 *
 * 범용 findAll 을 만들지 않는다. 자세 목록이 필요한 화면은 온보딩 그리드 하나이고 그것은
 * 부위로 걸러 온다 (docs/domains.md §4-2).
 */
interface TargetPoseQueryRepository {
    fun findDetail(targetPoseIdentity: TargetPoseIdentity): TargetPoseDetailView?

    /**
     * 온보딩 자세 그리드. bodyPartCode 는 screening 소유 어휘를 값으로 받는다.
     *
     * 해당 부위에 자세가 없으면 빈 목록이 정상이다. 예외가 아니다.
     */
    fun findAllByBodyPartCode(bodyPartCode: String): List<TargetPoseSummaryView>
}
