package team.aligner.screening.repository.jdbc

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
import team.aligner.screening.infrastructure.BodyPartRepository
import team.aligner.screening.infrastructure.CauseRuleRepository
import team.aligner.screening.infrastructure.ScreeningQueryRepository
import team.aligner.screening.infrastructure.ScreeningResultRepository
import team.aligner.screening.model.PerceivedDifficulty
import team.aligner.screening.model.ScreeningAnswer
import team.aligner.screening.model.ScreeningResult
import team.aligner.screening.repository.jdbc.bootstrap.ScreeningRepositoryTestApplication

/**
 * 러너는 Kotest 가 아니라 JUnit5 다. kotest-extensions-spring 이 버전 카탈로그에 없다.
 * 단언만 kotest-assertions-core 를 쓴다.
 *
 * 픽스처를 seed 가 아니라 테스트가 직접 넣는다. seed changeset 은 후속 이슈이고, 감수 데이터가
 * 바뀔 때마다 이 테스트가 깨지면 안 된다.
 *
 * 여기서 처음으로 확인되는 것들이다.
 * - changelog 가 Liquibase 로 실제로 도는가
 * - 자식 둘을 매단 애그리거트가 한 번의 save 로 저장·회수되는가
 * - 스키마 제약(UNIQUE·CHECK)이 실제로 막는가
 * - JdbcClient SQL 이 schema-qualified 인가 (안 그러면 public 을 친다)
 */
@Testcontainers
@SpringBootTest(classes = [ScreeningRepositoryTestApplication::class])
class ScreeningRepositoryIntegrationTest {
    @Autowired
    private lateinit var screeningResultRepository: ScreeningResultRepository

    @Autowired
    private lateinit var screeningQueryRepository: ScreeningQueryRepository

    @Autowired
    private lateinit var bodyPartRepository: BodyPartRepository

    @Autowired
    private lateinit var causeRuleRepository: CauseRuleRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun `픽스처를 새로 넣는다`() {
        jdbcClient
            .sql(
                """
                TRUNCATE screening.screening_cause, screening.screening_answer,
                         screening.screening_result, screening.cause_rule,
                         screening.cause, screening.body_part
                RESTART IDENTITY CASCADE
                """.trimIndent(),
            ).update()

        insertBodyPart("BACK", "등", 1)
        insertBodyPart("ABDOMEN", "복부", 2)

        insertCause("THORACIC_STIFFNESS", "굳은 흉추", "BACK", "흉추가 굳으면 목이 앞으로 나온다")
        insertCause("WEAK_CORE", "약한 코어", "ABDOMEN", null)

        insertCauseRule(10L, "HARD", "THORACIC_STIFFNESS", 3)
        insertCauseRule(11L, "HARD", "THORACIC_STIFFNESS", 2)
        insertCauseRule(10L, "HARD", "WEAK_CORE", 4)
        insertCauseRule(10L, "EASY", "WEAK_CORE", 1)
    }

    @Test
    fun `changelog 가 screening 스키마에 테이블을 만든다`() {
        jdbcClient
            .sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'screening'")
            .query(Int::class.java)
            .single() shouldBe 6

        jdbcClient
            .sql("SELECT count(*) FROM public.databasechangelog WHERE id LIKE 'screening-%'")
            .query(Int::class.java)
            // dev 전용 changeset(screening-0010)도 여기서는 실행돼 개수에 포함된다.
            // **Liquibase 는 런타임 컨텍스트를 주지 않으면 context 가 붙은 changeset 도 전부 돌린다.**
            // 그 잠금은 application.yml 의 spring.liquibase.contexts 가 하는데, 이 부트스트랩은
            // 그 설정을 읽지 않는다. 픽스처를 TRUNCATE 후에 넣으므로 다른 단언에는 영향이 없다.
            //
            // changeset 을 새로 쌓을 때마다 이 숫자를 올린다. 개수를 세는 단언이라 changelog
            // 자체가 돌았는지만 보고, 어떤 changeset 인지는 보지 않는다.
            .single() shouldBe 11
    }

    @Test
    fun `도메인 테이블이 public 에 새지 않는다`() {
        listOf("body_part", "cause", "cause_rule", "screening_result", "screening_answer", "screening_cause")
            .forEach { table ->
                jdbcClient
                    .sql("SELECT to_regclass('public.$table')")
                    .query(String::class.java)
                    .optional()
                    .orElse(null)
                    .shouldBeNull()
            }
    }

