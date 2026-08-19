package team.aligner.catalog.repository.jdbc

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
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.infrastructure.TargetPoseQueryRepository
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.MuscleRole
import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.repository.jdbc.bootstrap.CatalogRepositoryTestApplication
import java.math.BigDecimal

/**
 * 러너는 Kotest 가 아니라 JUnit5 다. kotest-extensions-spring 이 버전 카탈로그에 없다.
 * 단언만 kotest-assertions-core 를 쓴다.
 *
 * 픽스처를 seed 가 아니라 테스트가 직접 넣는다. seed changeset 은 후속 이슈이고, 감수 데이터가
 * 바뀔 때마다 이 테스트가 깨지면 안 된다.
 *
 * 여기서 처음으로 확인되는 것들이다.
 * - changelog 가 Liquibase 로 실제로 도는가 (DDL 자체는 psql 로 확인했으나 YAML 은 아니다)
 * - JdbcClient SQL 이 schema-qualified 인가 (안 그러면 public 을 친다)
 * - `@EnableJdbcRepositories` 없이 JdbcClient Bean 이 주입되는가
 */
@Testcontainers
@SpringBootTest(classes = [CatalogRepositoryTestApplication::class])
class CatalogRepositoryIntegrationTest {
    @Autowired
    private lateinit var exerciseQueryRepository: ExerciseQueryRepository

    @Autowired
    private lateinit var targetPoseQueryRepository: TargetPoseQueryRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun `픽스처를 새로 넣는다`() {
        jdbcClient
            .sql(
                """
                TRUNCATE catalog.exercise_voice_cue, catalog.pose_muscle, catalog.exercise_muscle,
                         catalog.exercise, catalog.target_pose, catalog.muscle
                RESTART IDENTITY CASCADE
                """.trimIndent(),
            ).update()

        // 척추기립근은 뒤에만, 장요근은 앞에만 보인다.
        insertMuscle("ERECTOR_SPINAE", "척추기립근", "BACK", backKey = "erector-spinae-back")
        insertMuscle("ILIOPSOAS", "장요근", "PELVIS", frontKey = "iliopsoas-front")

        insertExercise(
            1L,
            "camel-pose",
            "낙타자세",
            imageAssetKey = "exercise/camel",
            thumbnailUrl = "https://ymove.test/camel.jpg",
        )
        insertExerciseMuscle(1L, "ERECTOR_SPINAE", MuscleRole.STRENGTHEN, 1, "가슴을 먼저 들어 올린 뒤에 뒤로 젖히세요.")
        // 문구가 아직 없는 근육도 정상이다. 감수 전이면 NULL 로 남는다.
        insertExerciseMuscle(1L, "ILIOPSOAS", MuscleRole.STRETCH, 2)
        insertVoiceCue(1L, 1, null, null, "무릎을 골반 너비로 벌리세요")
        insertVoiceCue(1L, 2, 35, 75, "명치를 천장으로 끌어올리고 유지하세요")

        insertExercise(2L, "cat-cow-pose", "캣카우")

        insertTargetPose(1L, "camel-pose", "낙타자세", "BACK", 2)
        insertPoseMuscle(1L, "ERECTOR_SPINAE", MuscleRole.STRENGTHEN, 1)
        insertTargetPose(2L, "upward-facing-dog-pose", "업독", "BACK", 1)
        insertTargetPose(3L, "bridge-pose", "브릿지", "PELVIS", 1)
    }

    @Test
    fun `changelog 가 catalog 스키마에 테이블을 만든다`() {
        jdbcClient
            .sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'catalog'")
            .query(Int::class.java)
            .single() shouldBe 6

        // changeset 을 추가할 때마다 함께 올린다. 0012~0014 는 YMove 연동(썸네일 컬럼,
        // slug·썸네일 seed, 음성 큐 seed), 0015~0016 은 근육맵 seed,
        // 0017~0018 은 핵심 동작 문구, 0019~0020 은 자세 썸네일,
        // 0021~0024 는 002-exercise.sql 이 비워뒀던 분류·MET·기본 세트 수·기본 시간 seed 다.
        jdbcClient
            .sql("SELECT count(*) FROM public.databasechangelog WHERE id LIKE 'catalog-%'")
            .query(Int::class.java)
            .single() shouldBe 24
    }

