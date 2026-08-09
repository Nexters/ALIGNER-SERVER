package team.aligner.course.model

import team.aligner.course.model.exception.CourseStepNotFoundException
import team.aligner.course.model.exception.EmptyCourseTemplateException
import java.time.Instant

/**
 * 회원에게 처방된 코스. 애그리거트 루트다.
 *
 * **하나의 핀포즈가 곧 하나의 코스다.** 회원이 고른 (강화 부위, 난이도)가 곧
 * `catalog.target_pose` 의 (부위, 레벨)이고 그 자세의 템플릿으로 만들어진다
 * (docs/domains.md §4-4).
 *
 * 일자 개념이 없다. 홈의 "오늘의 코스" 는 별도 개념이 아니라 **진행 중인 코스**의 다른
 * 이름이다.
 *
 * Spring Data JDBC 에는 더티체킹이 없다. [completeStep] 은 새 인스턴스를 반환하고 호출부가
 * save 를 명시한다 (docs/architecture.md §4).
 */
data class Course(
    val identity: CourseIdentity?,
    val memberId: Long,
    val templateId: Long,
    val targetPoseId: Long,
    /** 처방 시점에 검증에 쓴 원인의 스냅샷. 재진단으로 원인이 바뀌어도 남는다. */
    val causeCode: String?,
    val status: CourseStatus,
    val steps: List<CourseStep>,
    val createdAt: Instant?,
    val completedAt: Instant?,
) {
    /** 자세 도전 현황의 분모다. */
    val totalStepCount: Int get() = steps.size

    /** 자세 도전 현황의 분자다 — "3 / 4" 의 3. */
    val completedStepCount: Int get() = steps.count { it.status == CourseStepStatus.COMPLETED }

    /**
     * 다음에 수행할 스텝 순서. 완료한 스텝이 없으면 1 이고, 다 했으면 null 이다.
     *
     * 별도 컬럼으로 두지 않고 스텝 상태에서 계산한다. 컬럼으로 두면 스텝 완료와 커서 갱신이
     * 어긋날 수 있는데, 그 둘이 어긋난 상태를 표현할 이유가 없다.
     */
    val currentStepOrder: Int?
        get() =
            steps
                .filter { it.status != CourseStepStatus.COMPLETED }
                .minOfOrNull { it.stepOrder }

    /**
     * 스텝 하나를 완료로 바꾼다. 마지막 스텝이었으면 코스도 완료가 된다.
     *
     * **이미 완료된 스텝을 다시 완료해도 아무 일도 일어나지 않는다.** `training` 이 세션
     * 완료를 push 하는데 그 요청이 재시도될 수 있고, 그때 진행도가 두 번 오르면 안 된다
     * (docs/domains.md §7-8). 멱등성이 여기서 나온다.
     */
    fun completeStep(
        stepOrder: Int,
        at: Instant,
    ): Course {
        val target =
            steps.find { it.stepOrder == stepOrder }
                ?: throw CourseStepNotFoundException()

        if (target.status == CourseStepStatus.COMPLETED) {
            return this
        }

        val changed =
            steps.map {
                if (it.stepOrder == stepOrder) it.complete(at) else it
            }
        val allDone = changed.all { it.status == CourseStepStatus.COMPLETED }

        return copy(
            steps = changed,
            status = if (allDone) CourseStatus.COMPLETED else CourseStatus.IN_PROGRESS,
            // 코스 완료 시각은 마지막 스텝이 끝난 시각이다. 이미 완료된 코스면 그대로 둔다.
            completedAt = if (allDone) (completedAt ?: at) else null,
        )
    }

    companion object {
        /**
         * 템플릿을 회원의 코스로 복사한다.
         *
         * **복사하는 이유는 진행 상태가 회원별이기 때문**이고, 템플릿 seed 가 나중에 바뀌어도
         * 이미 처방된 코스의 구성이 흔들리면 안 되기 때문이다.
         *
         * 스텝이 없는 템플릿은 처방하지 않는다. 진행도의 분모가 0 이 되어 "완료했는데 완성이
         * 아닌" 코스가 남는다.
         */
        fun prescribe(
            memberId: Long,
            template: CourseTemplate,
            causeCode: String?,
        ): Course {
            if (template.steps.isEmpty()) {
                throw EmptyCourseTemplateException()
            }
            return Course(
                identity = null,
                memberId = memberId,
                templateId = template.templateId,
                targetPoseId = template.targetPoseId,
                causeCode = causeCode,
                status = CourseStatus.IN_PROGRESS,
                steps =
                    template.steps.map { templateStep ->
                        CourseStep(
                            identity = null,
                            stepOrder = templateStep.stepOrder,
                            status = CourseStepStatus.NOT_STARTED,
                            completedAt = null,
                            exercises =
                                templateStep.exercises.map {
                                    CourseStepExercise(
                                        identity = null,
                                        exerciseId = it.exerciseId,
                                        displayOrder = it.displayOrder,
                                        durationSeconds = it.durationSeconds,
                                        setCount = it.setCount,
                                    )
                                },
                        )
                    },
                createdAt = null,
                completedAt = null,
            )
        }
    }
}

/**
 * 코스의 스텝 하나. 코스 개요의 번호 매겨진 행이다.
 */
data class CourseStep(
    val identity: Long?,
    val stepOrder: Int,
    val status: CourseStepStatus,
    val completedAt: Instant?,
    val exercises: List<CourseStepExercise>,
) {
    fun complete(at: Instant): CourseStep = copy(status = CourseStepStatus.COMPLETED, completedAt = at)
}

/**
 * 스텝에 편성된 운동 하나.
 *
 * `durationSeconds` `setCount` 는 override 다. 비어 있으면 `catalog.exercise` 의 기본값을
 * 쓴다 (docs/domains.md §7-1). 이름·MET 같은 나머지는 `catalog` 가 갖는다 — 여기에는
 * 식별자만 값으로 둔다.
 */
data class CourseStepExercise(
    val identity: Long?,
    val exerciseId: Long,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
)
