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
    fun completeSession(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
    ): CourseProgressLookup
}

data class CourseProgressLookup(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val courseCompleted: Boolean,
    val stampAcquired: Boolean,
)
