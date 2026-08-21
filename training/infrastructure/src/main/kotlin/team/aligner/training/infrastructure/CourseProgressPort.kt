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

/**
 * 완료 리포트가 그대로 쓰는 값이다. 진행도뿐 아니라 **헤더의 자세 정보와 파이어로그**까지
 * course 가 실어 보낸다 — 셋 다 course 소유라 training 이 따로 조회할 수 없다.
 */
data class CourseProgressLookup(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    /** 이번 회차의 모든 스텝을 끝냈는지. **자세 완성과 다르다** — 완성은 4 회 완주다. */
    val courseCompleted: Boolean,
    /** 이 호출로 이번 회차의 도장이 새로 붙었는지. 재시도에서는 false 다. */
    val stampAcquired: Boolean,
    /** 이번 세션의 소모 칼로리. course 가 계산한다. 계산이 성립하지 않으면 null 이다. */
    val estimatedKcal: Int?,
    val targetPoseId: Long,
    /** 핀포즈의 catalog.exercise 식별자. 영상 조회에 쓴다. targetPoseId 와 다른 값이다. */
    val targetPoseExerciseId: Long?,
    /** catalog 에 자세가 없으면 빈 문자열이다. */
    val targetPoseName: String,
    /** 리포트 헤더의 `골반 난이도 상`. catalog 에 자세가 없으면 null 이다. */
    val bodyPartCode: String?,
    val level: Int?,
    /** 이 자세를 지금까지 완주한 횟수 = 붙은 도장 수. 리포트의 "파이어로그 N / 4회" 다. */
    val acquiredStampCount: Int,
    val requiredStampCount: Int,
    /** 도장을 다 채웠는지. 자세 완성 축하 화면의 신호다. */
    val targetPoseCompleted: Boolean,
)
