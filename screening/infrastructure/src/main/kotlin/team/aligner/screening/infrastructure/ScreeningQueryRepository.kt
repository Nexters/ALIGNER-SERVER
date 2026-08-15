package team.aligner.screening.infrastructure

import team.aligner.screening.model.view.BodyPartView
import team.aligner.screening.model.view.ScreeningResultView

/**
 * 조회 out-port. 구현체가 `JdbcClient` 로 조회 모델을 직접 채운다 (docs/architecture.md §4).
 *
 * 애그리거트를 거치지 않는다. 결과 화면이 원인의 이름·설명까지 필요로 하는데 그건 마스터
 * seed 와의 조인이고, Command 모델에 넣을 것이 아니다.
 */
interface ScreeningQueryRepository {
    /** 부위 목록. `display_order` 순이다. */
    fun findAllBodyParts(): List<BodyPartView>

    /** 회원의 가장 최근 진단. 원인은 `rank` 순으로 실린다. */
    fun findLatestByMemberId(memberId: Long): ScreeningResultView?

    /**
     * 식별자로 하나를 집는다. 제출 직후 그 결과를 그대로 돌려줄 때 쓴다.
     *
     * `memberId` 를 함께 받는 것은 남의 결과를 식별자만으로 읽지 못하게 하기 위해서다.
     * 조건에서 빼면 `resultId` 를 바꿔가며 다른 회원의 진단을 볼 수 있다.
     */
    fun findByIdAndMemberId(
        resultId: Long,
        memberId: Long,
    ): ScreeningResultView?
}
