package team.aligner.catalog.repository.jdbc

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseSummaryView
import team.aligner.catalog.model.view.ExerciseVoiceCueView

/**
 * 조회는 JdbcClient 로 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * 엔티티도 CrudRepository 도 없다. catalog 는 쓰기가 없어 Spring Data JDBC 매핑이 필요한
 * 자리가 아예 없다 (docs/domains.md §4-3). 덕분에 pose_muscle 같은 복합키 자식 테이블을
 * 애그리거트로 감쌀 필요도 없다.
 *
 * SQL 은 schema-qualified 다. FROM exercise 로 쓰면 public 을 친다 (§6).
 */
internal class ExerciseQueryRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : ExerciseQueryRepository {
    /**
     * 본체·근육·음성 큐를 3 회로 나눠 읽는다.
     *
     * 한 번에 조인하면 근육 n 개 × 큐 m 개의 카티션 곱이 나온다. 자식이 둘이라 조인으로는
     * 중복 제거 비용이 더 크다.
     */
    override fun findDetail(exerciseIdentity: ExerciseIdentity): ExerciseDetailView? {
        val exerciseId = exerciseIdentity.value
        val base =
            jdbcClient
                .sql(
                    """
                    SELECT exercise_id, name, image_asset_key, thumbnail_url,
                           default_set_count, default_rep_count,
                           default_duration_seconds, met_value, difficulty, category, caution_note
                    FROM catalog.exercise
                    WHERE exercise_id = :exerciseId
                    """.trimIndent(),
                ).param("exerciseId", exerciseId)
                .query { rs, _ ->
                    ExerciseDetailView(
                        exerciseId = rs.getLong("exercise_id"),
                        name = rs.getString("name"),
                        imageAssetKey = rs.getString("image_asset_key"),
                        // video_url 컬럼을 더 이상 읽지 않는다. 재생 URL 은 48 시간 만료라 DB 에
                        // 담을 수 없고 adapter-ymove 가 요청 시점에 채운다 (docs/domains.md §4-3-1).
                        // 컬럼 자체는 남겨둔다 — 연동이 도는 것을 확인한 뒤 드롭한다.
                        videoUrl = null,
                        thumbnailUrl = rs.getString("thumbnail_url"),
                        defaultSetCount = rs.getIntOrNull("default_set_count"),
                        defaultRepCount = rs.getIntOrNull("default_rep_count"),
                        defaultDurationSeconds = rs.getIntOrNull("default_duration_seconds"),
                        metValue = rs.getBigDecimal("met_value"),
                        difficulty = rs.getString("difficulty"),
                        category = rs.getString("category"),
                        cautionNote = rs.getString("caution_note"),
                        muscles = emptyList(),
                        voiceCues = emptyList(),
                    )
                }.optional()
                .orElse(null) ?: return null

        return base.copy(
            muscles = findMuscles(exerciseId),
            voiceCues = findVoiceCues(exerciseId),
        )
    }

    override fun findAll(): List<ExerciseSummaryView> =
        jdbcClient
            .sql(
                """
                $SUMMARY_COLUMNS
                FROM catalog.exercise
                ORDER BY exercise_id
                """.trimIndent(),
            ).query(SummaryRowMapper)
            .list()

    override fun findAllByIdentities(exerciseIdentities: List<ExerciseIdentity>): List<ExerciseSummaryView> =
        jdbcClient
            .sql(
                """
                $SUMMARY_COLUMNS
                FROM catalog.exercise
                WHERE exercise_id IN (:exerciseIds)
                ORDER BY exercise_id
                """.trimIndent(),
            ).param("exerciseIds", exerciseIdentities.map { it.value })
            .query(SummaryRowMapper)
            .list()

    /**
     * `ymove_slug IS NOT NULL` 을 SQL 에서 거른다. 맵에 null 값을 담아 호출부가 다시 거르게
     * 하면 "없음" 이 두 가지 모양(키 없음 / 값 null)을 갖게 된다.
     */
    override fun findYmoveSlugs(exerciseIdentities: List<ExerciseIdentity>): Map<Long, String> =
        jdbcClient
            .sql(
                """
                SELECT exercise_id, ymove_slug
                FROM catalog.exercise
                WHERE exercise_id IN (:exerciseIds)
                  AND ymove_slug IS NOT NULL
                """.trimIndent(),
            ).param("exerciseIds", exerciseIdentities.map { it.value })
            .query { rs, _ -> rs.getLong("exercise_id") to rs.getString("ymove_slug") }
            .list()
            .toMap()

    private fun findMuscles(exerciseId: Long) =
        jdbcClient
            .sql(
                """
                SELECT m.muscle_code, m.name, m.body_part_code,
                       m.front_highlight_asset_key, m.back_highlight_asset_key,
                       em.role, em.display_order
                FROM catalog.exercise_muscle em
                JOIN catalog.muscle m ON m.muscle_code = em.muscle_code
                WHERE em.exercise_id = :exerciseId
                ORDER BY em.display_order
                """.trimIndent(),
            ).param("exerciseId", exerciseId)
            .query(MuscleViewRowMapper)
            .list()

    private fun findVoiceCues(exerciseId: Long) =
        jdbcClient
            .sql(
                """
                SELECT display_order, start_offset_seconds, end_offset_seconds, content
                FROM catalog.exercise_voice_cue
                WHERE exercise_id = :exerciseId
                ORDER BY display_order
                """.trimIndent(),
            ).param("exerciseId", exerciseId)
            .query { rs, _ ->
                ExerciseVoiceCueView(
                    displayOrder = rs.getInt("display_order"),
                    startOffsetSeconds = rs.getIntOrNull("start_offset_seconds"),
                    endOffsetSeconds = rs.getIntOrNull("end_offset_seconds"),
                    content = rs.getString("content"),
                )
            }.list()

    /**
     * 전체 조회와 일괄 조회가 같은 뷰를 돌려주므로 컬럼 목록과 매핑을 한 곳에 둔다.
     * 둘로 두면 요약에 컬럼이 늘 때 한쪽만 고치게 된다.
     */
    private companion object {
        const val SUMMARY_COLUMNS =
            "SELECT exercise_id, name, image_asset_key, thumbnail_url, default_set_count, default_rep_count, " +
                "default_duration_seconds, met_value, difficulty, category"

        val SummaryRowMapper =
            RowMapper { rs, _ ->
                ExerciseSummaryView(
                    exerciseId = rs.getLong("exercise_id"),
                    name = rs.getString("name"),
                    imageAssetKey = rs.getString("image_asset_key"),
                    thumbnailUrl = rs.getString("thumbnail_url"),
                    defaultSetCount = rs.getIntOrNull("default_set_count"),
                    defaultRepCount = rs.getIntOrNull("default_rep_count"),
                    defaultDurationSeconds = rs.getIntOrNull("default_duration_seconds"),
                    metValue = rs.getBigDecimal("met_value"),
                    difficulty = rs.getString("difficulty"),
                    category = rs.getString("category"),
                )
            }
    }
}
