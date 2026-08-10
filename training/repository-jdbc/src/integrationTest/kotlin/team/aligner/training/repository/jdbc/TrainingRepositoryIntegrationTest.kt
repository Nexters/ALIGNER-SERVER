package team.aligner.training.repository.jdbc

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.simple.JdbcClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import team.aligner.training.infrastructure.SessionRepository
import team.aligner.training.model.ExerciseResult
import team.aligner.training.model.Session
import team.aligner.training.model.SessionStatus
import team.aligner.training.model.StepExercise
import team.aligner.training.repository.jdbc.bootstrap.TrainingRepositoryTestApplication
import java.time.Instant

/**
 * 러너는 Kotest 가 아니라 JUnit5 다. 단언만 kotest-assertions-core 를 쓴다.
 *
 * 여기서 처음으로 확인되는 것들이다.
 * - 자식을 매단 애그리거트가 한 번의 save 로 오가는가
 * - @Table(schema = "training") 이 실제로 먹었는가
 * - 완료 상태와 완료 시각이 어긋나는 것을 CHECK 이 막는가
 */
@Testcontainers
@SpringBootTest(classes = [TrainingRepositoryTestApplication::class])
class TrainingRepositoryIntegrationTest {
    @Autowired
    private lateinit var sessionRepository: SessionRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun `픽스처를 새로 넣는다`() {
        jdbcClient
            .sql("TRUNCATE training.session_exercise_record, training.session RESTART IDENTITY CASCADE")
            .update()
    }

    @Test
    fun `changelog 가 training 스키마에 테이블을 만든다`() {
        jdbcClient
            .sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'training'")
            .query(Int::class.java)
            .single() shouldBe 2

        jdbcClient
            .sql("SELECT count(*) FROM public.databasechangelog WHERE id LIKE 'training-%'")
            .query(Int::class.java)
            .single() shouldBe 3
    }

    @Test
    fun `도메인 테이블이 public 에 새지 않는다`() {
        listOf("session", "session_exercise_record").forEach { table ->
            jdbcClient
                .sql("SELECT to_regclass('public.$table')")
                .query(String::class.java)
                .optional()
                .orElse(null)
                .shouldBeNull()
        }
    }

    @Test
    fun `애그리거트가 자식과 함께 한 번에 저장되고 되읽힌다`() {
        val saved = sessionRepository.save(started())
        val identity = saved.identity.shouldNotBeNull()

        val found = sessionRepository.findByIdentity(identity).shouldNotBeNull()

        found.courseId shouldBe COURSE_ID
        found.stepOrder shouldBe 1
        found.status shouldBe SessionStatus.IN_PROGRESS
        found.records.map { it.displayOrder } shouldBe listOf(1, 2)
        found.records.all { !it.completed } shouldBe true
        // save() 가 돌려준 시각이 DB 에 실제로 들어간 값이어야 한다. TIMESTAMPTZ 가
        // 마이크로초로 자르므로 나노초를 그대로 넣으면 여기서 갈린다.
        found.startedAt shouldBe saved.startedAt
    }

    @Test
    fun `완료하면 수행 결과가 저장된다`() {
        val saved = sessionRepository.save(started())
        val identity = saved.identity.shouldNotBeNull()

        sessionRepository.save(
            saved.complete(
                results = listOf(ExerciseResult(courseStepExerciseId = 51L, completed = true, performedDurationSeconds = 120)),
                at = AT,
            ),
        )

        val found = sessionRepository.findByIdentity(identity).shouldNotBeNull()
        found.status shouldBe SessionStatus.COMPLETED
        found.completedAt shouldBe AT
        found.records.first { it.courseStepExerciseId == 51L }.completed shouldBe true
        found.records.first { it.courseStepExerciseId == 51L }.performedDurationSeconds shouldBe 120
        // 요청에 없던 운동은 미수행으로 남는다.
        found.records.first { it.courseStepExerciseId == 52L }.completed shouldBe false
    }

    /**
     * status 와 completed_at 이 따로 놀지 않게 CHECK 이 막는다.
     */
    @Test
    fun `완료 상태인데 완료 시각이 없으면 DB 가 막는다`() {
        val saved = sessionRepository.save(started())

        assertThrows<DataIntegrityViolationException> {
            jdbcClient
                .sql("UPDATE training.session SET status = 'COMPLETED' WHERE session_id = :id")
                .param("id", saved.identity.shouldNotBeNull().value)
                .update()
        }
    }

    /**
     * 같은 코스 스텝 운동이 한 세션에 두 번 들어가면 수행 기록이 어느 쪽인지 알 수 없다.
     */
    @Test
    fun `한 세션에 같은 코스 스텝 운동을 두 번 넣지 못한다`() {
        val saved = sessionRepository.save(started())

        assertThrows<DataIntegrityViolationException> {
            jdbcClient
                .sql(
                    """
                    INSERT INTO training.session_exercise_record
                        (session_id, course_step_exercise_id, exercise_id, display_order, completed)
                    VALUES (:sessionId, 51, 101, 3, false)
                    """.trimIndent(),
                ).param("sessionId", saved.identity.shouldNotBeNull().value)
                .update()
        }
    }

    private fun started(): Session =
        Session.start(
            memberId = MEMBER_ID,
            courseId = COURSE_ID,
            stepOrder = 1,
            exercises =
                listOf(
                    StepExercise(courseStepExerciseId = 51L, exerciseId = 101L, displayOrder = 1),
                    StepExercise(courseStepExerciseId = 52L, exerciseId = 102L, displayOrder = 2),
                ),
        )

    companion object {
        private const val MEMBER_ID = 1L
        private const val COURSE_ID = 20L
        private val AT: Instant = Instant.parse("2026-08-10T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }
}