    @Test
    fun `도메인 테이블이 public 에 새지 않는다`() {
        listOf("exercise", "target_pose", "muscle", "exercise_voice_cue").forEach { table ->
            jdbcClient
                .sql("SELECT to_regclass('public.$table')")
                .query(String::class.java)
                .optional()
                .orElse(null)
                .shouldBeNull()
        }
    }

    @Test
    fun `운동 상세가 근육과 음성 큐를 순서대로 싣는다`() {
        val detail = exerciseQueryRepository.findDetail(ExerciseIdentity.of(1L)).shouldNotBeNull()

        detail.name shouldBe "낙타자세"
        detail.category shouldBe "가동성 웜업"
        // 새로 넣은 컬럼이 제 자리로 매핑돼야 한다. video_url 은 YMove 연동 전이라 비어 있는
        // 것이 정상이고, 그 상태로도 상세 조회가 성립해야 한다.
        detail.imageAssetKey shouldBe "exercise/camel"
        detail.videoUrl.shouldBeNull()
        detail.muscles.map { it.name } shouldBe listOf("척추기립근", "장요근")
        // RENAME 한 컬럼과 새로 넣은 컬럼이 각각 제 자리로 매핑돼야 한다.
        detail.muscles.map { it.frontHighlightAssetKey } shouldBe listOf(null, "iliopsoas-front")
        detail.muscles.map { it.backHighlightAssetKey } shouldBe listOf("erector-spinae-back", null)
        // role 문자열이 MuscleRole 로 매핑돼야 한다.
        detail.muscles.map { it.role } shouldBe listOf(MuscleRole.STRENGTHEN, MuscleRole.STRETCH)
        detail.voiceCues.map { it.displayOrder } shouldBe listOf(1, 2)
        // 타임코드 미확정 큐와 구간 큐가 섞여 있어도 그대로 돌아와야 한다.
        detail.voiceCues.map { it.startOffsetSeconds } shouldBe listOf(null, 35)
        detail.voiceCues.map { it.endOffsetSeconds } shouldBe listOf(null, 75)
        // 핵심 동작은 근육마다 다르고, 감수 전이면 NULL 로 남는다.
        detail.muscles.map { it.description } shouldBe listOf("가슴을 먼저 들어 올린 뒤에 뒤로 젖히세요.", null)
    }

    /**
     * 운영 목록의 전체 조회다.
     *
     * 일괄 조회(`findAllByIdentities`)와 매핑을 공유하므로, 공유한 뒤에도 두 조회가 같은 값을
     * 돌려주는지 함께 본다. 정렬은 SQL 이 책임진다 — 화면이 다시 정렬하지 않는다.
     */
    @Test
    fun `운동 전체 조회가 식별자 순으로 전부 돌려준다`() {
        val all = exerciseQueryRepository.findAll()

        all.map { it.exerciseId } shouldBe listOf(1L, 2L)
        all.map { it.name } shouldBe listOf("낙타자세", "캣카우")
        all.first().imageAssetKey shouldBe "exercise/camel"
        // 감수자가 목록에서 그림을 확인하는 값이다. imageAssetKey 는 키라 그대로 열리지 않는다.
        all.first().thumbnailUrl shouldBe "https://ymove.test/camel.jpg"
        all.last().thumbnailUrl.shouldBeNull()
        // 일괄 조회와 같은 매핑을 쓴다. 한쪽만 고치면 여기서 깨진다.
        all shouldBe
            exerciseQueryRepository.findAllByIdentities(
                listOf(ExerciseIdentity.of(1L), ExerciseIdentity.of(2L)),
            )
    }

