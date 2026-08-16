package team.aligner.api

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.TestPropertySource
import org.springframework.web.bind.annotation.RestController
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * **조립이 실제로 서는지 확인하는 유일한 테스트다.**
 *
 * ComponentScan 을 쓰지 않는 구조라 Bean 등록 누락과 AutoConfiguration.imports 누락이
 * 컴파일에도 `build` 에도 걸리지 않는다. 기동은 성공하는데 호출만 404 가 되는 자리라
 * 사람 눈이나 이 테스트로만 잡힌다 (docs/architecture.md §5, .claude/rules/review.md 5번).
 *
 * 실제 앱(AlignerApplication)을 그대로 띄운다. 테스트 전용 부트스트랩을 두면 조립 누락을
 * 그 부트스트랩이 가려버려 검증의 의미가 사라진다.
 *
 * **컨텍스트가 뜬다는 것 자체가 가장 큰 단언이다.** 예를 들어 catalog:adapter-ymove 가 조립에서
 * 빠지면 PoseVideoPort Bean 이 없어 CatalogServiceAutoConfiguration 이 ExerciseQueryService 를
 * 만들지 못하고, 이 클래스의 모든 테스트가 기동 단계에서 죽는다.
 *
 * 시크릿은 여기서 더미로 채운다. application.yml 이 기본값을 두지 않기 때문이다
 * (DB_PASSWORD · JWT_SECRET · YMOVE_API_KEY).
 */
@Testcontainers
@SpringBootTest(classes = [AlignerApplication::class])
@TestPropertySource(
    properties = [
        "aligner.auth.jwt.secret=dummy-secret-for-integration-test-32bytes",
        "aligner.auth.kakao.client-id=dummy-client-id",
        "aligner.auth.kakao.client-secret=dummy-client-secret",
        // 실제 YMove 를 치지 않는다. 월 고유 운동 상한이 있어 CI 가 실 API 를 치면 안 된다.
        "aligner.ymove.api-key=dummy-api-key",
    ],
)
class ApplicationBootstrapIntegrationTest {
    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Test
    fun `컨트롤러가 Bean 으로 등록된다`() {
        // 컨트롤러를 @Bean 으로 올리지 않으면 기동은 되고 호출만 404 다. 개수가 아니라
        // 존재를 본다 — 컨트롤러가 늘 때마다 이 테스트가 깨지면 안 된다.
        context.getBeansWithAnnotation(RestController::class.java).keys.isEmpty() shouldBe false
    }

    /**
     * 타입을 import 하지 않고 이름으로 확인한다. application-api 는 조립 모듈이라
     * catalog:infrastructure 가 컴파일 클래스패스에 없다 — 그 자체가 계층 규칙이 서 있다는 뜻이다.
     */
    @Test
    fun `PoseVideoPort 가 조립돼 있다`() {
        context.beanDefinitionNames.toList() shouldContain "poseVideoPort"
    }

    @Test
    fun `Liquibase 가 다섯 도메인의 스키마를 전부 만든다`() {
        val schemas =
            jdbcClient
                .sql(
                    """
                    SELECT schema_name FROM information_schema.schemata
                    WHERE schema_name IN ('member', 'screening', 'catalog', 'course', 'training')
                    """.trimIndent(),
                ).query { rs, _ -> rs.getString("schema_name") }
                .list()

        schemas.sorted() shouldBe listOf("catalog", "course", "member", "screening", "training")
    }

    /**
     * seed 는 여기서만 실제로 적재된다. 도메인별 통합 테스트는 픽스처를 직접 넣고 시작하면서
     * TRUNCATE 를 하므로 seed 내용을 보지 못한다.
     */
    @Test
    fun `YMove slug 와 썸네일이 운동 29 개에 전부 채워진다`() {
        val filled =
            jdbcClient
                .sql(
                    """
                    SELECT count(*) FROM catalog.exercise
                    WHERE exercise_id BETWEEN 101 AND 129
                      AND ymove_slug IS NOT NULL AND thumbnail_url IS NOT NULL
                    """.trimIndent(),
                ).query(Int::class.java)
                .single()

        filled shouldBe 29
    }

    /**
     * 좌우가 갈리는 자세는 정본이 번역한 쪽을 써야 영상과 음성 큐가 어긋나지 않는다.
     * 호랑이 자세의 left 는 다리를 펴서 드는 **다른 동작**이라 여기가 틀리면 잘못된 지도가 된다.
     */
    @Test
    fun `좌우가 갈리는 자세는 정본이 번역한 쪽 slug 를 쓴다`() {
        val slugs =
            jdbcClient
                .sql("SELECT exercise_id, ymove_slug FROM catalog.exercise WHERE exercise_id IN (104, 129, 103)")
                .query { rs, _ -> rs.getLong("exercise_id") to rs.getString("ymove_slug") }
                .list()
                .toMap()

        slugs[104L] shouldBe "tiger-pose-right"
        slugs[129L] shouldBe "fire-log-pose-left"
        slugs[103L] shouldBe "cow-face-pose-left"
    }

    @Test
    fun `음성 큐가 운동 29 개에 순서대로 들어간다`() {
        val exercisesWithCues =
            jdbcClient
                .sql("SELECT count(DISTINCT exercise_id) FROM catalog.exercise_voice_cue")
                .query(Int::class.java)
                .single()
        exercisesWithCues shouldBe 29

        // display_order 는 1 부터 빈틈없이 이어져야 한다. 순차 재생이 유일한 재생 순서다.
        val broken =
            jdbcClient
                .sql(
                    """
                    SELECT count(*) FROM (
                        SELECT exercise_id FROM catalog.exercise_voice_cue
                        GROUP BY exercise_id
                        HAVING min(display_order) <> 1 OR max(display_order) <> count(*)
                    ) t
                    """.trimIndent(),
                ).query(Int::class.java)
                .single()
        broken shouldBe 0
    }

    /**
     * 타임코드는 아직 확정되지 않았다 (docs/domains.md §7-15). 값이 들어가 있으면 감수를
     * 거치지 않은 값이 재생에 쓰이는 것이다.
     */
    @Test
    fun `음성 큐 타임코드는 전부 비어 있다`() {
        val timed =
            jdbcClient
                .sql("SELECT count(*) FROM catalog.exercise_voice_cue WHERE start_offset_seconds IS NOT NULL")
                .query(Int::class.java)
                .single()

        timed shouldBe 0
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}
