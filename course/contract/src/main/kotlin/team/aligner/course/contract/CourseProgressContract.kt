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
    /**
     * 이번 세션에서 **실제로 수행한** 운동. 소모 칼로리 계산 입력이다.
     *
     * `training` 이 계산하지 않고 실측값만 넘긴다. kcal = MET × 3.5 × 체중 ÷ 200 × 분 이라
     * MET(catalog)과 몸무게(member)가 둘 다 필요한데 그 둘을 이미 읽고 있는 쪽이 course 다
     * (docs/domains.md §4-3). training 이 계산하려면 port 두 개를 새로 뚫어야 한다.
     *
     * 비어 있으면 `estimatedKcal` 이 null 로 돌아간다.
     */
    val performedExercises: List<PerformedExerciseCommand> = emptyList(),
)

/**
 * 수행한 운동 하나. **수행하지 않은 운동은 담지 않는다** — 담기면 0 분으로 계산되어
 * "운동량 없음" 과 "안 했음" 이 뭉개진다.
 */
data class PerformedExerciseCommand(
    val courseStepExerciseId: Long,
    val performedDurationSeconds: Int?,
)

data class CourseProgressResponse(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val courseCompleted: Boolean,
    /** 이 호출로 도장이 새로 붙었는지. 재시도로 들어온 호출에서는 false 다. */
    val stampAcquired: Boolean,
    /**
     * **이번 세션의** 소모 칼로리. 코스 누적이 아니다 — 완료 리포트가 방금 한 운동의 스탯을
     * 보여준다.
     *
     * 몸무게나 MET 이 없거나 수행 시간을 모르면 **0 이 아니라 null** 이다 (`CalorieCalculator`).
     */
    val estimatedKcal: Int?,
)