    /**
     * SMALLINT 컬럼(default_set_count·default_rep_count)이 Int 로 매핑되는지 확인한다.
     *
     * getObject 캐스트를 쓰던 시절에는 컬럼 타입이 넓어지면 런타임에 깨졌다. getIntOrNull 로
     * 바꿨고 그 매핑이 실제로 도는지 여기서 본다.
     */
    @Test
    fun `숫자 컬럼이 값과 NULL 을 구분해 매핑된다`() {
        val detail = exerciseQueryRepository.findDetail(ExerciseIdentity.of(1L)).shouldNotBeNull()

        // SMALLINT 에 값이 있는 경우
        detail.defaultSetCount shouldBe 3
        // SMALLINT 가 비어 있는 경우. getInt 는 0 을 돌려주므로 wasNull 을 안 보면 0 이 된다.
        detail.defaultRepCount shouldBe null
        // INT
        detail.defaultDurationSeconds shouldBe 120
        detail.metValue shouldBe BigDecimal("2.30")

        val summary =
            exerciseQueryRepository.findAllByIdentities(listOf(ExerciseIdentity.of(1L))).single()

        summary.defaultSetCount shouldBe 3
        summary.defaultRepCount shouldBe null
        summary.defaultDurationSeconds shouldBe 120
    }

    @Test
    fun `자식이 없는 운동은 빈 목록이다`() {
        val detail = exerciseQueryRepository.findDetail(ExerciseIdentity.of(2L)).shouldNotBeNull()

        detail.muscles shouldBe emptyList()
        detail.voiceCues shouldBe emptyList()
    }

    @Test
    fun `없는 운동은 null 이다`() {
        exerciseQueryRepository.findDetail(ExerciseIdentity.of(999L)).shouldBeNull()
    }

    @Test
    fun `일괄 조회는 없는 식별자가 섞여도 찾은 것만 돌려준다`() {
        val summaries =
            exerciseQueryRepository.findAllByIdentities(
                listOf(ExerciseIdentity.of(1L), ExerciseIdentity.of(999L), ExerciseIdentity.of(2L)),
            )

        summaries.map { it.exerciseId } shouldBe listOf(1L, 2L)
    }

    @Test
    fun `자세 그리드는 부위로 걸러 레벨 순으로 돌려준다`() {
        val poses = targetPoseQueryRepository.findAll("BACK")

        poses.map { it.name } shouldBe listOf("업독", "낙타자세")
        poses.map { it.level } shouldBe listOf(1, 2)
    }

    /**
     * 온보딩 그리드가 부위를 먼저 묻지 않고 핀포즈 전체를 펼친다 (docs/domains.md §4-2).
     *
     * null 파라미터는 `CAST(:bodyPartCode AS VARCHAR) IS NULL` 로 끊는데, 캐스팅이 빠지면
     * PostgreSQL 이 파라미터 타입을 못 정해 여기서만 터진다. 그래서 컨테이너 테스트가 필요하다.
     */
    @Test
    fun `부위를 생략하면 전체 자세가 부위와 레벨 순으로 돌아온다`() {
        val poses = targetPoseQueryRepository.findAll(null)

        // ORDER BY body_part_code, level, target_pose_id — BACK 이 PELVIS 보다 앞이고,
        // BACK 안에서는 레벨 1 인 업독이 레벨 2 인 낙타자세보다 앞이다.
        poses.map { it.name } shouldBe listOf("업독", "낙타자세", "브릿지")
        poses.map { it.bodyPartCode }.distinct() shouldBe listOf("BACK", "PELVIS")
    }

    @Test
    fun `자세가 없는 부위는 빈 목록이다`() {
        targetPoseQueryRepository.findAll("ABDOMEN") shouldBe emptyList()
    }

