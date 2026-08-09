package team.aligner.course.adapter.catalog

import team.aligner.catalog.contract.ExerciseContract
import team.aligner.catalog.contract.TargetPoseContract
import team.aligner.course.infrastructure.ExerciseCatalogEntry
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.TargetPoseCatalogEntry
import team.aligner.course.infrastructure.TargetPoseCatalogPort

/**
 * `course` 의 port 둘을 `catalog:contract` 로 잇는다 (docs/architecture.md §7).
 */
internal class TargetPoseCatalogAdapter(
    private val targetPoseContract: TargetPoseContract,
) : TargetPoseCatalogPort {
    override fun findByBodyPartCodeAndLevel(
        bodyPartCode: String,
        level: Int,
    ): TargetPoseCatalogEntry? =
        targetPoseContract.findByBodyPartCodeAndLevel(bodyPartCode, level)?.let {
            TargetPoseCatalogEntry(
                targetPoseId = it.targetPoseId,
                name = it.name,
                imageAssetKey = it.imageAssetKey,
                bodyPartCode = it.bodyPartCode,
                level = it.level,
            )
        }

    /**
     * 계약이 단건 조회만 노출하므로 여기서 식별자마다 부른다. 자세 목록이 회원의 도전 현황
     * 한 화면치라 수가 작다 — 계약을 넓히는 것은 실제로 커진 뒤에 한다
     * (docs/architecture.md §3 "미리 만들지 않는다").
     */
    override fun findAllByIds(targetPoseIds: List<Long>): List<TargetPoseCatalogEntry> =
        targetPoseIds.distinct().mapNotNull { id ->
            targetPoseContract.findById(id)?.let {
                TargetPoseCatalogEntry(
                    targetPoseId = it.targetPoseId,
                    name = it.name,
                    imageAssetKey = it.imageAssetKey,
                    bodyPartCode = it.bodyPartCode,
                    level = it.level,
                )
            }
        }
}

internal class ExerciseCatalogAdapter(
    private val exerciseContract: ExerciseContract,
) : ExerciseCatalogPort {
    /**
     * 존재하지 않는 식별자가 섞여 있어도 예외가 아니다. 도메인 간 FK 가 없어 course seed 가
     * catalog 보다 앞서갈 수 있다 (docs/domains.md §6).
     */
    override fun findAllByIds(exerciseIds: List<Long>): List<ExerciseCatalogEntry> =
        exerciseContract.findAllByIds(exerciseIds).map {
            ExerciseCatalogEntry(
                exerciseId = it.exerciseId,
                name = it.name,
                category = it.category,
                defaultSetCount = it.defaultSetCount,
                defaultDurationSeconds = it.defaultDurationSeconds,
                metValue = it.metValue,
            )
        }
}
