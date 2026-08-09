package team.aligner.member.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.exception.MemberNotFoundException
import team.aligner.member.model.view.MemberProfileView

interface MemberQueryService {
    /** 화면용. 없으면 예외다. */
    fun getProfile(memberIdentity: MemberIdentity): MemberProfileView

    /**
     * 계약용. 없으면 null 이다.
     *
     * MemberBodyContract 가 nullable 을 돌려주기로 정해져 있어(docs/domains.md §3) 필요하다.
     * 예외를 잡아서 null 로 바꾸면 흐름 제어에 예외를 쓰게 되므로 조회를 따로 노출한다 —
     * catalog 의 getDetail·findDetail 과 같은 형태다.
     */
    fun findProfile(memberIdentity: MemberIdentity): MemberProfileView?
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
        findProfile(memberIdentity)
            ?: throw MemberNotFoundException()

    override fun findProfile(memberIdentity: MemberIdentity): MemberProfileView? = memberQueryRepository.findProfile(memberIdentity)
}
