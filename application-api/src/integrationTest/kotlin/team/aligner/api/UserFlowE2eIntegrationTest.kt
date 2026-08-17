package team.aligner.api

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import team.aligner.catalog.infrastructure.PoseVideoPlayback
import team.aligner.catalog.infrastructure.PoseVideoPort
import team.aligner.support.web.auth.KakaoUser
import team.aligner.support.web.auth.KakaoUserClient
import tools.jackson.databind.JsonNode
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** @TestPropertySource 와 signDevToken 이 같은 값을 써야 한다. 애노테이션 인자라 파일 레벨 const 다. */
private const val JWT_SECRET = "dummy-secret-for-integration-test-32bytes"
private const val DEV_MEMBER_ID = 900001L

/**
 * `docs/user-flow.md` 의 해피 패스를 처음부터 끝까지 HTTP 로 태운다.
 *
 * **한 메서드가 플로우 전체를 순서대로 지난다.** 단계마다 테스트를 쪼개면 앞 단계가 만든 상태를
 * 공유하려고 필드가 늘고 순서 의존이 숨는다. 이 흐름은 실제로 순서 의존적이다 —
 * 진단 없이 코스가 없고, 코스 없이 세션이 없다.
 *
 * **외부 경계 둘만 스텁한다.** 카카오와 YMove 다. 나머지(PostgreSQL·Liquibase·JWT·Security)는
 * 전부 실물이라 조립이나 SQL 이 틀리면 여기서 깨진다.
 *
 * screening 의 원인 분기표는 테스트가 직접 넣는다. seed 가 아직 없고, **감수 데이터가 바뀔
 * 때마다 이 테스트가 깨지면 안 된다** (CatalogRepositoryIntegrationTest 와 같은 판단).
 * 자세·운동·코스 템플릿은 seed 를 그대로 쓴다 — 그게 실제로 적재되는지도 이 테스트가 본다.
 */
