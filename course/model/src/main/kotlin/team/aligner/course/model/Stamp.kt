package team.aligner.course.model

import java.time.Instant

/**
 * 완수한 목표 자세. 코스의 모든 스텝을 완료하면 붙는다 (docs/domains.md §7-8).
 *
 * `training` 이 아니라 `course` 가 소유한다. 기록은 training, **판단은 course** 다 (§2).
 */
data class Stamp(
    val identity: Long?,
    val memberId: Long,
    val targetPoseId: Long,
    val courseId: Long,
    val acquiredAt: Instant?,
) {
    companion object {
        fun acquire(
            memberId: Long,
            targetPoseId: Long,
            courseId: Long,
            at: Instant,
        ): Stamp =
            Stamp(
                identity = null,
                memberId = memberId,
                targetPoseId = targetPoseId,
                courseId = courseId,
                acquiredAt = at,
            )
    }
}
