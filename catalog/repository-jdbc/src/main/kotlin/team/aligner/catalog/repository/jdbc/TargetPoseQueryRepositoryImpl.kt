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
                    SELECT tp.target_pose_id, tp.name, tp.image_asset_key, tp.thumbnail_url,
                           tp.body_part_code, tp.level,
                           -- 같은 자세의 exercise 행. 두 테이블이 ymove_slug 를 공유한다
                           -- (seed/008 이 썸네일에 쓴 대응과 같다). LEFT JOIN 인 것은 slug 가
                           -- 없거나 짝이 아직 없는 자세가 정상 경로이기 때문이다.
                           e.exercise_id
                    FROM catalog.target_pose tp
                    LEFT JOIN catalog.exercise e ON e.ymove_slug = tp.ymove_slug
                    WHERE tp.target_pose_id = :targetPoseId
                    """.trimIndent(),
                ).param("targetPoseId", targetPoseId)
                .query { rs, _ ->
                    TargetPoseDetailView(
                        targetPoseId = rs.getLong("target_pose_id"),
                        name = rs.getString("name"),
                        imageAssetKey = rs.getString("image_asset_key"),
                        thumbnailUrl = rs.getString("thumbnail_url"),
                        exerciseId = rs.getLongOrNull("exercise_id"),
                        bodyPartCode = rs.getString("body_part_code"),
                        level = rs.getInt("level"),
                        muscles = emptyList(),
                    )
                }.optional()
                .orElse(null) ?: return null

        return base.copy(muscles = findMuscles(targetPoseId))
    }

    /**
     * 온보딩 자세 그리드. 근육을 싣지 않아 1 쿼리다.
     *
     * 필터를 문자열 결합으로 붙이지 않고 SQL 안에서 끈다. 쿼리가 하나로 유지되고 파라미터
     * 바인딩이 그대로라 주입 여지가 없다.
     *
     * `CAST(... AS VARCHAR)` 가 필요하다. 이름 파라미터는 결국 `?` 로 바뀌는데, PostgreSQL 은
     * `? IS NULL` 처럼 타입 단서가 없는 자리에서 "could not determine data type" 으로 거절한다.
     * null 을 넘기는 순간에만 터지므로 전체 조회에서만 드러난다.
     */
    override fun findAll(bodyPartCode: String?): List<TargetPoseSummaryView> =
        jdbcClient
            .sql(
                """
                SELECT tp.target_pose_id, tp.name, tp.image_asset_key, tp.thumbnail_url,
                       tp.body_part_code, tp.level,
                       -- 같은 자세의 exercise 행. 두 테이블이 ymove_slug 를 공유한다
                       -- (seed/008 이 썸네일에 쓴 대응과 같다). LEFT JOIN 인 것은 slug 가
                       -- 없거나 짝이 아직 없는 자세가 정상 경로이기 때문이다.
                       e.exercise_id
                FROM catalog.target_pose tp
                LEFT JOIN catalog.exercise e ON e.ymove_slug = tp.ymove_slug
                WHERE (CAST(:bodyPartCode AS VARCHAR) IS NULL OR tp.body_part_code = CAST(:bodyPartCode AS VARCHAR))
                ORDER BY tp.body_part_code, tp.level, tp.target_pose_id
                """.trimIndent(),
            ).param("bodyPartCode", bodyPartCode)
            .query { rs, _ ->
                TargetPoseSummaryView(
                    targetPoseId = rs.getLong("target_pose_id"),
                    name = rs.getString("name"),
                    imageAssetKey = rs.getString("image_asset_key"),
                    thumbnailUrl = rs.getString("thumbnail_url"),
                    exerciseId = rs.getLongOrNull("exercise_id"),
                    bodyPartCode = rs.getString("body_part_code"),
                    level = rs.getInt("level"),
                )
            }.list()

    private fun findMuscles(targetPoseId: Long) =
        jdbcClient
            .sql(
                """
                SELECT m.muscle_code, m.name, m.body_part_code,
                       m.front_highlight_asset_key, m.back_highlight_asset_key,
                       pm.role, pm.display_order,
                       -- 자세에는 핵심 동작 문구가 없다. 운동과 매퍼를 공유하므로 같은
                       -- 이름으로 NULL 을 내보낸다.
                       NULL::text AS description
                FROM catalog.pose_muscle pm
                JOIN catalog.muscle m ON m.muscle_code = pm.muscle_code
                WHERE pm.target_pose_id = :targetPoseId
                ORDER BY pm.display_order
                """.trimIndent(),
            ).param("targetPoseId", targetPoseId)
            .query(MuscleViewRowMapper)
            .list()
}
