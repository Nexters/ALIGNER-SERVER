package team.aligner.course.infrastructure

/**
 * 읽기 out-port. 화면 하나에 대응하는 조회만 둔다 (docs/architecture.md §4).
 *
 * **catalog 값을 붙이지 않은 뼈대만 돌려준다.** 자세 이름·운동 이름·MET 은 catalog 소유라
 * 이 리포지토리가 알 수 없다. 조립은 service 가 port 로 받아서 한다 — 그래야 SQL 이
 * 도메인 경계를 넘지 않는다 (docs/domains.md §6).
 */
interface CourseQueryRepository {
    /**
     * 홈의 "오늘의 코스".
     *
     * **진행 중인 코스가 없어도 오늘 완주한 코스가 있으면 그것이다.** 완주 즉시 사라지면
     * 홈의 완료 상태 화면(「내일 운동 미리보기」가 붙는 자리)을 그릴 수 없다. 어제 완주한
     * 코스까지 끌고 오지는 않는다 — 그건 오늘 할 일이 아니라 지난 기록이다.
     *
     * `completedOnOrAfter` 는 "오늘" 의 시작 시각이다. **날짜 경계 계산은 service 가 한다** —
     * 시간대는 비즈니스 판단이라 SQL 이 정할 값이 아니다.
     */
    fun findTodayCourseSkeleton(
        memberId: Long,
        completedOnOrAfter: java.time.Instant,
    ): CourseSkeleton?

    fun findCourseSkeleton(
        courseId: Long,
        memberId: Long,
    ): CourseSkeleton?

    /**
     * 자세로 회원의 코스를 찾는다. 「내일 운동 미리보기」가 이미 시작한 자세를 골랐을 때
     * **템플릿이 아니라 회원의 코스에서** 운동 개수·시간을 세기 위한 것이다.
     *
     * 코스 스텝은 추천 시점에 복사되므로 템플릿 seed 가 나중에 바뀌면 둘이 갈린다. 회원이
     * 내일 실제로 수행할 것은 복사본 쪽이다.
     */
    fun findCourseSkeletonByTargetPoseId(
        memberId: Long,
        targetPoseId: Long,
    ): CourseSkeleton?

    /**
     * 아직 시작하지 않은 자세의 코스 구성. 「내일 운동 미리보기」의 후보는 회원이 한 번도
     * 열지 않은 자세일 수 있고, 그때는 `course.course` 행 자체가 없어 템플릿이 유일한 근거다.
     */
    fun findTemplateSkeleton(targetPoseId: Long): CourseTemplateSkeleton?

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
    override val exerciseId: Long,
    val displayOrder: Int,
    override val durationSeconds: Int?,
    override val setCount: Int?,
) : ExerciseComposition

/**
 * 코스 카드의 합계(운동 개수·세트 합·예상 시간·칼로리)를 세는 데 필요한 최소 정보.
 *
 * 회원의 코스 스텝과 템플릿 스텝이 같은 계산을 타므로 공통 형태로 둔다. 미리보기가 두
 * 출처를 오가는데 합계 규칙까지 둘로 갈리면 같은 화면에 다른 숫자가 나온다.
 */
interface ExerciseComposition {
    val exerciseId: Long
    val durationSeconds: Int?
    val setCount: Int?
}

/**
 * 템플릿 마스터의 구성. 회원 코스가 없는 자세의 미리보기가 이것으로 카드를 그린다.
 *
 * 진행도가 없다 — 아직 아무도 시작하지 않은 코스라 완료한 스텝이라는 개념이 없다.
 */
data class CourseTemplateSkeleton(
    val targetPoseId: Long,
    val templateName: String,
    val recommendationReason: String?,
    val totalStepCount: Int,
    val exercises: List<TemplateStepExerciseSkeleton>,
)

data class TemplateStepExerciseSkeleton(
    override val exerciseId: Long,
    override val durationSeconds: Int?,
    override val setCount: Int?,
) : ExerciseComposition
