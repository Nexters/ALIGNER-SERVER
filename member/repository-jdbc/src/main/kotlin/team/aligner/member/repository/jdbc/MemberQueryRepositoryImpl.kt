package team.aligner.member.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.view.MemberProfileView

/**
 * 조회는 JdbcClient 로 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * SQL 은 schema-qualified 다. FROM member 로 쓰면 public 을 친다 (§6).
 */
internal class MemberQueryRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : MemberQueryRepository {
    override fun findProfile(memberIdentity: MemberIdentity): MemberProfileView? =
        jdbcClient
            .sql(
                """
                SELECT member_id, nickname, profile_image_url
                FROM member.member
                WHERE member_id = :memberId
                """.trimIndent(),
            ).param("memberId", memberIdentity.value)
            .query { rs, _ ->
                MemberProfileView(
                    memberId = rs.getLong("member_id"),
                    nickname = rs.getString("nickname"),
                    profileImageUrl = rs.getString("profile_image_url"),
                )
            }.optional()
            .orElse(null)
}
