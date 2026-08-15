package team.aligner.course.service

import team.aligner.course.contract.CompleteSessionCommand
import team.aligner.course.contract.CourseProgressContract
import team.aligner.course.contract.CourseProgressResponse

/**
 * 계약 구현체. internal 로 대상 도메인 service 에 둔다 (docs/architecture.md §7).
 *
 * 위임만 하고 로직을 두지 않는다. 여기에 판단이 생기면 계약 소비자마다 다른 동작을 하게 된다 —
 * 진행도·도장 판단은 CourseCommandService 안에 있어야 한다 (docs/domains.md §2).
 */
internal class CourseProgressContractImpl(
    private val courseCommandService: CourseCommandService,
) : CourseProgressContract {
    override fun completeSession(command: CompleteSessionCommand): CourseProgressResponse {
        val result =
            courseCommandService.completeStep(
                memberId = command.memberId,
                courseId = command.courseId,
                stepOrder = command.stepOrder,
                performedExercises =
                    command.performedExercises.map {
                        PerformedExercise(
                            courseStepExerciseId = it.courseStepExerciseId,
                            performedDurationSeconds = it.performedDurationSeconds,
                        )
                    },
            )
        return CourseProgressResponse(
            courseId = result.courseId,
            completedStepCount = result.completedStepCount,
            totalStepCount = result.totalStepCount,
            courseCompleted = result.courseCompleted,
            stampAcquired = result.stampAcquired,
            estimatedKcal = result.estimatedKcal,
            targetPoseId = result.targetPoseId,
            targetPoseName = result.targetPoseName,
            bodyPartCode = result.bodyPartCode,
            level = result.level,
            acquiredStampCount = result.acquiredStampCount,
            requiredStampCount = result.requiredStampCount,
            targetPoseCompleted = result.targetPoseCompleted,
        )
    }
}
