package team.aligner.catalog.infrastructure

import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.model.view.TargetPoseDetailView
import team.aligner.catalog.model.view.TargetPoseSummaryView

/**
 * 읽기 out-port. 화면 하나에 대응하는 조회만 둔다 (docs/architecture.md §4).
 */
interface TargetPoseQueryRepository {
    fun findDetail(targetPoseIdentity: TargetPoseIdentity): TargetPoseDetailView?

    /**
     * 자세 목록. `bodyPartCode` 가 null 이면 전체다.
     *
     * **전체 조회가 온보딩 그리드의 기본이다.** 온보딩이 부위를 먼저 묻지 않고 핀포즈 전체를
     * 펼쳐 보여주므로(docs/domains.md §4-2), 필터는 부위별 자세를 그리는 화면을 위한 것이다.
     *
     * `bodyPartCode` 는 screening 소유 어휘를 값으로 받는다. catalog 가 값 집합을 검증하지
     * 않으므로 모르는 코드에는 빈 목록이 나간다. 그것이 정상이고 예외가 아니다.
     */
    fun findAll(bodyPartCode: String?): List<TargetPoseSummaryView>
}
