package team.aligner.member.repository.jdbc

import org.springframework.data.repository.CrudRepository

/**
 * @EnableJdbcRepositories 의 basePackageClasses 기준점이다.
 *
 * 두 조회 모두 **탈퇴하지 않은 회원만** 집는다. 탈퇴는 행을 지우지 않으므로(Member.withdraw)
 * 조건을 빼면 탈퇴 회원이 그대로 조회된다. 리프레시 토큰이 없어 발급된 JWT 를 회수할 수단이
 * 없으니, 탈퇴 직후에도 남아 있는 토큰이 이 조회에서 걸러져야 한다.
 *
 * findByKakaoId 는 kakao_id 가 NULL 이 되는 것만으로도 탈퇴 회원을 비껴가지만 조건을 함께
 * 적는다. 두 조회가 "이용 중인 회원" 이라는 같은 뜻을 갖는 편이 읽기 쉽다.
 */
internal interface MemberJdbcRepository : CrudRepository<MemberEntity, Long> {
    fun findByKakaoIdAndWithdrawnAtIsNull(kakaoId: String): MemberEntity?

    fun findByMemberIdAndWithdrawnAtIsNull(memberId: Long): MemberEntity?
}
