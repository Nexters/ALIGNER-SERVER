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

/**
 * 완료 리포트가 그대로 쓰는 값이다. 진행도뿐 아니라 **헤더의 자세 정보와 파이어로그**까지
 * 함께 싣는다 — 셋 다 course 가 이미 들고 있는 값이라, training 이 따로 조회하면 같은 값을
 * 다른 시점에 읽게 된다.
 */
data class CourseProgressResponse(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    /** 이번 회차의 모든 스텝을 끝냈는지. **자세 완성과 다르다** — 완성은 4 회 완주다. */
    val courseCompleted: Boolean,
    /** 이 호출로 이번 회차의 도장이 새로 붙었는지. 재시도로 들어온 호출에서는 false 다. */
    val stampAcquired: Boolean,
    /**
     * **이번 세션의** 소모 칼로리. 코스 누적이 아니다 — 완료 리포트가 방금 한 운동의 스탯을
     * 보여준다.
     *
     * 몸무게나 MET 이 없거나 수행 시간을 모르면 **0 이 아니라 null** 이다 (`CalorieCalculator`).
     */
    val estimatedKcal: Int?,
    /** 이 코스의 목표 자세. 리포트 헤더가 그린다. */
    val targetPoseId: Long,
    /**
     * 핀포즈의 catalog.exercise 식별자. **targetPoseId 로는 영상을 못 받는다** — 같은 자세가
     * 두 테이블에 각각 행을 갖고 영상·음성 큐는 exercise 쪽에만 있다. 핀포즈 직후 체감
     * 화면이 이 값으로 GET /catalog/exercises/{id} 를 부른다.
     *
     * slug 가 이어지지 않으면 null 이다. 화면은 그때 영상 없이 그린다.
     */
    val targetPoseExerciseId: Long?,
    /** catalog 에 자세가 없으면 빈 문자열이다. 리포트를 실패시키지 않는다. */
    val targetPoseName: String,
    /** 리포트 헤더의 `골반 난이도 상`. catalog 에 자세가 없으면 null 이다. */
    val bodyPartCode: String?,
    val level: Int?,
    /**
     * 이 자세에 지금까지 붙은 도장 수. 리포트의 **"파이어로그 N / 4회"** 의 N 이다.
     *
     * 코스 안 스텝 수가 아니다 — 한 번 완주할 때마다 하나씩 오른다.
     */
    val acquiredStampCount: Int,
    /** 세그먼트 개수. 화면이 4 를 하드코딩하지 않게 서버가 함께 내린다. */
    val requiredStampCount: Int,
    /** 도장을 다 채웠는지. 자세 완성 축하 화면의 신호다. */
    val targetPoseCompleted: Boolean,
)
