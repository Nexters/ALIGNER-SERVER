package team.aligner.catalog.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.catalog.infrastructure.TargetPoseQueryRepository
import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.model.view.TargetPoseDetailView
import team.aligner.catalog.model.view.TargetPoseSummaryView

/**
 * 조회는 JdbcClient 로 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * SQL 은 schema-qualified 다 (§6).
 */
internal class TargetPoseQueryRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : TargetPoseQueryRepository {
    /** 본체 1 쿼리 + 근육 1 쿼리. 자세는 음성 큐를 갖지 않아 2 회면 끝난다. */
    override fun findDetail(targetPoseIdentity: TargetPoseIdentity): TargetPoseDetailView? {
        val targetPoseId = targetPoseIdentity.value
        val base =
            jdbcClient
                .sql(
                    """
                    SELECT target_pose_id, name, image_asset_key, body_part_code, level
                    FROM catalog.target_pose
                    WHERE target_pose_id = :targetPoseId
                    """.trimIndent(),
                ).param("targetPoseId", targetPoseId)
                .query { rs, _ ->
                    TargetPoseDetailView(
                        targetPoseId = rs.getLong("target_pose_id"),
                        name = rs.getString("name"),
                        imageAssetKey = rs.getString("image_asset_key"),
                        bodyPartCode = rs.getString("body_part_code"),
                        level = rs.getInt("level"),
                        muscles = emptyList(),
                    )
                }.optional()
                .orElse(null) ?: return null

        return base.copy(muscles = findMuscles(targetPoseId))
    }

    /** 온보딩 자세 그리드. 근육을 싣지 않아 1 쿼리다. */
    override fun findAllByBodyPartCode(bodyPartCode: String): List<TargetPoseSummaryView> =
        jdbcClient
            .sql(
                """
                SELECT target_pose_id, name, image_asset_key, body_part_code, level
                FROM catalog.target_pose
                WHERE body_part_code = :bodyPartCode
                ORDER BY level, target_pose_id
                """.trimIndent(),
            ).param("bodyPartCode", bodyPartCode)
            .query { rs, _ ->
                TargetPoseSummaryView(
                    targetPoseId = rs.getLong("target_pose_id"),
                    name = rs.getString("name"),
                    imageAssetKey = rs.getString("image_asset_key"),
                    bodyPartCode = rs.getString("body_part_code"),
                    level = rs.getInt("level"),
                )
            }.list()

    private fun findMuscles(targetPoseId: Long) =
        jdbcClient
            .sql(
                """
                SELECT m.muscle_code, m.name, m.body_part_code, m.highlight_asset_key,
                       pm.role, pm.display_order
                FROM catalog.pose_muscle pm
                JOIN catalog.muscle m ON m.muscle_code = pm.muscle_code
                WHERE pm.target_pose_id = :targetPoseId
                ORDER BY pm.display_order
                """.trimIndent(),
            ).param("targetPoseId", targetPoseId)
            .query(MuscleViewRowMapper)
            .list()
}
