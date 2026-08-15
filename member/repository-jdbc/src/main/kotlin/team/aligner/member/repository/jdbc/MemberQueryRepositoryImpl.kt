package team.aligner.member.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.model.ExperienceLevel
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.view.MemberProfileView
import java.sql.ResultSet

/**
 * 조회는 JdbcClient 로 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * SQL 은 schema-qualified 다. FROM member 로 쓰면 public 을 친다 (§6).
 */
internal class MemberQueryRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : MemberQueryRepository {
    /**
     * **탈퇴한 회원은 없는 것으로 본다.** 탈퇴가 행을 지우지 않으므로 조건이 필요하다.
     * 부분 인덱스 ix_member_active 가 이 조건을 받는다.
     */
    override fun findProfile(memberIdentity: MemberIdentity): MemberProfileView? =
        jdbcClient
            .sql(
                """
                SELECT member_id, nickname, profile_image_url,
                       height_cm, weight_kg, experience_level,
                       reinforcement_body_part_code, reinforcement_level
                FROM member.member
                WHERE member_id = :memberId AND withdrawn_at IS NULL
                """.trimIndent(),
            ).param("memberId", memberIdentity.value)
            .query { rs, _ ->
                MemberProfileView(
                    memberId = rs.getLong("member_id"),
                    nickname = rs.getString("nickname"),
                    profileImageUrl = rs.getString("profile_image_url"),
                    heightCm = rs.getIntOrNull("height_cm"),
                    weightKg = rs.getIntOrNull("weight_kg"),
                    experienceLevel = rs.getString("experience_level")?.let { ExperienceLevel.valueOf(it) },
                    reinforcementBodyPartCode = rs.getString("reinforcement_body_part_code"),
                    reinforcementLevel = rs.getIntOrNull("reinforcement_level"),
                )
            }.optional()
            .orElse(null)
}

/**
 * ResultSet.getInt 는 NULL 을 0 으로 돌려준다. 키·몸무게에서 0 과 "입력 안 함" 이 같은 값이
 * 되면 온보딩 미완료 판정이 깨지므로 wasNull 로 구분한다.
 */
private fun ResultSet.getIntOrNull(columnLabel: String): Int? {
    val value = getInt(columnLabel)
    return if (wasNull()) null else value
}
