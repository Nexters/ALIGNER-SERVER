package team.aligner.catalog.service

import team.aligner.catalog.contract.ExerciseContract
import team.aligner.catalog.contract.ExerciseResponse
import team.aligner.catalog.model.ExerciseIdentity

/**
 * 계약 구현체. internal 로 대상 도메인 service 에 둔다 (docs/architecture.md §7).
 *
 * 위임과 변환만 하고 판단을 두지 않는다. 여기에 판단이 생기면 계약 소비자마다 다른 동작을 한다.
 */
internal class ExerciseContractImpl(
    private val exerciseQueryService: ExerciseQueryService,
) : ExerciseContract {
    override fun findAllByIds(exerciseIds: List<Long>): List<ExerciseResponse> =
        exerciseQueryService
            .getAll(exerciseIds.map(ExerciseIdentity::of))
            .map {
                ExerciseResponse(
                    exerciseId = it.exerciseId,
                    name = it.name,
                    defaultSetCount = it.defaultSetCount,
                    defaultRepCount = it.defaultRepCount,
                    defaultDurationSeconds = it.defaultDurationSeconds,
                    metValue = it.metValue,
                    difficulty = it.difficulty,
                )
            }
}
