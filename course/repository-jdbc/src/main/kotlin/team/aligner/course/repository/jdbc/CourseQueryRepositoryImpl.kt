package team.aligner.course.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseSkeleton
import team.aligner.course.infrastructure.CourseStepExerciseSkeleton
import team.aligner.course.infrastructure.CourseStepSkeleton
import java.time.Instant

/**
 * 조회는 JdbcClient 로 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * **catalog·member 값을 붙이지 않는다.** 자세 이름·운동 이름·MET·몸무게는 다른 도메인
 * 소유라 이 SQL 이 알 수 없다. 조립은 service 가 port 로 받아서 한다 (docs/domains.md §6).
 *
 * SQL 은 schema-qualified 다 (§6).
 */
internal class CourseQueryRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : CourseQueryRepository {
    /**
     * 홈의 "오늘의 코스". **진행 중인 코스가 곧 오늘의 코스다** — 일자 컬럼이 없다
     * (docs/domains.md §4-4).
     *
     * 진행 중인 코스가 여럿이면 가장 최근에 처방된 것을 집는다. 회원이 난이도를 바꿔 다른
     * 자세의 코스를 새로 받으면 그것이 지금 하고 있는 코스이기 때문이다.
     */
    override fun findInProgressCourseSkeleton(memberId: Long): CourseSkeleton? {
        val courseId =
            jdbcClient
                .sql(
                    """
                    SELECT course_id
                    FROM course.course
                    WHERE member_id = :memberId AND status = 'IN_PROGRESS'
                    ORDER BY created_at DESC, course_id DESC
                    LIMIT 1
                    """.trimIndent(),
                ).param("memberId", memberId)
                .query(Long::class.java)
                .optional()
                .orElse(null) ?: return null

        return findCourseSkeleton(courseId, memberId)
    }

    /**
     * 남의 코스를 읽지 못하도록 `memberId` 를 조건에 함께 넣는다. 없는 식별자와 남의 식별자를
     * 같은 결과(null)로 돌려주는 것도 의도다 — 구분해서 알려주면 존재 여부가 새어나간다
     * (screening 의 findByIdAndMemberId 와 같은 판단).
     */
    override fun findCourseSkeleton(
        courseId: Long,
        memberId: Long,
    ): CourseSkeleton? {
        val base =
            jdbcClient
                .sql(
                    """
                    SELECT c.course_id, c.target_pose_id, c.status, t.name, t.recommendation_reason
                    FROM course.course c
                    JOIN course.course_template t ON t.template_id = c.template_id
                    WHERE c.course_id = :courseId AND c.member_id = :memberId
                    """.trimIndent(),
                ).param("courseId", courseId)
                .param("memberId", memberId)
                .query { rs, _ ->
                    BaseRow(
                        courseId = rs.getLong("course_id"),
                        targetPoseId = rs.getLong("target_pose_id"),
                        completed = rs.getString("status") == COMPLETED,
                        templateName = rs.getString("name"),
                        recommendationReason = rs.getString("recommendation_reason"),
                    )
                }.optional()
                .orElse(null) ?: return null

        return base.toSkeleton(findSteps(base.courseId))
    }

    /**
     * 자세 도전 현황. 회원의 코스 전체를 훑는다.
     *
     * **스텝과 운동까지 한 번에 읽지 않는다.** 이 화면은 `3 / 4` 와 상태만 그리므로 집계로
     * 충분하고, 코스 수만큼 자식을 끌어오면 응답이 화면이 쓰지 않는 값으로 부푼다.
     */
    override fun findAllCourseSkeletons(memberId: Long): List<CourseSkeleton> =
        jdbcClient
            .sql(
                """
                SELECT c.course_id, c.target_pose_id, c.status, t.name, t.recommendation_reason,
                       count(s.course_step_id)                                        AS total_step_count,
                       count(s.course_step_id) FILTER (WHERE s.status = 'COMPLETED')  AS completed_step_count,
                       min(s.step_order) FILTER (WHERE s.status <> 'COMPLETED')       AS current_step_order
                FROM course.course c
                JOIN course.course_template t ON t.template_id = c.template_id
                LEFT JOIN course.course_step s ON s.course_id = c.course_id
                WHERE c.member_id = :memberId
                GROUP BY c.course_id, c.target_pose_id, c.status, t.name, t.recommendation_reason
                ORDER BY c.created_at DESC, c.course_id DESC
                """.trimIndent(),
            ).param("memberId", memberId)
            .query { rs, _ ->
                CourseSkeleton(
                    courseId = rs.getLong("course_id"),
                    targetPoseId = rs.getLong("target_pose_id"),
                    templateName = rs.getString("name"),
                    recommendationReason = rs.getString("recommendation_reason"),
                    completed = rs.getString("status") == COMPLETED,
                    completedStepCount = rs.getInt("completed_step_count"),
                    totalStepCount = rs.getInt("total_step_count"),
                    currentStepOrder = rs.getIntOrNull("current_step_order"),
                    // 목록 화면은 스텝 내역을 그리지 않는다.
                    steps = emptyList(),
                )
            }.list()