    @Test
    fun `자세 상세가 근육을 싣는다`() {
        val detail = targetPoseQueryRepository.findDetail(TargetPoseIdentity.of(1L)).shouldNotBeNull()

        detail.imageAssetKey shouldBe "camel-pose"
        detail.level shouldBe 2
        detail.muscles.map { it.name } shouldBe listOf("척추기립근")
        // 자세 상세는 운동 상세와 다른 SQL 이다. 여기서도 앞·뒤 컬럼을 각각 단언하지 않으면
        // 이 쿼리에서만 두 컬럼이 뒤바뀌어도 테스트가 통과한다.
        detail.muscles.map { it.frontHighlightAssetKey } shouldBe listOf(null)
        detail.muscles.map { it.backHighlightAssetKey } shouldBe listOf("erector-spinae-back")
        // 자세에는 핵심 동작 문구가 없다. pose_muscle 에 컬럼이 없어 SQL 이 NULL 을 내보낸다 —
        // 매퍼를 공유하므로 컬럼 이름이 어긋나면 여기서 깨진다.
        detail.muscles.map { it.description } shouldBe listOf(null)
    }

    @Test
    fun `핀포즈는 같은 slug 로 exercise 와 target_pose 양쪽에 존재한다`() {
        val exerciseSlug =
            jdbcClient
                .sql("SELECT ymove_slug FROM catalog.exercise WHERE exercise_id = 1")
                .query(String::class.java)
                .single()
        val poseSlug =
            jdbcClient
                .sql("SELECT ymove_slug FROM catalog.target_pose WHERE target_pose_id = 1")
                .query(String::class.java)
                .single()

        exerciseSlug shouldBe poseSlug
    }

    @Test
    fun `findYmoveSlugs 는 slug 가 있는 운동만 돌려준다`() {
        // 1 번은 camel-pose, 2 번은 slug 가 있고, 90 번은 NULL 이다.
        insertExercise(90L, null, "slug 없는 운동")

        val slugs =
            exerciseQueryRepository.findYmoveSlugs(
                listOf(ExerciseIdentity.of(1L), ExerciseIdentity.of(90L), ExerciseIdentity.of(999L)),
            )

        // NULL 인 행과 없는 식별자는 맵에서 빠진다. "없음" 이 한 가지 모양이어야 호출부가
        // videoUrl = null 로 접는 판단을 한 곳에서 한다.
        slugs shouldBe mapOf(1L to "camel-pose")
    }

    @Test
    fun `운동 상세는 video_url 컬럼을 더 이상 읽지 않는다`() {
        // 컬럼에 값이 남아 있어도 응답에 실리면 안 된다. 재생 URL 은 48 시간 만료라 DB 값이
        // 곧 죽은 URL 이다 — adapter-ymove 가 요청 시점에 채운다 (docs/domains.md §4-3-1).
        insertExercise(95L, "stale-url", "묵은 URL", videoUrl = "https://cdn/expired.mp4")

        val detail = exerciseQueryRepository.findDetail(ExerciseIdentity.of(95L))

        detail.shouldNotBeNull()
        detail.videoUrl.shouldBeNull()
    }

    @Test
    fun `ymove_slug 는 중복을 막지만 비어 있는 것은 여럿 허용한다`() {
        assertThrows<DataIntegrityViolationException> {
            insertExercise(90L, "camel-pose", "중복 slug")
        }

        insertExercise(91L, null, "무명1")
        insertExercise(92L, null, "무명2")

        jdbcClient
            .sql("SELECT count(*) FROM catalog.exercise WHERE ymove_slug IS NULL")
            .query(Int::class.java)
            .single() shouldBe 2
    }

    @Test
    fun `근육 역할에 정의되지 않은 값이 들어가지 않는다`() {
        assertThrows<DataIntegrityViolationException> {
            jdbcClient
                .sql(
                    """
                    INSERT INTO catalog.exercise_muscle (exercise_id, muscle_code, role, display_order)
                    VALUES (2, 'ERECTOR_SPINAE', 'INVALID', 1)
                    """.trimIndent(),
                ).update()
        }
    }

