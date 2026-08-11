package team.aligner.course.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.model.Stamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * `ON CONFLICT DO NOTHING` 으로 확인과 저장을 **한 문장에** 끝낸다.
 *
 * 확인 뒤 저장으로 나누면 두 요청이 확인을 함께 통과해 둘 다 INSERT 하고 유니크 제약에
 * 걸린다. 세션 완료 push 는 재시도되는 경로라 실제로 겹친다.
 *
 * CrudRepository 를 쓰지 않고 JdbcClient 를 쓰는 이유가 이것이다 — `ON CONFLICT` 를 태우려면
 * SQL 을 직접 써야 한다. 도장은 자식이 없는 단일 행이라 애그리거트 매핑이 필요 없다.
 *
 * SQL 은 schema-qualified 다 (docs/architecture.md §6).
 */
internal class StampRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : StampRepository {
    override fun saveIfAbsent(stamp: Stamp): Boolean {
        val inserted =
            jdbcClient
                .sql(
                    """
                    INSERT INTO course.stamp (member_id, target_pose_id, course_id, acquired_at)
                    VALUES (:memberId, :targetPoseId, :courseId, :acquiredAt)
                    ON CONFLICT (member_id, target_pose_id) DO NOTHING
                    """.trimIndent(),
                ).param("memberId", stamp.memberId)
                .param("targetPoseId", stamp.targetPoseId)
                .param("courseId", stamp.courseId)
                // Instant 를 그대로 넘기면 드라이버가 SQL 타입을 추론하지 못한다. Spring Data JDBC
                // 경로에는 변환기가 있지만 JdbcClient 는 값을 그대로 넘긴다. TIMESTAMPTZ 에
                // 대응하는 OffsetDateTime 으로 바꿔 넘긴다.
                //
                // 마이크로초로 자르는 것은 TIMESTAMPTZ 정밀도에 맞추기 위해서다
                // (CourseRepositoryImpl 과 같은 이유).
                .param(
                    "acquiredAt",
                    OffsetDateTime.ofInstant(
                        (stamp.acquiredAt ?: Instant.now()).truncatedTo(ChronoUnit.MICROS),
                        ZoneOffset.UTC,
                    ),
                ).update()

        // DO NOTHING 이 걸리면 0 행이다. 그것이 "이미 있었다" 는 뜻이다.
        return inserted > 0
    }
}
