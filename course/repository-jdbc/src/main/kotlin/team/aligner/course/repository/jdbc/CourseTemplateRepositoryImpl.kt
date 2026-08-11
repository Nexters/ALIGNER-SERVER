package team.aligner.course.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.model.CourseTemplate
import team.aligner.course.model.CourseTemplateStep
import team.aligner.course.model.CourseTemplateStepExercise

/**
 * 템플릿은 seed 라 쓰기가 없다. 애그리거트도 CrudRepository 도 필요 없이 JdbcClient 로 읽는다
 * (catalog 의 조회 전용 리포지토리와 같은 형태).
 *
 * SQL 은 schema-qualified 다 (docs/architecture.md §6).
 */
internal class CourseTemplateRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : CourseTemplateRepository {
    /**
     * 본체 1 쿼리 + 스텝·운동 1 쿼리다. 스텝마다 운동을 따로 읽으면 스텝 수만큼 늘어난다.
     */
    override fun findByTargetPoseId(targetPoseId: Long): CourseTemplate? {
        val base =
            jdbcClient
                .sql(
                    """
                    SELECT template_id, target_pose_id, name, recommendation_reason
                    FROM course.course_template
                    WHERE target_pose_id = :targetPoseId
                    """.trimIndent(),
                ).param("targetPoseId", targetPoseId)
                .query { rs, _ ->
                    CourseTemplate(
                        templateId = rs.getLong("template_id"),
                        targetPoseId = rs.getLong("target_pose_id"),
                        name = rs.getString("name"),
                        recommendationReason = rs.getString("recommendation_reason"),
                        steps = emptyList(),
                    )
                }.optional()
                .orElse(null) ?: return null

        return base.copy(steps = findSteps(base.templateId))
    }

    private fun findSteps(templateId: Long): List<CourseTemplateStep> {
        data class Row(
            val stepOrder: Int,
            val exercise: CourseTemplateStepExercise?,
        )

        // LEFT JOIN 이다. 운동이 아직 편성되지 않은 스텝도 스텝으로는 존재한다.
        val rows =
            jdbcClient
                .sql(
                    """
                    SELECT ts.step_order, tse.exercise_id, tse.display_order,
                           tse.duration_seconds, tse.set_count
                    FROM course.template_step ts
                    LEFT JOIN course.template_step_exercise tse
                           ON tse.template_step_id = ts.template_step_id
                    WHERE ts.template_id = :templateId
                    ORDER BY ts.step_order, tse.display_order
                    """.trimIndent(),
                ).param("templateId", templateId)
                .query { rs, _ ->
                    val exerciseId = rs.getLong("exercise_id")
                    // wasNull() 은 마지막으로 읽은 컬럼을 가리킨다. step_order 를 읽은 뒤에
                    // 물으면 항상 false 라 운동이 없는 스텝에 exerciseId 0 이 들어간다.
                    val hasExercise = !rs.wasNull()
                    Row(
                        stepOrder = rs.getInt("step_order"),
                        exercise =
                            if (!hasExercise) {
                                null
                            } else {
                                CourseTemplateStepExercise(
                                    exerciseId = exerciseId,
                                    displayOrder = rs.getInt("display_order"),
                                    durationSeconds = rs.getIntOrNull("duration_seconds"),
                                    setCount = rs.getIntOrNull("set_count"),
                                )
                            },
                    )
                }.list()

        return rows
            .groupBy { it.stepOrder }
            .toSortedMap()
            .map { (stepOrder, grouped) ->
                CourseTemplateStep(
                    stepOrder = stepOrder,
                    exercises = grouped.mapNotNull { it.exercise }.sortedBy { it.displayOrder },
                )
            }
    }
}