    @Test
    fun `없는 근육을 참조하지 못한다`() {
        assertThrows<DataIntegrityViolationException> {
            insertExerciseMuscle(2L, "존재하지-않는-근육", MuscleRole.STRETCH, 1)
        }
        assertThrows<DataIntegrityViolationException> {
            insertPoseMuscle(1L, "존재하지-않는-근육", MuscleRole.STRETCH, 1)
        }
    }

    @Test
    fun `없는 자세를 참조하지 못한다`() {
        assertThrows<DataIntegrityViolationException> {
            insertPoseMuscle(999L, "ERECTOR_SPINAE", MuscleRole.STRETCH, 1)
        }
    }

    /**
     * 한 자세·운동에서 같은 근육이 두 역할을 갖지 못하게 막는다.
     *
     * docs/domains.md §4-3 이 이 제약을 설계 근거로 명시했다. 감수 결과가 뒤집히면 PK 가 아니라
     * 이 UNIQUE 만 재정의하면 된다는 판단이라, 지금 무엇을 막고 있는지 고정해 둔다.
     */
    @Test
    fun `한 자세에서 같은 근육이 두 번 나오지 못한다`() {
        assertThrows<DataIntegrityViolationException> {
            insertPoseMuscle(1L, "ERECTOR_SPINAE", MuscleRole.STRETCH, 9)
        }
        assertThrows<DataIntegrityViolationException> {
            insertExerciseMuscle(1L, "ERECTOR_SPINAE", MuscleRole.STRETCH, 9)
        }
    }

    @Test
    fun `한 운동에서 큐 순서가 겹치지 못한다`() {
        assertThrows<DataIntegrityViolationException> {
            insertVoiceCue(1L, 1, null, null, "순서 중복")
        }
    }

    /**
     * NULL 은 "기본값 미지정"이고 0 이나 음수는 잘못된 값이다. 둘을 구분하지 않으면 세트 0 회짜리
     * 운동이 코스 스텝으로 내려간다. default_duration_seconds·met_value 와 같은 성질이라
     * 제약도 같이 건다.
     */
    @Test
    fun `기본 세트 수와 반복 수는 0 이나 음수가 될 수 없다`() {
        assertThrows<DataIntegrityViolationException> {
            insertExercise(91L, "zero-set", "세트 0", setCount = 0)
        }
        assertThrows<DataIntegrityViolationException> {
            insertExercise(92L, "negative-rep", "반복 음수", repCount = -1)
        }

        // 미지정은 그대로 통과해야 한다.
        insertExercise(93L, "unset-counts", "미지정", setCount = null, repCount = null)
    }

    @Test
    fun `음수 타임코드와 잘못된 레벨을 막는다`() {
        assertThrows<DataIntegrityViolationException> {
            insertVoiceCue(2L, 1, -1, null, "음수 시작")
        }
        assertThrows<DataIntegrityViolationException> {
            insertTargetPose(90L, "zero-level", "레벨 0", "BACK", 0)
        }
    }

    /**
     * ORDER BY level, target_pose_id 의 두 번째 정렬 키를 확인한다.
     * 레벨이 서로 다른 픽스처만으로는 검증되지 않는다.
     */
    @Test
    fun `같은 레벨은 식별자 순으로 정렬된다`() {
        insertTargetPose(51L, "pose-b", "나중", "ABDOMEN", 1)
        insertTargetPose(50L, "pose-a", "먼저", "ABDOMEN", 1)

        targetPoseQueryRepository
            .findAll("ABDOMEN")
            .map { it.targetPoseId } shouldBe listOf(50L, 51L)
    }

    @Test
    fun `음성 큐는 종료가 시작보다 앞설 수 없다`() {
        assertThrows<DataIntegrityViolationException> {
            insertVoiceCue(2L, 1, 50, 40, "역전된 구간")
        }
    }

    @Test
    fun `음성 큐는 시작 없이 종료만 가질 수 없다`() {
        assertThrows<DataIntegrityViolationException> {
            insertVoiceCue(2L, 1, null, 40, "시작 없는 구간")
        }
    }

