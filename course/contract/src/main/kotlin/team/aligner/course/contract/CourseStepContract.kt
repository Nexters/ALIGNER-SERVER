package team.aligner.course.contract

/**
 * `training` 이 세션을 시작할 때 읽는 스텝 구성. 통합 전용이라 좁게 만든다
 * (docs/architecture.md §7).
 *
 * 운동의 이름·MET·음성 큐를 싣지 않는다. `training` 은 그것을 `catalog:contract` 로 직접
 * 읽는다 (docs/domains.md §3). 여기서 같이 내리면 두 경로가 같은 값을 서로 다른 시점에
 * 들고 오게 된다.
 */
interface CourseStepContract {
    fun findStep(
        courseId: Long,
        stepOrder: Int,
    ): CourseStepResponse?
}

/**
 * `durationSeconds` `setCount` 는 코스의 override 가 있으면 그 값이고 없으면 null 이다.
 * null 일 때 `catalog` 기본값으로 메우는 것은 읽는 쪽의 몫이다 — 여기서 메우면 course 가
 * catalog 값을 계약에 실어 나르게 된다.
 */
data class CourseStepResponse(
    val courseId: Long,
    val courseStepId: Long,
    val stepOrder: Int,
    val completed: Boolean,
    val exercises: List<CourseStepExerciseResponse>,
)

data class CourseStepExerciseResponse(
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
)