    /**
     * 스텝과 운동을 한 쿼리로 읽는다. LEFT JOIN 인 것은 운동이 편성되지 않은 스텝도 스텝으로
     * 존재하기 때문이다.
     */
    private fun findSteps(courseId: Long): List<CourseStepSkeleton> {
        data class Row(
            val courseStepId: Long,
            val stepOrder: Int,
            val completed: Boolean,
            val completedAt: Instant?,
            val exercise: CourseStepExerciseSkeleton?,
        )

        val rows =
            jdbcClient
                .sql(
                    """
                    SELECT s.course_step_id, s.step_order, s.status, s.completed_at,
                           e.course_step_exercise_id, e.exercise_id, e.display_order,
                           e.duration_seconds, e.set_count
                    FROM course.course_step s
                    LEFT JOIN course.course_step_exercise e ON e.course_step_id = s.course_step_id
                    WHERE s.course_id = :courseId
                    ORDER BY s.step_order, e.display_order
                    """.trimIndent(),
                ).param("courseId", courseId)
                .query { rs, _ ->
                    val exerciseRowId = rs.getLong("course_step_exercise_id")
                    // wasNull() 은 **마지막으로 읽은 컬럼**을 가리킨다. 다른 컬럼을 읽은 뒤에
                    // 물으면 그 컬럼의 null 여부가 돌아온다 — completed_at 이 NULL 인 스텝에서
                    // 운동이 통째로 사라졌다. 읽은 직후에 붙잡아 둔다.
                    val hasExercise = !rs.wasNull()
                    Row(
                        courseStepId = rs.getLong("course_step_id"),
                        stepOrder = rs.getInt("step_order"),
                        completed = rs.getString("status") == COMPLETED,
                        completedAt = rs.getTimestamp("completed_at")?.toInstant(),
                        exercise =
                            if (!hasExercise) {
                                null
                            } else {
                                CourseStepExerciseSkeleton(
                                    courseStepExerciseId = exerciseRowId,
                                    exerciseId = rs.getLong("exercise_id"),
                                    displayOrder = rs.getInt("display_order"),
                                    durationSeconds = rs.getIntOrNull("duration_seconds"),
                                    setCount = rs.getIntOrNull("set_count"),
                                )
                            },
                    )
                }.list()

        return rows
            .groupBy { it.courseStepId }
            .map { (_, grouped) ->
                val head = grouped.first()
                CourseStepSkeleton(
                    courseStepId = head.courseStepId,
                    stepOrder = head.stepOrder,
                    completed = head.completed,
                    completedAt = head.completedAt,
                    exercises = grouped.mapNotNull { it.exercise }.sortedBy { it.displayOrder },
                )
            }.sortedBy { it.stepOrder }
    }

    private data class BaseRow(
        val courseId: Long,
        val targetPoseId: Long,
        val completed: Boolean,
        val templateName: String,
        val recommendationReason: String?,
    )

    private fun BaseRow.toSkeleton(steps: List<CourseStepSkeleton>): CourseSkeleton =
        CourseSkeleton(
            courseId = courseId,
            targetPoseId = targetPoseId,
            templateName = templateName,
            recommendationReason = recommendationReason,
            completed = completed,
            completedStepCount = steps.count { it.completed },
            totalStepCount = steps.size,
            currentStepOrder = steps.filterNot { it.completed }.minOfOrNull { it.stepOrder },
            steps = steps,
        )

    private companion object {
        const val COMPLETED = "COMPLETED"
    }
}
