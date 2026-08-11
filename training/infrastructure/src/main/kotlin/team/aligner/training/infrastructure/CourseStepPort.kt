package team.aligner.training.infrastructure

/**
 * 세션을 열 때 읽는 코스 스텝 구성. `training/adapter-course` 가 구현한다
 * (docs/domains.md §3).
 *
 * 운동의 이름·MET·음성 큐가 없다. 그것은 catalog 소유라 [ExerciseDetailPort] 로 따로 읽는다 —
 * 한 경로가 두 도메인의 값을 실어 나르면 어느 쪽이 정본인지 흐려진다.
 */
interface CourseStepPort {
    fun findStep(
        courseId: Long,
        stepOrder: Int,
    ): CourseStepLookup?
}

data class CourseStepLookup(
    val courseId: Long,
    val courseStepId: Long,
    val stepOrder: Int,
    val completed: Boolean,
    val exercises: List<CourseStepExerciseLookup>,
)

/**
 * `durationSeconds` `setCount` 는 코스에 override 가 있을 때만 값이 있다. 없으면 catalog
 * 기본값을 쓰는데 그 해석은 읽는 쪽의 몫이다 (docs/domains.md §3).
 */
data class CourseStepExerciseLookup(
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
)
