package team.aligner.course.service

import team.aligner.course.contract.CourseStepContract
import team.aligner.course.contract.CourseStepExerciseResponse
import team.aligner.course.contract.CourseStepResponse
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.model.CourseIdentity
import team.aligner.course.model.CourseStepStatus

/**
 * 계약 구현체. internal 로 대상 도메인 service 에 둔다 (docs/architecture.md §7).
 *
 * 없는 코스·스텝을 null 로 돌려주는 것은 계약이 그렇게 정해져 있어서다. 세션을 시작할 수
 * 있는지 판단하는 쪽은 training 이다.
 */
internal class CourseStepContractImpl(
    private val courseRepository: CourseRepository,
) : CourseStepContract {
    override fun findStep(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
    ): CourseStepResponse? {
        // 남의 코스도 없는 코스와 같이 null 이다. 다른 조회가 전부 memberId 를 조건에 넣는데
        // 여기만 빠져 있으면 그 경로로 코스 구성이 새어나간다.
        val course =
            courseRepository
                .findByIdentity(CourseIdentity.of(courseId))
                ?.takeIf { it.memberId == memberId }
                ?: return null
        val step = course.steps.find { it.stepOrder == stepOrder } ?: return null
        return CourseStepResponse(
            courseId = courseId,
            courseStepId = checkNotNull(step.identity) { "저장된 스텝에 식별자가 없다" },
            stepOrder = step.stepOrder,
            completed = step.status == CourseStepStatus.COMPLETED,
            exercises =
                step.exercises
                    .sortedBy { it.displayOrder }
                    .map {
                        CourseStepExerciseResponse(
                            courseStepExerciseId = checkNotNull(it.identity) { "저장된 운동에 식별자가 없다" },
                            exerciseId = it.exerciseId,
                            displayOrder = it.displayOrder,
                            durationSeconds = it.durationSeconds,
                            setCount = it.setCount,
                        )
                    },
        )
    }
}
