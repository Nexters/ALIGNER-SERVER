package team.aligner.screening.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.screening.infrastructure.BodyPartRepository
import team.aligner.screening.infrastructure.CauseRuleRepository
import team.aligner.screening.model.CauseRule
import team.aligner.screening.model.PerceivedDifficulty

/**
 * seed 마스터를 읽는 port 구현 둘. 쓰기가 없어 애그리거트도 CrudRepository 도 필요 없다.
 */
internal class BodyPartRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : BodyPartRepository {
    override fun existsByCode(bodyPartCode: String): Boolean =
        jdbcClient
            .sql(
                """
                SELECT EXISTS(
                    SELECT 1 FROM screening.body_part WHERE body_part_code = :bodyPartCode
                )
                """.trimIndent(),
            ).param("bodyPartCode", bodyPartCode)
            .query(Boolean::class.java)
            .single()
}

internal class CauseRuleRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : CauseRuleRepository {
    /**
     * 빈 목록으로 물어보면 `IN ()` 이 되어 SQL 이 깨진다. 호출 자체를 하지 않는다 —
     * 응답이 비는 경우는 애그리거트가 먼저 막지만, port 가 자기 입력을 스스로 지켜야 한다.
     */
    override fun findAllByTargetPoseIds(targetPoseIds: Collection<Long>): List<CauseRule> {
        if (targetPoseIds.isEmpty()) {
            return emptyList()
        }
        return jdbcClient
            .sql(
                """
                SELECT target_pose_id, perceived_difficulty, cause_code, weight
                FROM screening.cause_rule
                WHERE target_pose_id IN (:targetPoseIds)
                """.trimIndent(),
            ).param("targetPoseIds", targetPoseIds)
            .query { rs, _ ->
                CauseRule(
                    targetPoseId = rs.getLong("target_pose_id"),
                    // DDL 의 CHECK 이 값 집합을 강제하므로 valueOf 가 실패하면 스키마가 어긋난 것이다.
                    perceivedDifficulty = PerceivedDifficulty.valueOf(rs.getString("perceived_difficulty")),
                    causeCode = rs.getString("cause_code"),
                    weight = rs.getInt("weight"),
                )
            }.list()
    }
}
