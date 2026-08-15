package team.aligner.catalog.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.catalog.infrastructure.TargetPoseQueryRepository
import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.model.exception.TargetPoseNotFoundException
import team.aligner.catalog.model.view.TargetPoseDetailView
import team.aligner.catalog.model.view.TargetPoseSummaryView

interface TargetPoseQueryService {
    /** 화면용. 없으면 예외다. */
    fun getDetail(targetPoseIdentity: TargetPoseIdentity): TargetPoseDetailView

    /**
     * 계약용. 없으면 null 이다.
     *
     * TargetPoseContract 가 nullable 을 돌려주기로 정해져 있어(docs/domains.md §3) 필요하다.
     * 예외를 잡아서 null 로 바꾸면 흐름 제어에 예외를 쓰게 되므로 조회를 따로 노출한다.
     */
    fun findDetail(targetPoseIdentity: TargetPoseIdentity): TargetPoseDetailView?

    /** 자세 목록. `bodyPartCode` 가 null 이면 온보딩 그리드용 전체다. */
    fun getAll(bodyPartCode: String?): List<TargetPoseSummaryView>
}

/**
 * `@Transactional` 을 클래스에 붙이는 이유는 ExerciseQueryServiceImpl 주석 참고.
 */
@Transactional(readOnly = true)
internal class TargetPoseQueryServiceImpl(
    private val targetPoseQueryRepository: TargetPoseQueryRepository,
) : TargetPoseQueryService {
    override fun getDetail(targetPoseIdentity: TargetPoseIdentity): TargetPoseDetailView =
        findDetail(targetPoseIdentity)
            ?: throw TargetPoseNotFoundException()

    override fun findDetail(targetPoseIdentity: TargetPoseIdentity): TargetPoseDetailView? =
        targetPoseQueryRepository.findDetail(targetPoseIdentity)

    /**
     * 자세가 없으면 빈 목록이 정상이다. 예외를 던지지 않는다.
     *
     * bodyPartCode 는 screening 소유 어휘라 catalog 가 값 집합을 검증하지 않는다. 모르는 코드가
     * 들어오면 빈 목록이 나간다 (docs/domains.md §4-2).
     */
    override fun getAll(bodyPartCode: String?): List<TargetPoseSummaryView> = targetPoseQueryRepository.findAll(bodyPartCode)
}
