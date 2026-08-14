package team.aligner.training.infrastructure

/**
 * 세션 완료를 `course` 로 밀어넣는 out-port. `training/adapter-course` 가 구현한다.
 *
 * **push 다.** course 가 training 을 조회하는 pull 로 짜면 두 도메인이 양방향이 된다
 * (docs/domains.md §2). 진행도·도장 판단은 전부 course 안에서 끝난다.
 *
 * **멱등하다.** 같은 스텝을 두 번 완료해도 진행도가 두 번 오르지 않고 도장도 한 번만 붙는다 —
 * 재시도 흡수는 course 애그리거트가 한다.
 */
interface CourseProgressPort {
    /**
     * `performedExercises` 는 **실측 수행 시간**이다. training 이 계산하지 않고 값만 넘긴다 —
     * kcal 은 MET(catalog)과 몸무게(member)의 함수인데 그 둘을 이미 읽는 쪽이 course 다
     * (docs/domains.md §4-3). training 이 계산하려면 port 두 개를 새로 뚫어야 한다.
     */
    fun completeSession(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
        performedExercises: List<PerformedExerciseLookup>,
    ): CourseProgressLookup
}

/** 실제로 수행한 운동 하나. **수행하지 않은 운동은 담지 않는다.** */
data class PerformedExerciseLookup(
    val courseStepExerciseId: Long,
    val performedDurationSeconds: Int?,
)

data class CourseProgressLookup(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val courseCompleted: Boolean,
    val stampAcquired: Boolean,
    /** 이번 세션의 소모 칼로리. course 가 계산한다. 계산이 성립하지 않으면 null 이다. */
    val estimatedKcal: Int?,
)
