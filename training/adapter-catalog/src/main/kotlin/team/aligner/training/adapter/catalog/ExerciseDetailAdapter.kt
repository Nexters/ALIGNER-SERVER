package team.aligner.training.adapter.catalog

import team.aligner.catalog.contract.ExerciseContract
import team.aligner.training.infrastructure.ExerciseDetailLookup
import team.aligner.training.infrastructure.ExerciseDetailPort

/**
 * `training` 의 port 를 `catalog:contract` 로 잇는다 (docs/architecture.md §7).
 *
 * 존재하지 않는 식별자가 섞여 있어도 예외가 아니다. 도메인 간 FK 가 없어 세션 기록이 가리키는
 * 운동이 catalog 에서 사라질 수 있고, 그때 세션 조회가 통째로 실패하면 안 된다
 * (docs/domains.md §6).
 */
internal class ExerciseDetailAdapter(
    private val exerciseContract: ExerciseContract,
) : ExerciseDetailPort {
    override fun findAllByIds(exerciseIds: List<Long>): List<ExerciseDetailLookup> =
        exerciseContract.findAllByIds(exerciseIds).map {
            ExerciseDetailLookup(
                exerciseId = it.exerciseId,
                name = it.name,
                category = it.category,
                defaultSetCount = it.defaultSetCount,
                defaultDurationSeconds = it.defaultDurationSeconds,
            )
        }
}
