package team.aligner.member.infrastructure

import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.view.MemberProfileView

/**
 * 읽기 out-port. 화면 하나에 대응하는 조회만 둔다 (docs/architecture.md §4).
 *
 * 범용 조회 메서드를 만들지 않는다. 화면이 늘면 그때 메서드를 늘린다.
 */
interface MemberQueryRepository {
    fun findProfile(memberIdentity: MemberIdentity): MemberProfileView?
}
