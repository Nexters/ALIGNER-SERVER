package team.aligner.member.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.exception.MemberNotFoundException
import team.aligner.member.model.view.MemberProfileView

interface MemberQueryService {
    fun getProfile(memberIdentity: MemberIdentity): MemberProfileView
}

/**
 * CommandService 를 주입받지 않는다. Query 는 조회 모델에 직결한다
 * (docs/architecture.md §4).
 *
 * `@Transactional` 을 클래스에 붙이는 이유는 MemberCommandServiceImpl 주석 참고.
 */
@Transactional(readOnly = true)
internal class MemberQueryServiceImpl(
    private val memberQueryRepository: MemberQueryRepository,
) : MemberQueryService {
    override fun getProfile(memberIdentity: MemberIdentity): MemberProfileView =
        memberQueryRepository.findProfile(memberIdentity)
            ?: throw MemberNotFoundException()
}
