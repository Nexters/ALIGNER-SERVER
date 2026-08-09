package team.aligner.course.adapter.screening

import team.aligner.course.infrastructure.CauseLookup
import team.aligner.course.infrastructure.CauseLookupPort
import team.aligner.screening.contract.ScreeningResultContract

/**
 * `course` 의 port 를 `screening:contract` 로 잇는다. 양쪽 도메인이 서로를 모르고 이 모듈만
 * 둘을 안다 (docs/architecture.md §7).
 *
 * 진단한 적이 없으면 계약이 빈 목록을 돌려주고, 그 판단(409 로 볼지)은 course 가 한다.
 * 여기에 판단을 두면 계약 소비자마다 다른 동작을 하게 된다.
 */
internal class CauseLookupAdapter(
    private val screeningResultContract: ScreeningResultContract,
) : CauseLookupPort {
    override fun findLatestCauses(memberId: Long): List<CauseLookup> =
        screeningResultContract.findLatestCauses(memberId).map {
            CauseLookup(causeCode = it.causeCode, bodyPartCode = it.bodyPartCode, rank = it.rank)
        }
}