    /**
     * 컬럼을 명시한다. VALUES 만 나열하면 컬럼이 늘어날 때마다 조용히 깨진다.
     *
     * frontKey·backKey 를 따로 받는 것은 근육이 앞·뒤 중 한쪽에만 보이는 경우를 픽스처로
     * 재현하기 위해서다.
     */
    private fun insertMuscle(
        code: String,
        name: String,
        bodyPartCode: String,
        frontKey: String? = null,
        backKey: String? = null,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO catalog.muscle (muscle_code, name, body_part_code,
                                        front_highlight_asset_key, back_highlight_asset_key)
            VALUES (:code, :name, :bodyPartCode, :frontKey, :backKey)
            """.trimIndent(),
        ).param("code", code)
        .param("name", name)
        .param("bodyPartCode", bodyPartCode)
        .param("frontKey", frontKey)
        .param("backKey", backKey)
        .update()

    private fun insertExercise(
        id: Long,
        slug: String?,
        name: String,
        setCount: Int? = 3,
        repCount: Int? = null,
        category: String? = "가동성 웜업",
        imageAssetKey: String? = null,
        videoUrl: String? = null,
        thumbnailUrl: String? = null,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO catalog.exercise (exercise_id, ymove_slug, name, image_asset_key, video_url,
                                          thumbnail_url, default_set_count, default_rep_count,
                                          default_duration_seconds, met_value, difficulty, category, caution_note)
            VALUES (:id, :slug, :name, :imageAssetKey, :videoUrl,
                    :thumbnailUrl, :setCount, :repCount, 120, 2.30, '하', :category, '통증이 오면 중단하세요')
            """.trimIndent(),
        ).param("id", id)
        .param("slug", slug)
        .param("name", name)
        .param("imageAssetKey", imageAssetKey)
        .param("videoUrl", videoUrl)
        .param("thumbnailUrl", thumbnailUrl)
        .param("setCount", setCount)
        .param("repCount", repCount)
        .param("category", category)
        .update()

    private fun insertTargetPose(
        id: Long,
        slug: String,
        name: String,
        bodyPartCode: String,
        level: Int,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO catalog.target_pose (target_pose_id, ymove_slug, name, image_asset_key,
                                             body_part_code, level)
            VALUES (:id, :slug, :name, :slug, :bodyPartCode, :level)
            """.trimIndent(),
        ).param("id", id)
        .param("slug", slug)
        .param("name", name)
        .param("bodyPartCode", bodyPartCode)
        .param("level", level)
        .update()

    private fun insertExerciseMuscle(
        exerciseId: Long,
        muscleCode: String,
        role: MuscleRole,
        displayOrder: Int,
        description: String? = null,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO catalog.exercise_muscle (exercise_id, muscle_code, role, display_order, description)
            VALUES (:exerciseId, :muscleCode, :role, :displayOrder, :description)
            """.trimIndent(),
        ).param("exerciseId", exerciseId)
        .param("muscleCode", muscleCode)
        .param("role", role.name)
        .param("displayOrder", displayOrder)
        .param("description", description)
        .update()

    private fun insertPoseMuscle(
        targetPoseId: Long,
        muscleCode: String,
        role: MuscleRole,
        displayOrder: Int,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO catalog.pose_muscle (target_pose_id, muscle_code, role, display_order)
            VALUES (:targetPoseId, :muscleCode, :role, :displayOrder)
            """.trimIndent(),
        ).param("targetPoseId", targetPoseId)
        .param("muscleCode", muscleCode)
        .param("role", role.name)
        .param("displayOrder", displayOrder)
        .update()

    private fun insertVoiceCue(
        exerciseId: Long,
        displayOrder: Int,
        start: Int?,
        end: Int?,
        content: String,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order,
                                                    start_offset_seconds, end_offset_seconds, content)
            VALUES (:exerciseId, :displayOrder, :start, :end, :content)
            """.trimIndent(),
        ).param("exerciseId", exerciseId)
        .param("displayOrder", displayOrder)
        .param("start", start)
        .param("end", end)
        .param("content", content)
        .update()

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}
