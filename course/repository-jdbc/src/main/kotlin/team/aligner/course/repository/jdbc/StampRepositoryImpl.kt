package team.aligner.course.repository.jdbc

import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.model.Stamp
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 이미 있으면 아무것도 하지 않는다. 세션 완료 push 가 재시도돼도 도장이 두 번 붙지 않아야
 * 한다 (docs/domains.md §7-8).
 *
 * 유니크 제약이 DB 에도 있지만 거기까지 가면 제약 위반이 500 으로 나간다. 재시도는 정상
 * 흐름이라 예외가 될 일이 아니다.
 */
internal class StampRepositoryImpl(
    private val stampJdbcRepository: StampJdbcRepository,
) : StampRepository {
    override fun saveIfAbsent(stamp: Stamp) {
        if (stampJdbcRepository.existsByMemberIdAndTargetPoseId(stamp.memberId, stamp.targetPoseId)) {
            return
        }
        stampJdbcRepository.save(
            StampEntity(
                stampId = null,
                memberId = stamp.memberId,
                targetPoseId = stamp.targetPoseId,
                courseId = stamp.courseId,
                // TIMESTAMPTZ 가 마이크로초까지만 담는다 (CourseRepositoryImpl 과 같은 이유).
                acquiredAt = (stamp.acquiredAt ?: Instant.now()).truncatedTo(ChronoUnit.MICROS),
            ),
        )
    }
}
