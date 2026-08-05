package team.aligner.screening.service

import team.aligner.screening.contract.LatestCauseResponse
import team.aligner.screening.contract.ScreeningResultContract
import team.aligner.screening.infrastructure.ScreeningQueryRepository

/**
 * 계약 구현체는 `internal` 로 대상 도메인 service 에 둔다 (docs/architecture.md §7).
 *
 * `ScreeningQueryService` 를 쓰지 않고 리포지토리를 직접 본다. 그쪽은 진단이 없을 때 404 를
 * 던지는데, `course` 입장에서 "아직 진단 전" 은 예외가 아니라 빈 목록이다. 화면용 서비스의
 * 실패 규약을 계약이 물려받으면 안 된다.
 */
internal class ScreeningResultContractImpl(
    private val screeningQueryRepository: ScreeningQueryRepository,
) : ScreeningResultContract {
    override fun findLatestCauses(memberId: Long): List<LatestCauseResponse> =
        screeningQueryRepository
            .findLatestByMemberId(memberId)
            ?.causes
            // 리포지토리가 rank 순으로 싣지만 계약이 "rank 순" 을 약속하므로 여기서도 고정한다.
            ?.sortedBy { it.rank }
            ?.map {
                LatestCauseResponse(
                    causeCode = it.causeCode,
                    bodyPartCode = it.bodyPartCode,
                    rank = it.rank,
                )
            }
            ?: emptyList()
}
