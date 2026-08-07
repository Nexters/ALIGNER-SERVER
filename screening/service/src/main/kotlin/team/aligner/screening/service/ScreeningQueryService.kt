package team.aligner.screening.service

import team.aligner.screening.infrastructure.ScreeningQueryRepository
import team.aligner.screening.model.exception.ScreeningResultNotFoundException
import team.aligner.screening.model.view.BodyPartView
import team.aligner.screening.model.view.ScreeningResultView

interface ScreeningQueryService {
    fun getBodyParts(): List<BodyPartView>

    fun getLatestResult(memberId: Long): ScreeningResultView

    /** 방금 제출한 결과를 그 자리에서 돌려주기 위한 조회. */
    fun getResult(
        memberId: Long,
        resultId: Long,
    ): ScreeningResultView
}

internal class ScreeningQueryServiceImpl(
    private val screeningQueryRepository: ScreeningQueryRepository,
) : ScreeningQueryService {
    override fun getBodyParts(): List<BodyPartView> = screeningQueryRepository.findAllBodyParts()

    /**
     * 진단한 적이 없으면 404 다. 빈 결과를 200 으로 내리지 않는 것은 화면이 "진단 전" 과
     * "원인 0 개" 를 구분해야 하기 때문이다 — 전자는 온보딩으로 보내고 후자는 있을 수 없다
     * (판별이 비면 저장 시점에 422 로 막힌다).
     */
    override fun getLatestResult(memberId: Long): ScreeningResultView =
        screeningQueryRepository.findLatestByMemberId(memberId)
            ?: throw ScreeningResultNotFoundException()

    /**
     * 제출 직후 조회다. "최신" 이 아니라 방금 만든 **식별자로** 집는다 — 같은 회원이 동시에 두 번
     * 제출하면 최신이 다른 요청의 결과일 수 있다.
     *
     * 남의 결과를 읽지 못하도록 `memberId` 를 조건에 함께 넣는다. 없는 식별자와 남의 식별자를
     * 같은 404 로 돌려주는 것도 의도다 — 구분해서 알려주면 존재 여부가 새어나간다.
     */
    override fun getResult(
        memberId: Long,
        resultId: Long,
    ): ScreeningResultView =
        screeningQueryRepository.findByIdAndMemberId(resultId, memberId)
            ?: throw ScreeningResultNotFoundException()
}
