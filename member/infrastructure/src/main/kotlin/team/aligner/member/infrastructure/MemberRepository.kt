package team.aligner.member.infrastructure

import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity

/**
 * 쓰기 out-port. 애그리거트 단위로만 오간다 (docs/architecture.md §4).
 *
 * findByKakaoId 가 조회처럼 보이지만 여기 있다. 가입·재로그인 분기에서 애그리거트가 필요하고
 * 반환 타입이 Member 이기 때문이다. Command/Query 의 기준은 "무엇을 반환하는가"다.
 *
 * 부분 갱신용 메서드를 만들지 않는다. 수정은 Member 를 통째로 save 한다.
 */
interface MemberRepository {
    fun save(member: Member): Member

    fun findByKakaoId(kakaoId: String): Member?

    fun findByMemberIdentity(memberIdentity: MemberIdentity): Member?
}
