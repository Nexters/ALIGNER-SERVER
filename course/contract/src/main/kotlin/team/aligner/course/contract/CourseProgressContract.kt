package team.aligner.course.contract

/**
 * `training` 이 course 에 요구하는 계약. 통합 전용이라 좁게 만든다
 * (docs/architecture.md §7).
 *
 * **세션 완료를 `training` 이 push 한다.** course 가 training 을 조회하는 pull 로 짜면
 * 두 도메인이 양방향이 된다. `training` 은 "무슨 일이 있었나" 만 기록하고 진행도·도장
 * **판단은 전부 course** 가 한다 (docs/domains.md §2).
 *
 * 초판 초안에 있던 `completeStep` 을 두지 않는다. 두 진입점이 열려 있으면 세션 없이 스텝만
 * 완료하거나 같은 세션이 두 번 반영될 수 있는데, 스텝 완료를 세션 완료와 따로 부를 주체가
 * 이 설계에 없다 (§3).
 *
 * 구현체는 internal 로 course:service 에 두고 Bean 도 거기서 등록한다.
 */
interface CourseProgressContract {
    /**
     * 세션 하나가 끝났음을 반영한다. 스텝이 완료되고, 마지막 스텝이었으면 도장이 붙는다.
     *
     * **멱등하다.** 같은 스텝을 두 번 완료해도 진행도가 두 번 오르지 않고 도장도 한 번만
     * 붙는다. `training` 이 세션을 저장한 뒤 push 에 실패해 재시도하는 경우를 위한 것이다
     * (docs/domains.md §7-8).
     */
    fun completeSession(command: CompleteSessionCommand): CourseProgressResponse
}

/**
 * `memberId` 를 함께 받는다. 남의 코스를 완료 처리하지 못하도록 course 가 소유권을
 * 확인한다 — 계약 소비자를 신뢰해서 검증을 건너뛰지 않는다.
 */
data class CompleteSessionCommand(
    val memberId: Long,
    val courseId: Long,
    val stepOrder: Int,
)

data class CourseProgressResponse(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val courseCompleted: Boolean,
    /** 이 호출로 도장이 새로 붙었는지. 재시도로 들어온 호출에서는 false 다. */
    val stampAcquired: Boolean,
)
