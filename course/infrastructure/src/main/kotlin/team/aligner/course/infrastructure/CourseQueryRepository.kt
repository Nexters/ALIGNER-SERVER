package team.aligner.course.infrastructure

/**
 * 읽기 out-port. 화면 하나에 대응하는 조회만 둔다 (docs/architecture.md §4).
 *
 * **catalog 값을 붙이지 않은 뼈대만 돌려준다.** 자세 이름·운동 이름·MET 은 catalog 소유라
 * 이 리포지토리가 알 수 없다. 조립은 service 가 port 로 받아서 한다 — 그래야 SQL 이
 * 도메인 경계를 넘지 않는다 (docs/domains.md §6).
 */
interface CourseQueryRepository {
    fun findInProgressCourseSkeleton(memberId: Long): CourseSkeleton?

    fun findCourseSkeleton(
        courseId: Long,
        memberId: Long,
    ): CourseSkeleton?

    fun findAllCourseSkeletons(memberId: Long): List<CourseSkeleton>

    /**
     * 자세별로 지금까지 붙은 도장 수. 「자세 도전 현황」의 `3 / 4` 다.
     *
     * 코스 뼈대 조회에 합치지 않는다. 도장은 코스와 다른 애그리거트이고, 시작하지 않은 자세는
     * 코스 행 자체가 없어 같은 쿼리에 얹으면 LEFT JOIN 이 하나 더 붙기만 한다.
     */
    fun findStampCounts(memberId: Long): List<TargetPoseStampCount>
}

/** 자세 하나에 붙은 도장 수. 도장이 없는 자세는 목록에 실리지 않는다. */
data class TargetPoseStampCount(
    val targetPoseId: Long,
    val acquiredStampCount: Int,
)

/**
 * 조회 모델을 만들기 전의 중간 형태. course 스키마만으로 알 수 있는 값이다.
 */
data class CourseSkeleton(
    val courseId: Long,
    val targetPoseId: Long,
    val templateName: String,
    val recommendationReason: String?,
    val completed: Boolean,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val currentStepOrder: Int?,
    val steps: List<CourseStepSkeleton>,
)

data class CourseStepSkeleton(
    val courseStepId: Long,
    val stepOrder: Int,
    val completed: Boolean,
    val completedAt: java.time.Instant?,
    val exercises: List<CourseStepExerciseSkeleton>,
)

data class CourseStepExerciseSkeleton(
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
)
