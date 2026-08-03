package team.aligner.member.repository.jdbc

import org.springframework.data.repository.CrudRepository

/**
 * @EnableJdbcRepositories 의 basePackageClasses 기준점이다.
 */
internal interface MemberJdbcRepository : CrudRepository<MemberEntity, Long> {
    fun findByKakaoId(kakaoId: String): MemberEntity?
}
