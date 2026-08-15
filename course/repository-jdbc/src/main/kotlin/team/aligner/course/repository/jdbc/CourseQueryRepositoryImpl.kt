package team.aligner.course.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseSkeleton
import team.aligner.course.infrastructure.CourseStepExerciseSkeleton
import team.aligner.course.infrastructure.CourseStepSkeleton
import team.aligner.course.infrastructure.CourseTemplateSkeleton
import team.aligner.course.infrastructure.TargetPoseStampCount
import team.aligner.course.infrastructure.TemplateStepExerciseSkeleton
import java.sql.Timestamp
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
     * 진행 중인 코스가 여럿이면 가장 최근에 추천된 것을 집는다. 회원이 난이도를 바꿔 다른
     * 자세의 코스를 새로 받으면 그것이 지금 하고 있는 코스이기 때문이다.
     *
     * **진행 중인 코스가 없으면 오늘 완주한 코스를 집는다.** 완주하는 순간 status 가
     * COMPLETED 로 바뀌는데 그때 404 를 내면 홈의 완료 상태 화면을 그릴 수 없다. 진행 중인
     * 쪽이 언제나 우선이다 — 완주한 뒤 다른 자세를 새로 시작한 회원의 홈은 새 코스여야 한다.
     *
     * 완주 코스끼리는 `completed_at` 이, 진행 중 코스끼리는 `created_at` 이 최신 판단 기준이다.
     * 정렬에서 두 무리가 섞이지 않으므로 COALESCE 하나로 둘을 함께 다룬다.
     */
    override fun findTodayCourseSkeleton(
        memberId: Long,
        completedOnOrAfter: Instant,
    ): CourseSkeleton? {
        val courseId =
            jdbcClient
                .sql(
                    """
                    SELECT course_id
                    FROM course.course
                    WHERE member_id = :memberId
                      AND (status = 'IN_PROGRESS'
                           OR (status = 'COMPLETED' AND completed_at >= :completedOnOrAfter))
                    ORDER BY CASE WHEN status = 'IN_PROGRESS' THEN 0 ELSE 1 END,
                             COALESCE(completed_at, created_at) DESC,
                             course_id DESC
                    LIMIT 1
                    """.trimIndent(),
                ).param("memberId", memberId)
                .param("completedOnOrAfter", Timestamp.from(completedOnOrAfter))
                .query(Long::class.java)
                .optional()
                .orElse(null) ?: return null

        return findCourseSkeleton(courseId, memberId)
    }

    /**
     * 자세로 회원의 코스를 찾는다. `(member_id, target_pose_id)` 가 유니크라 최대 하나다.
     *
     * 「내일 운동 미리보기」가 이미 시작한 자세를 골랐을 때 쓴다. 회원이 내일 실제로 수행할
     * 것은 추천 시점에 복사된 이 스텝들이지 지금의 템플릿 seed 가 아니다.
     */
    override fun findCourseSkeletonByTargetPoseId(
        memberId: Long,
        targetPoseId: Long,
    ): CourseSkeleton? {
        val courseId =
            jdbcClient
                .sql(
                    """
                    SELECT course_id
                    FROM course.course
                    WHERE member_id = :memberId AND target_pose_id = :targetPoseId
                    """.trimIndent(),
                ).param("memberId", memberId)
                .param("targetPoseId", targetPoseId)
                .query(Long::class.java)
                .optional()
                .orElse(null) ?: return null

        return findCourseSkeleton(courseId, memberId)
    }

    /**
     * 템플릿 마스터의 구성. 회원이 아직 열지 않은 자세의 미리보기가 이것으로 카드를 그린다.
     *
     * 스텝이 하나도 없는 템플릿도 행이 돌아온다 — LEFT JOIN 인 이유다. 그런 템플릿은
     * 추천 시점에 EMPTY_COURSE_TEMPLATE 로 걸리지만, 미리보기는 그 판단까지 하지 않고
     * "운동 0 개" 로 내려보내는 대신 조립하는 쪽이 후보에서 걸러낼 수 있게 그대로 돌려준다.
     */
    override fun findTemplateSkeleton(targetPoseId: Long): CourseTemplateSkeleton? {
        data class Row(
            val templateName: String,
            val recommendationReason: String?,
            val templateStepId: Long?,
            val exercise: TemplateStepExerciseSkeleton?,
        )

        val rows =
            jdbcClient
                .sql(
                    """
                    SELECT t.name, t.recommendation_reason,
                           s.template_step_id, e.exercise_id, e.duration_seconds, e.set_count
                    FROM course.course_template t
                    LEFT JOIN course.template_step s ON s.template_id = t.template_id
                    LEFT JOIN course.template_step_exercise e ON e.template_step_id = s.template_step_id
                    WHERE t.target_pose_id = :targetPoseId
                    ORDER BY s.step_order, e.display_order
                    """.trimIndent(),
                ).param("targetPoseId", targetPoseId)
                .query { rs, _ ->
                    val templateStepId = rs.getLong("template_step_id")
                    val hasStep = !rs.wasNull()
                    val exerciseId = rs.getLong("exercise_id")
                    // wasNull() 은 마지막으로 읽은 컬럼을 가리킨다. findSteps 와 같은 함정이라
                    // 읽은 직후에 붙잡아 둔다.
                    val hasExercise = !rs.wasNull()
                    Row(
                        templateName = rs.getString("name"),
                        recommendationReason = rs.getString("recommendation_reason"),
                        templateStepId = templateStepId.takeIf { hasStep },
                        exercise =
                            if (!hasExercise) {
                                null
                            } else {
                                TemplateStepExerciseSkeleton(
                                    exerciseId = exerciseId,
                                    durationSeconds = rs.getIntOrNull("duration_seconds"),
                                    setCount = rs.getIntOrNull("set_count"),
                                )
                            },
                    )
                }.list()

        val head = rows.firstOrNull() ?: return null
        return CourseTemplateSkeleton(
            targetPoseId = targetPoseId,
            templateName = head.templateName,
            recommendationReason = head.recommendationReason,
            totalStepCount = rows.mapNotNull { it.templateStepId }.distinct().size,
            exercises = rows.mapNotNull { it.exercise },
        )
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
     * 자세별 도장 수. 도전 현황의 `3 / 4` 이자 완성 판정의 근거다.
     *
     * 도장이 없는 자세는 행이 없다. 조립하는 쪽이 "코스를 시작했는가" 로 0 과 null 을
     * 가른다 — 0/4 는 "시작했는데 아직 한 번도 완주 못 함" 이고 null 은 "아직 열지 않음" 이다.
     */
    override fun findStampCounts(memberId: Long): List<TargetPoseStampCount> =
        jdbcClient
            .sql(
                """
                SELECT target_pose_id, count(*) AS acquired_stamp_count
                FROM course.stamp
                WHERE member_id = :memberId
                GROUP BY target_pose_id
                """.trimIndent(),
            ).param("memberId", memberId)
            .query { rs, _ ->
                TargetPoseStampCount(
                    targetPoseId = rs.getLong("target_pose_id"),
                    acquiredStampCount = rs.getInt("acquired_stamp_count"),
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