    @Test
    fun `애그리거트가 자식 둘과 함께 한 번에 저장된다`() {
        val saved = screeningResultRepository.save(determinedResult())

        val identity = saved.identity.shouldNotBeNull()
        saved.answers.size shouldBe 2
        saved.causes.map { it.rank } shouldBe listOf(1, 2)

        jdbcClient
            .sql("SELECT count(*) FROM screening.screening_answer WHERE result_id = :id")
            .param("id", identity.value)
            .query(Int::class.java)
            .single() shouldBe 2
        jdbcClient
            .sql("SELECT count(*) FROM screening.screening_cause WHERE result_id = :id")
            .param("id", identity.value)
            .query(Int::class.java)
            .single() shouldBe 2
    }

    @Test
    fun `부위 목록은 노출 순서로 돌아온다`() {
        screeningQueryRepository.findAllBodyParts().map { it.bodyPartCode } shouldBe
            listOf("BACK", "ABDOMEN")
    }

    @Test
    fun `분기표를 자세 식별자로 좁혀 읽는다`() {
        val rules = causeRuleRepository.findAllByTargetPoseIds(listOf(10L))

        rules.size shouldBe 3
        rules.map { it.targetPoseId }.toSet() shouldBe setOf(10L)
    }

    @Test
    fun `빈 목록으로 분기표를 물으면 쿼리하지 않고 빈 결과다`() {
        // IN () 은 SQL 문법 오류다. port 가 자기 입력을 스스로 지켜야 한다.
        causeRuleRepository.findAllByTargetPoseIds(emptyList()) shouldBe emptyList()
    }

    @Test
    fun `부위 존재 검증이 코드로 동작한다`() {
        bodyPartRepository.existsByCode("ABDOMEN") shouldBe true
        bodyPartRepository.existsByCode("NOT_EXIST") shouldBe false
    }

    @Test
    fun `최신 결과가 원인을 rank 순으로 싣고 이름과 부위를 붙인다`() {
        screeningResultRepository.save(determinedResult())

        val view = screeningQueryRepository.findLatestByMemberId(MEMBER_ID).shouldNotBeNull()

        // (10,HARD)+(11,HARD) 로 THORACIC_STIFFNESS 가 3+2=5, WEAK_CORE 가 4 다.
        // (10,EASY) 규칙은 체감이 달라 매칭되지 않는다.
        view.causes.map { it.causeCode } shouldBe listOf("THORACIC_STIFFNESS", "WEAK_CORE")
        view.causes.map { it.rank } shouldBe listOf(1, 2)
        view.causes.map { it.score } shouldBe listOf(5, 4)
        // **회원은 자세만 골랐는데 서로 다른 부위의 원인이 순위로 나온다.** 진단 결과 화면이
        // 이 부위들을 늘어놓고, 회원은 그다음 화면에서 강화할 부위를 고른다 (docs/domains.md §4-2).
        view.causes.map { it.bodyPartCode } shouldBe listOf("BACK", "ABDOMEN")
        view.causes.first().name shouldBe "굳은 흉추"
        view.causes.first().description shouldBe "흉추가 굳으면 목이 앞으로 나온다"
        // description 은 nullable 이다.
        view.causes
            .last()
            .description
            .shouldBeNull()
    }

    @Test
    fun `여러 번 진단하면 가장 최근 것을 돌려준다`() {
        screeningResultRepository.save(determinedResult())
        val second = screeningResultRepository.save(determinedResult())

        screeningQueryRepository
            .findLatestByMemberId(MEMBER_ID)
            .shouldNotBeNull()
            .resultId shouldBe second.identity.shouldNotBeNull().value
    }

    @Test
    fun `진단한 적이 없으면 최신 결과가 null 이다`() {
        screeningQueryRepository.findLatestByMemberId(MEMBER_ID).shouldBeNull()
    }

    @Test
    fun `남의 결과는 식별자로도 읽지 못한다`() {
        val saved = screeningResultRepository.save(determinedResult())
        val resultId = saved.identity.shouldNotBeNull().value

        screeningQueryRepository.findByIdAndMemberId(resultId, MEMBER_ID).shouldNotBeNull()
        screeningQueryRepository.findByIdAndMemberId(resultId, MEMBER_ID + 1).shouldBeNull()
    }

    @Test
    fun `같은 진단에서 같은 자세를 두 번 저장하지 못한다`() {
        // 애그리거트가 먼저 막지만 DB 도 막아야 한다. 다른 경로로 들어와도 순위가 뒤틀리면 안 된다.
        val resultId = insertBareResult()

        insertAnswer(resultId, 10L, "HARD")
        assertThrows<DataIntegrityViolationException> { insertAnswer(resultId, 10L, "EASY") }
    }

    @Test
    fun `정의되지 않은 체감 값은 들어가지 못한다`() {
        val resultId = insertBareResult()

        assertThrows<DataIntegrityViolationException> { insertAnswer(resultId, 10L, "MEDIUM") }
    }

