package team.aligner.screening.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.screening.infrastructure.ScreeningQueryRepository
import team.aligner.screening.model.view.BodyPartView
import team.aligner.screening.model.view.ScreeningCauseView
import team.aligner.screening.model.view.ScreeningResultView

/**
 * 조회는 JdbcClient 로 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * SQL 은 schema-qualified 다 (§6).
 */
internal class ScreeningQueryRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : ScreeningQueryRepository {
    override fun findAllBodyParts(): List<BodyPartView> =
        jdbcClient
            .sql(
                """
                SELECT body_part_code, name
                FROM screening.body_part
                ORDER BY display_order
                """.trimIndent(),
            ).query { rs, _ ->
                BodyPartView(
                    bodyPartCode = rs.getString("body_part_code"),
                    name = rs.getString("name"),
                )
            }.list()

    override fun findLatestByMemberId(memberId: Long): ScreeningResultView? {
        val base =
            jdbcClient
                .sql(
                    """
                    SELECT result_id, perceived_body_part_code, created_at
                    FROM screening.screening_result
                    WHERE member_id = :memberId
                    ORDER BY created_at DESC, result_id DESC
                    LIMIT 1
                    """.trimIndent(),
                ).param("memberId", memberId)
                .query(ScreeningResultRowMapper)
                .optional()
                .orElse(null) ?: return null

        return base.copy(causes = findCauses(base.resultId))
    }

    override fun findByIdAndMemberId(
        resultId: Long,
        memberId: Long,
    ): ScreeningResultView? {
        val base =
            jdbcClient
                .sql(
                    """
                    SELECT result_id, perceived_body_part_code, created_at
                    FROM screening.screening_result
                    WHERE result_id = :resultId AND member_id = :memberId
                    """.trimIndent(),
                ).param("resultId", resultId)
                .param("memberId", memberId)
                .query(ScreeningResultRowMapper)
                .optional()
                .orElse(null) ?: return null

        return base.copy(causes = findCauses(base.resultId))
    }

    /**
     * 원인 이름·설명은 마스터 seed 와의 조인이다. 애그리거트에는 `cause_code` 만 있어서
     * 결과 화면을 그리려면 여기서 붙여야 한다.
     *
     * `bodyPartCode` 는 **원인이 있는 부위**다. `screening_result.perceived_body_part_code`
     * (회원이 고른 부위)와 다를 수 있고, 다른 것이 이 도메인의 요점이다.
     */
    private fun findCauses(resultId: Long): List<ScreeningCauseView> =
        jdbcClient
            .sql(
                """
                SELECT sc.cause_code, c.name, c.body_part_code, c.description, sc.rank, sc.score
                FROM screening.screening_cause sc
                JOIN screening.cause c ON c.cause_code = sc.cause_code
                WHERE sc.result_id = :resultId
                ORDER BY sc.rank
                """.trimIndent(),
            ).param("resultId", resultId)
            .query { rs, _ ->
                ScreeningCauseView(
                    causeCode = rs.getString("cause_code"),
                    name = rs.getString("name"),
                    bodyPartCode = rs.getString("body_part_code"),
                    description = rs.getString("description"),
                    rank = rs.getInt("rank"),
                    score = rs.getInt("score"),
                )
            }.list()
}

/**
 * 두 조회가 같은 컬럼을 읽는다. 매퍼를 공유해 한쪽만 컬럼이 늘어나는 어긋남을 막는다.
 * `causes` 는 호출부가 채운다.
 */
private object ScreeningResultRowMapper : org.springframework.jdbc.core.RowMapper<ScreeningResultView> {
    override fun mapRow(
        rs: java.sql.ResultSet,
        rowNum: Int,
    ): ScreeningResultView =
        ScreeningResultView(
            resultId = rs.getLong("result_id"),
            perceivedBodyPartCode = rs.getString("perceived_body_part_code"),
            causes = emptyList(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}
