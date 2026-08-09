package team.aligner.member.service

import team.aligner.member.contract.MemberBodyContract
import team.aligner.member.contract.MemberBodyResponse
import team.aligner.member.model.MemberIdentity

/**
 * 계약 구현체. internal 로 대상 도메인 service 에 둔다 (docs/architecture.md §7).
 *
 * 위임만 하고 로직을 두지 않는다. 여기에 판단이 생기면 계약 소비자마다 다른 동작을 하게 된다.
 */
internal class MemberBodyContractImpl(
    private val memberQueryService: MemberQueryService,
) : MemberBodyContract {
    override fun findBody(memberId: Long): MemberBodyResponse? =
        memberQueryService
            .findProfile(MemberIdentity.of(memberId))
            ?.let { MemberBodyResponse(memberId = it.memberId, weightKg = it.weightKg) }
}