@Testcontainers
@SpringBootTest(
    classes = [AlignerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestPropertySource(
    properties = [
        "aligner.auth.jwt.secret=$JWT_SECRET",
        "aligner.auth.kakao.client-id=dummy-client-id",
        "aligner.auth.kakao.client-secret=dummy-client-secret",
        "aligner.ymove.api-key=dummy-api-key",
    ],
)
class UserFlowE2eIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    /** 실제 포트로 평범한 HTTP 를 친다. Boot 4 에서 TestRestTemplate 이 사라졌다. */
    private val rest: RestClient by lazy { RestClient.create("http://localhost:$port") }

    @TestBean
    private lateinit var kakaoUserClient: KakaoUserClient

    @TestBean
    private lateinit var poseVideoPort: PoseVideoPort

    @Test
    fun `로그인부터 완료 리포트까지 한 번에 지난다`() {
        insertCauseRules()

        // ── 1. 로그인 ─────────────────────────────────────────────────────────
        val token =
            post("/auth/kakao", mapOf("authorizationCode" to "test-code"))
                .also { it.statusCode shouldBe HttpStatus.OK }
                .body!!["accessToken"]
                .asText()

        // ── 2. 진입 분기 — 프로필이 비어 있어야 온보딩으로 간다 ────────────────
        get("/members/me", token).body!!["experienceLevel"].isNull shouldBe true

        // ── 3. 온보딩: 경력 → 신체 정보 ────────────────────────────────────────
        patch("/members/me", token, mapOf("experienceLevel" to "UNDER_ONE_YEAR")).statusCode shouldBe HttpStatus.OK
        patch("/members/me", token, mapOf("heightCm" to 170, "weightKg" to 60)).statusCode shouldBe HttpStatus.OK

        // ── 4. 자세 그리드 — seed 가 실제로 적재됐는지 여기서 드러난다 ──────────
        val poses = get("/catalog/target-poses", token).body!!
        poses.size() shouldBe 9

        // ── 5. 스크리닝 제출 = 저장·판별·결과 반환이 한 번에 일어난다 ───────────
        val result =
            post(
                "/screening/results",
                mapOf("answers" to listOf(mapOf("targetPoseId" to 1, "perceivedDifficulty" to "HARD"))),
                token,
            ).also { it.statusCode shouldBe HttpStatus.OK }.body!!
        result["causes"][0]["causeCode"].asText() shouldBe "BACK_EXTENSION_WEAK"

        // ── 6. 부위·난이도 선택 → 코스 추천 ────────────────────────────────────
        get("/screening/body-parts", token).body!!.size() shouldBe 3
        patch("/members/me", token, mapOf("reinforcementBodyPartCode" to "BACK", "reinforcementLevel" to 1))
        val courseId = post("/courses", mapOf("bodyPartCode" to "BACK", "level" to 1), token).body!!["courseId"].asLong()

        // ── 7. 홈 · 코스 개요 ──────────────────────────────────────────────────
        val today = get("/courses/today", token).body!!
        today["courseId"].asLong() shouldBe courseId
        today["targetPoseName"].asText() shouldBe "업독"

        val detail = get("/courses/$courseId", token).body!!
        val firstStep = detail["steps"][0]
        firstStep["completed"].asBoolean() shouldBe false
        val firstExercise = firstStep["exercises"][0]

        // ── 8. 세션 시작 → 운동 가이드 ─────────────────────────────────────────
        val sessionId =
            post("/sessions", mapOf("courseId" to courseId, "stepOrder" to firstStep["stepOrder"].asInt()), token)
                .body!!["sessionId"]
                .asLong()

        // ★ 이 이슈의 검증 지점이다. seed 의 ymove_slug → adapter → videoUrl 까지 이어진다.
        val exerciseId = firstExercise["exerciseId"].asLong()
        val exercise = get("/catalog/exercises/$exerciseId", token).body!!
        // 기대값을 DB 의 실제 slug 로 만든다. 상수로 두면 배선이 끊겨도 통과할 수 있다.
        val seededSlug =
            jdbcClient
                .sql("SELECT ymove_slug FROM catalog.exercise WHERE exercise_id = :id")
                .param("id", exerciseId)
                .query(String::class.java)
                .single()
        exercise["videoUrl"].asText() shouldBe "https://stub.test/$seededSlug.mp4"
        // 썸네일은 DB 값이라 YMove 와 무관하게 채워진다. videoUrl 과 출처가 다르다.
        exercise["thumbnailUrl"].asText().startsWith("https://exercise-api.ymove.app/") shouldBe true
        // 음성 큐가 순서대로 실린다. 타임코드는 아직 미확정이라 비어 있다.
        exercise["voiceCues"].size() shouldBeGreaterThan 0
        exercise["voiceCues"][0]["displayOrder"].asInt() shouldBe 1
        exercise["voiceCues"][0]["startOffsetSeconds"].isNull shouldBe true

        // ── 9. 세션 완료 = 진행도를 올리는 유일한 경로다 ───────────────────────
        // JsonNode.map 은 Kotlin 의 Iterable.map 이 아니라 노드 자신에 적용되는 Jackson 3 API 다.
        // 인덱스로 명시해 순회한다.
        val exercises = firstStep["exercises"]
        val records =
            (0 until exercises.size()).map { i ->
                mapOf(
                    "courseStepExerciseId" to exercises[i]["courseStepExerciseId"].asLong(),
                    "completed" to true,
                    "performedDurationSeconds" to 60,
                )
            }
        val completed =
            post("/sessions/$sessionId/complete", mapOf("exerciseRecords" to records), token)
                .also { it.statusCode shouldBe HttpStatus.OK }
                .body!!

        completed["status"].asText() shouldBe "COMPLETED"
        // 완료 리포트가 한 응답으로 끝난다 — 헤더 자세를 얻으려고 코스를 다시 부르지 않는다.
        val progress = completed["courseProgress"]
        progress.shouldNotBeNull()
        progress["completedStepCount"].asInt() shouldBe 1
        progress["targetPoseName"].asText() shouldBe "업독"

        // ── 10. 도전 현황 — 핀포즈 전체가 펼쳐진다 ─────────────────────────────
        val challenge = get("/courses/progress/target-poses", token).body!!
        challenge["totalCount"].asInt() shouldBe 9
        challenge["inProgressCount"].asInt() shouldBe 1
    }

    /**
     * `scripts/dev-token.sh` 가 만드는 토큰을 서버가 받아주는지 확인한다.
     *
     * 스크립트는 Spring 없이 openssl 로 서명하므로 형식이 어긋나면 프론트가 401 만 받고
     * 원인을 못 찾는다. 여기서 **같은 방식으로 만든 토큰**을 실제 서버에 태워 그 형식을 고정한다.
     *
     * 기대값이 401 이 아니라 404 인 것이 핵심이다 — 토큰은 통과했고(그래서 401 이 아니고)
     * 그 회원이 DB 에 없다는 뜻이다. **임시 회원은 dev 컨텍스트에서만 들어오므로 여기(기본값
     * prod)에는 없어야 한다.** 401 이면 서명 형식이 틀린 것이고, 200 이면 dev 전용 데이터가
     * 운영 경로로 새고 있다는 뜻이다.
     */
    @Test
    fun `dev-token 형식의 토큰은 통과하고 임시 회원은 prod 컨텍스트에 없다`() {
        val token = signDevToken(DEV_MEMBER_ID)

        val status =
            try {
                get("/members/me", token).statusCode
            } catch (exception: HttpClientErrorException) {
                exception.statusCode
            }

        status shouldBe HttpStatus.NOT_FOUND
    }

    /**
     * 운영 조회 둘이 실제로 매핑되고 seed 를 그대로 내리는지 본다.
     *
     * 컨트롤러를 @Bean 으로 올리지 않으면 기동은 되고 호출만 404 다 — 그 자리를 여기서 잡는다
     * (docs/architecture.md §5).
     *
     * **회원 플로우와 섞지 않는다.** 이 둘은 마스터 조회라 회원 상태에 기대지 않고, 그래서
     * DB 에 없는 회원의 토큰으로도 200 이어야 한다. 인증 자체는 여전히 필요하다.
     */
    @Test
    fun `운영 조회가 seed 를 그대로 내린다`() {
        val token = signDevToken(DEV_MEMBER_ID)

        val exercises = get("/operation/exercises", token).body!!
        exercises.size() shouldBe 29
        exercises[0]["exerciseId"].asLong() shouldBe 101L
        // 목록에서 그림을 확인할 수 있어야 한다. imageAssetKey 는 프론트가 정적 파일로 매핑하는
        // 키라 그대로 열리지 않고, 감수자가 브라우저로 볼 수 있는 값은 thumbnailUrl 뿐이다.
        exercises[0]["thumbnailUrl"].asText().startsWith("https://exercise-api.ymove.app/") shouldBe true

        val templates = get("/operation/course-templates", token).body!!
        templates.size() shouldBe 9
        // 자세 이름과 운동 이름은 catalog port 로 붙는다. 비어 있으면 배선이 끊긴 것이다 —
        // 찾지 못하면 예외가 아니라 빈 문자열이라 여기서만 드러난다.
        templates[0]["targetPoseName"].asText().isEmpty() shouldBe false
        templates[0]["stepCount"].asInt() shouldBeGreaterThan 0
        templates[0]["steps"][0]["exercises"][0]["name"].asText().isEmpty() shouldBe false
    }

    /** scripts/dev-token.sh 와 같은 헤더·클레임·서명이다. */
    private fun signDevToken(memberId: Long): String {
        val now = System.currentTimeMillis() / 1000
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload =
            encoder.encodeToString(
                """{"iss":"aligner","sub":"$memberId","iat":$now,"exp":${now + 3600}}""".toByteArray(),
            )
        val mac =
            Mac.getInstance("HmacSHA256").apply {
                init(SecretKeySpec(JWT_SECRET.toByteArray(), "HmacSHA256"))
            }
        val signature = encoder.encodeToString(mac.doFinal("$header.$payload".toByteArray()))
        return "$header.$payload.$signature"
    }

    /**
     * (자세, 체감) → 원인 분기표. 업독(1)을 어려워하면 등 신전 약화가 나오게 한 최소 규칙이다.
     */
    private fun insertCauseRules() {
        jdbcClient
            .sql(
                """
                INSERT INTO screening.cause (cause_code, name, body_part_code, description)
                VALUES ('BACK_EXTENSION_WEAK', '등 신전 약화', 'BACK', '흉추를 펴는 힘이 부족합니다')
                ON CONFLICT DO NOTHING
                """.trimIndent(),
            ).update()
        jdbcClient
            .sql(
                """
                INSERT INTO screening.cause_rule (target_pose_id, perceived_difficulty, cause_code, weight)
                VALUES (1, 'HARD', 'BACK_EXTENSION_WEAK', 3)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
            ).update()
    }

    private fun get(
        path: String,
        token: String,
    ) = rest
        .get()
        .uri(path)
        .headers { it.auth(token) }
        .retrieve()
        .toEntity(JsonNode::class.java)

    private fun post(
        path: String,
        body: Map<String, Any?>,
        token: String? = null,
    ) = rest
        .post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .headers { it.auth(token) }
        .body(body)
        .retrieve()
        .toEntity(JsonNode::class.java)

    private fun patch(
        path: String,
        token: String,
        body: Map<String, Any?>,
    ) = rest
        .patch()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .headers { it.auth(token) }
        .body(body)
        .retrieve()
        .toEntity(JsonNode::class.java)

    private fun HttpHeaders.auth(token: String?) = token?.let { setBearerAuth(it) }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        /** 인가 코드 교환을 대신한다. 같은 kakaoId 라 재로그인해도 같은 회원이다. */
        @JvmStatic
        fun kakaoUserClient(): KakaoUserClient =
            object : KakaoUserClient {
                override fun fetchUserByAuthorizationCode(authorizationCode: String) =
                    KakaoUser(kakaoId = "e2e-kakao-id", nickname = "테스터", profileImageUrl = null)
            }

        /**
         * slug 를 그대로 되돌려주지 않고 **exercise_id 로 만든 URL** 을 준다. 그래야 seed 의
         * ymove_slug → findYmoveSlugs → port → videoUrl 경로가 실제로 이어졌는지 단언할 수 있다.
         * 고정 문자열을 주면 배선이 끊겨도 테스트가 통과한다.
         */
        @JvmStatic
        fun poseVideoPort(): PoseVideoPort =
            object : PoseVideoPort {
                override fun findPlayback(ymoveSlugs: List<String>): Map<String, PoseVideoPlayback> =
                    ymoveSlugs.associateWith { PoseVideoPlayback("https://stub.test/$it.mp4") }
            }
    }
}
