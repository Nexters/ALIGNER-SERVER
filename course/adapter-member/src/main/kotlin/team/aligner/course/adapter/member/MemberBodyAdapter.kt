package team.aligner.course.adapter.member

import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.member.contract.MemberBodyContract

/**
 * `course` 의 port 를 `member:contract` 로 잇는다.
 *
 * 없는 회원과 몸무게 미입력을 **똑같이 null 로** 흘려보낸다. 둘을 구분해봐야 칼로리를
 * 계산할 수 없다는 결론이 같고, 구분이 필요해지면 그때 port 를 넓힌다.
 */
internal class MemberBodyAdapter(
    private val memberBodyContract: MemberBodyContract,
) : MemberBodyPort {
    override fun findWeightKg(memberId: Long): Int? = memberBodyContract.findBody(memberId)?.weightKg
}