    @Test
    fun `한 진단에서 같은 원인이나 같은 순위가 두 번 나오지 못한다`() {
        val resultId = insertBareResult()

        insertCauseRow(resultId, "THORACIC_STIFFNESS", 1, 5)
        assertThrows<DataIntegrityViolationException> { insertCauseRow(resultId, "THORACIC_STIFFNESS", 2, 3) }
        assertThrows<DataIntegrityViolationException> { insertCauseRow(resultId, "WEAK_CORE", 1, 3) }
    }

    @Test
    fun `순위와 점수는 0 이하가 될 수 없다`() {
        val resultId = insertBareResult()

        assertThrows<DataIntegrityViolationException> { insertCauseRow(resultId, "WEAK_CORE", 0, 3) }
        assertThrows<DataIntegrityViolationException> { insertCauseRow(resultId, "WEAK_CORE", 1, 0) }
    }

    /**
     * perceived_body_part_code 는 changeset 007 로 NULL 허용이 됐고 저장 경로가 채우지 않는다.
     * 컬럼이 아직 남아 있다는 것과, 값을 넣지 않아도 저장이 되는 것을 함께 고정한다.
     */
    @Test
    fun `부위 없이 저장되고 컬럼은 NULL 로 남는다`() {
        val saved = screeningResultRepository.save(determinedResult())

        // NULL 여부를 SQL 안에서 판정한다. 컬럼을 그대로 꺼내면 매핑 단계의 null 처리에
        // 결과가 좌우돼 무엇을 검증하는지가 흐려진다.
        jdbcClient
            .sql("SELECT perceived_body_part_code IS NULL FROM screening.screening_result WHERE result_id = :resultId")
            .param("resultId", saved.identity.shouldNotBeNull().value)
            .query(Boolean::class.java)
            .single() shouldBe true
    }

    private fun determinedResult(): ScreeningResult =
        ScreeningResult
            .submit(
                memberId = MEMBER_ID,
                answers =
                    listOf(
                        ScreeningAnswer(targetPoseId = 10L, perceivedDifficulty = PerceivedDifficulty.HARD),
                        ScreeningAnswer(targetPoseId = 11L, perceivedDifficulty = PerceivedDifficulty.HARD),
                    ),
            ).determineCauses(causeRuleRepository.findAllByTargetPoseIds(listOf(10L, 11L)))

    private fun insertBodyPart(
        code: String,
        name: String,
        displayOrder: Int,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO screening.body_part (body_part_code, name, display_order)
            VALUES (:code, :name, :displayOrder)
            """.trimIndent(),
        ).param("code", code)
        .param("name", name)
        .param("displayOrder", displayOrder)
        .update()

    private fun insertCause(
        code: String,
        name: String,
        bodyPartCode: String,
        description: String?,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO screening.cause (cause_code, name, body_part_code, description)
            VALUES (:code, :name, :bodyPartCode, :description)
            """.trimIndent(),
        ).param("code", code)
        .param("name", name)
        .param("bodyPartCode", bodyPartCode)
        .param("description", description)
        .update()

    private fun insertCauseRule(
        targetPoseId: Long,
        difficulty: String,
        causeCode: String,
        weight: Int,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO screening.cause_rule (target_pose_id, perceived_difficulty, cause_code, weight)
            VALUES (:targetPoseId, :difficulty, :causeCode, :weight)
            """.trimIndent(),
        ).param("targetPoseId", targetPoseId)
        .param("difficulty", difficulty)
        .param("causeCode", causeCode)
        .param("weight", weight)
        .update()

    /** 제약만 보는 테스트용. 자식 없이 루트 한 행만 만든다. */
    private fun insertBareResult(): Long =
        jdbcClient
            .sql(
                """
                INSERT INTO screening.screening_result (member_id, perceived_body_part_code, created_at)
                VALUES (:memberId, 'ABDOMEN', now())
                RETURNING result_id
                """.trimIndent(),
            ).param("memberId", MEMBER_ID)
            .query(Long::class.java)
            .single()

    private fun insertAnswer(
        resultId: Long,
        targetPoseId: Long,
        difficulty: String,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO screening.screening_answer (result_id, target_pose_id, perceived_difficulty)
            VALUES (:resultId, :targetPoseId, :difficulty)
            """.trimIndent(),
        ).param("resultId", resultId)
        .param("targetPoseId", targetPoseId)
        .param("difficulty", difficulty)
        .update()

    private fun insertCauseRow(
        resultId: Long,
        causeCode: String,
        rank: Int,
        score: Int,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO screening.screening_cause (result_id, cause_code, rank, score)
            VALUES (:resultId, :causeCode, :rank, :score)
            """.trimIndent(),
        ).param("resultId", resultId)
        .param("causeCode", causeCode)
        .param("rank", rank)
        .param("score", score)
        .update()

    companion object {
        private const val MEMBER_ID = 1L

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}
