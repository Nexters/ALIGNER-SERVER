package team.aligner.training.adapter.course

import team.aligner.course.contract.CompleteSessionCommand
import team.aligner.course.contract.CourseProgressContract
import team.aligner.course.contract.CourseStepContract
import team.aligner.course.contract.PerformedExerciseCommand
import team.aligner.training.infrastructure.CourseProgressLookup
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepExerciseLookup
import team.aligner.training.infrastructure.CourseStepLookup
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.PerformedExerciseLookup

/**
 * `training` 의 port 둘을 `course:contract` 로 잇는다. 양쪽 도메인이 서로를 모르고 이 모듈만
 * 둘을 안다 (docs/architecture.md §7).
 */
internal class CourseStepAdapter(
    private val courseStepContract: CourseStepContract,
) : CourseStepPort {
    override fun findStep(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
    ): CourseStepLookup? =
        courseStepContract.findStep(memberId, courseId, stepOrder)?.let { step ->
            CourseStepLookup(
                courseId = step.courseId,
                courseStepId = step.courseStepId,
                stepOrder = step.stepOrder,
                completed = step.completed,
                exercises =
                    step.exercises.map {
                        CourseStepExerciseLookup(
                            courseStepExerciseId = it.courseStepExerciseId,
                            exerciseId = it.exerciseId,
                            displayOrder = it.displayOrder,
                            durationSeconds = it.durationSeconds,
                            setCount = it.setCount,
                        )
                    },
            )
        }
}

/**
 * 세션 완료를 course 로 밀어넣는다. **판단은 course 가 하고 여기는 값을 옮기기만 한다** —
 * 진행도·도장 로직이 training 쪽에 생기면 잘못 나눈 것이다 (docs/domains.md §2).
 */
internal class CourseProgressAdapter(
    private val courseProgressContract: CourseProgressContract,
) : CourseProgressPort {
    override fun completeSession(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
        performedExercises: List<PerformedExerciseLookup>,
    ): CourseProgressLookup {
        val response =
            courseProgressContract.completeSession(
                CompleteSessionCommand(
                    memberId = memberId,
                    courseId = courseId,
                    stepOrder = stepOrder,
                    performedExercises =
                        performedExercises.map {
                            PerformedExerciseCommand(
                                courseStepExerciseId = it.courseStepExerciseId,
                                performedDurationSeconds = it.performedDurationSeconds,
                            )
                        },
                ),
            )
        return CourseProgressLookup(
            courseId = response.courseId,
            completedStepCount = response.completedStepCount,
            totalStepCount = response.totalStepCount,
            courseCompleted = response.courseCompleted,
            stampAcquired = response.stampAcquired,
            estimatedKcal = response.estimatedKcal,
            targetPoseId = response.targetPoseId,
            targetPoseName = response.targetPoseName,
            bodyPartCode = response.bodyPartCode,
            level = response.level,
            acquiredStampCount = response.acquiredStampCount,
            requiredStampCount = response.requiredStampCount,
            targetPoseCompleted = response.targetPoseCompleted,
        )
    }
}
