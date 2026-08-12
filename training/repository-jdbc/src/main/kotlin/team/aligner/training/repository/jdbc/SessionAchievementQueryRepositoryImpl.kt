package team.aligner.training.repository.jdbc

import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import java.time.LocalDate

/**
 * 조회는 JdbcClient 로 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * **날짜 접기를 DB 에서 한다.** 완료 시각을 전부 읽어 애플리케이션에서 접으면, 한 해 치를
 * 훑는 조회가 행을 그만큼 실어 온다. `DISTINCT` 까지 DB 가 하면 회원이 하루에 몇 번을
 * 했든 결과는 날짜 수만큼이다.
 *
 * SQL 은 schema-qualified 다. FROM session 으로 쓰면 public 을 친다 (§6).
 */
internal class SessionAchievementQueryRepositoryImpl(
    private val jdbcClient: JdbcClient,
) : SessionAchievementQueryRepository {
    override fun findCompletedDates(
        memberId: Long,
        from: LocalDate,
    ): List<LocalDate> =
        jdbcClient
            .sql(
                """
                SELECT DISTINCT (completed_at AT TIME ZONE :zone)::date AS completed_date
                FROM training.session
                WHERE member_id = :memberId
                  AND completed_at IS NOT NULL
                  AND (completed_at AT TIME ZONE :zone)::date >= :from
                ORDER BY completed_date DESC
                """.trimIndent(),
            ).param("memberId", memberId)
            .param("zone", ACHIEVEMENT_ZONE_ID)
            .param("from", from)
            .query { rs, _ -> rs.getObject("completed_date", LocalDate::class.java) }
            .list()

    private companion object {
        /**
         * 회원이 사는 날짜로 센다. 밤 늦게 한 운동이 UTC 로는 다음 날이라, UTC 로 접으면
         * 하루가 둘로 갈리고 연속이 끊긴 것처럼 보인다.
         */
        const val ACHIEVEMENT_ZONE_ID = "Asia/Seoul"
    }
}
