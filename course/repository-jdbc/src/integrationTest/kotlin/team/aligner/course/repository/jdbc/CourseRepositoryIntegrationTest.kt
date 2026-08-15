package team.aligner.course.repository.jdbc

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
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.jdbc.core.simple.JdbcClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.model.Course
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.Stamp
import team.aligner.course.repository.jdbc.bootstrap.CourseRepositoryTestApplication
import java.time.Instant

/**
 * 러너는 Kotest 가 아니라 JUnit5 다. kotest-extensions-spring 이 버전 카탈로그에 없다.
 * 단언만 kotest-assertions-core 를 쓴다.
 *
 * 픽스처를 seed 가 아니라 테스트가 직접 넣는다. seed changeset 은 후속 이슈이고, 감수 데이터가
 * 바뀔 때마다 이 테스트가 깨지면 안 된다.
 *
 * 여기서 처음으로 확인되는 것들이다.
 * - 손자까지 있는 애그리거트(course → step → exercise)가 한 번의 save 로 오가는가
 * - @Table(schema = "course") 가 실제로 먹었는가
 * - (member_id, target_pose_id) 유니크가 추천 멱등성을 실제로 강제하는가
 * - 도장 유니크가 중복 부여를 막는가
 */
@Testcontainers
@SpringBootTest(classes = [CourseRepositoryTestApplication::class])
class CourseRepositoryIntegrationTest {
    @Autowired
    private lateinit var courseRepository: CourseRepository

    @Autowired
    private lateinit var courseTemplateRepository: CourseTemplateRepository

    @Autowired
    private lateinit var courseQueryRepository: CourseQueryRepository

    @Autowired
    private lateinit var stampRepository: StampRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun `픽스처를 새로 넣는다`() {
        jdbcClient
            .sql(
                """
                TRUNCATE course.stamp, course.course_step_exercise, course.course_step, course.course,
                         course.template_step_exercise, course.template_step, course.course_template
                RESTART IDENTITY CASCADE
                """.trimIndent(),
            ).update()

        insertTemplate(templateId = 1L, targetPoseId = TARGET_POSE_ID, name = "낙타자세 정복하기")
        insertTemplateStep(templateStepId = 1L, templateId = 1L, stepOrder = 1)
        insertTemplateStepExercise(1L, exerciseId = 10L, displayOrder = 1, durationSeconds = 120, setCount = 1)
        insertTemplateStep(templateStepId = 2L, templateId = 1L, stepOrder = 2)
        insertTemplateStepExercise(2L, exerciseId = 11L, displayOrder = 1, durationSeconds = null, setCount = null)
    }

    @Test
    fun `changelog 가 course 스키마에 테이블을 만든다`() {
        jdbcClient
            .sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'course'")
            .query(Int::class.java)
            .single() shouldBe 7

        jdbcClient
            .sql("SELECT count(*) FROM public.databasechangelog WHERE id LIKE 'course-%'")
            .query(Int::class.java)
            .single() shouldBe 12
    }

    @Test
    fun `도메인 테이블이 public 에 새지 않는다`() {
        listOf("course", "course_step", "course_step_exercise", "course_template", "stamp")
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
    fun `템플릿이 스텝과 운동을 순서대로 돌려준다`() {
        val template = courseTemplateRepository.findByTargetPoseId(TARGET_POSE_ID).shouldNotBeNull()

        template.name shouldBe "낙타자세 정복하기"
        template.steps.map { it.stepOrder } shouldBe listOf(1, 2)
        template.steps
            .first()
            .exercises
            .map { it.exerciseId } shouldBe listOf(10L)
        // override 가 없는 스텝은 null 로 남는다. 0 이 되면 catalog 기본값으로 메우는
        // 분기가 깨진다.
        template.steps
            .last()
            .exercises
            .first()
            .durationSeconds
            .shouldBeNull()
        template.steps
            .last()
            .exercises
            .first()
            .setCount
            .shouldBeNull()
    }

    @Test
    fun `손자까지 있는 애그리거트가 한 번에 저장되고 되읽힌다`() {
        val saved = courseRepository.save(recommended())
        val identity = saved.identity.shouldNotBeNull()

        val found = courseRepository.findByIdentity(identity).shouldNotBeNull()

        found.steps.map { it.stepOrder } shouldBe listOf(1, 2)
        found.steps
            .first()
            .exercises
            .map { it.exerciseId } shouldBe listOf(10L)
        found.steps
            .first()
            .exercises
            .first()
            .durationSeconds shouldBe 120
        found.causeCode shouldBe "WEAK_BACK"
        found.status shouldBe CourseStatus.IN_PROGRESS

        jdbcClient
            .sql("SELECT count(*) FROM course.course_step_exercise")
            .query(Int::class.java)
            .single() shouldBe 2
    }

    /**
     * 하나의 핀포즈가 하나의 코스다. 추천이 재시도돼도 코스가 늘지 않아야 한다
     * (docs/domains.md §4-4).
     */
    @Test
    fun `같은 회원의 같은 자세로 코스를 두 개 만들지 못한다`() {
        courseRepository.save(recommended())

        assertThrows<DataIntegrityViolationException> {
            courseRepository.save(recommended())
        }
    }

    @Test
    fun `회원과 자세로 기존 코스를 찾는다`() {
        val saved = courseRepository.save(recommended())

        courseRepository
            .findByMemberIdAndTargetPoseId(MEMBER_ID, TARGET_POSE_ID)
            .shouldNotBeNull()
            .identity shouldBe saved.identity
        courseRepository.findByMemberIdAndTargetPoseId(MEMBER_ID + 1, TARGET_POSE_ID).shouldBeNull()
    }

    @Test
    fun `스텝을 완료하면 진행도가 저장된다`() {
        val saved = courseRepository.save(recommended())
        val identity = saved.identity.shouldNotBeNull()

        courseRepository.save(saved.completeStep(stepOrder = 1, at = AT))

        val found = courseRepository.findByIdentity(identity).shouldNotBeNull()
        found.completedStepCount shouldBe 1
        found.currentStepOrder shouldBe 2
        found.steps.first { it.stepOrder == 1 }.completedAt shouldBe AT
    }

    /**
     * status 와 completed_at 이 따로 놀지 않게 CHECK 이 막는다.
     */
    @Test
    fun `완료 상태인데 완료 시각이 없으면 DB 가 막는다`() {
        val saved = courseRepository.save(recommended())

        assertThrows<DataIntegrityViolationException> {
            jdbcClient
                .sql("UPDATE course.course SET status = 'COMPLETED' WHERE course_id = :id")
                .param("id", saved.identity.shouldNotBeNull().value)
                .update()
        }
    }

    /**
     * 세션 완료 push 는 재시도되는 경로다. 두 번째 호출이 예외가 되면 정상 재시도가 500 이 된다.
     *
     * **새로 붙었는지를 반환값이 알려줘야 한다.** 서비스가 "방금 코스가 완료됐나" 로 짐작하면
     * 두 요청이 겹칠 때 둘 다 획득으로 판단할 수 있다.
     */
    @Test
    fun `도장은 회차당 한 번만 붙고 두 번째 저장은 false 다`() {
        val saved = courseRepository.save(recommended())
        val courseId = saved.identity.shouldNotBeNull().value
        val stamp = Stamp.acquire(MEMBER_ID, TARGET_POSE_ID, courseId, attemptNo = 1, at = AT)

        stampRepository.saveIfAbsent(stamp) shouldBe true
        // 재시도를 흉내낸다. 예외가 아니라 false 여야 한다.
        stampRepository.saveIfAbsent(stamp) shouldBe false

        stampRepository.countAcquired(MEMBER_ID, TARGET_POSE_ID) shouldBe 1
    }

    /**
     * **파이어로그가 여기서 나온다.** 도장이 자세당 하나면 두 번째 완주를 기록할 자리가 없어
     * 완료 리포트의 `1 / 4회` 가 2 로 올라가지 못한다.
     */
    @Test
    fun `회차가 다르면 같은 자세에 도장이 또 붙는다`() {
        val saved = courseRepository.save(recommended())
        val courseId = saved.identity.shouldNotBeNull().value

        stampRepository.saveIfAbsent(Stamp.acquire(MEMBER_ID, TARGET_POSE_ID, courseId, attemptNo = 1, at = AT)) shouldBe true
        stampRepository.saveIfAbsent(Stamp.acquire(MEMBER_ID, TARGET_POSE_ID, courseId, attemptNo = 2, at = AT)) shouldBe true

        stampRepository.countAcquired(MEMBER_ID, TARGET_POSE_ID) shouldBe 2
    }

    /**
     * 다시 시작하면 스텝이 처음 상태로 돌아가고 회차가 오른다. 회차가 저장되지 않으면 다음
     * 완주가 1 회차 도장과 충돌해 조용히 묻힌다.
     */
    @Test
    fun `재도전은 스텝을 되돌리고 회차를 올린다`() {
        val saved = courseRepository.save(recommended())
        val identity = saved.identity.shouldNotBeNull()

        val completed =
            (1..saved.totalStepCount).fold(saved) { course, order -> course.completeStep(order, AT) }
        courseRepository.save(completed)

        courseRepository.save(courseRepository.findByIdentity(identity).shouldNotBeNull().restart())

        val restarted = courseRepository.findByIdentity(identity).shouldNotBeNull()
        restarted.attemptNo shouldBe 2
        restarted.completedStepCount shouldBe 0
        restarted.completedAt.shouldBeNull()
    }

    /**
     * 애그리거트를 통째로 저장하므로 버전이 없으면 나중 저장이 앞선 완료를 지운다.
     * training 이 세션 완료를 push 하고 재시도까지 하므로 실제로 겹칠 수 있다.
     *
     * 같은 시점에 읽은 두 사본으로 서로 다른 스텝을 완료해 그 상황을 재현한다.
     */
    @Test
    fun `같은 버전을 읽은 두 저장 중 나중 것이 낙관적 락에 걸린다`() {
        val saved = courseRepository.save(recommended())
        val identity = saved.identity.shouldNotBeNull()

        val first = courseRepository.findByIdentity(identity).shouldNotBeNull()
        val second = courseRepository.findByIdentity(identity).shouldNotBeNull()

        courseRepository.save(first.completeStep(stepOrder = 1, at = AT))

        assertThrows<OptimisticLockingFailureException> {
            courseRepository.save(second.completeStep(stepOrder = 2, at = AT))
        }

        // 앞선 완료가 남아 있어야 한다. 버전이 없으면 여기서 0 이 된다.
        courseRepository
            .findByIdentity(identity)
            .shouldNotBeNull()
            .completedStepCount shouldBe 1
    }

    @Test
    fun `진행 중인 코스를 오늘의 코스로 집는다`() {
        val saved = courseRepository.save(recommended())

        val skeleton = courseQueryRepository.findInProgressCourseSkeleton(MEMBER_ID).shouldNotBeNull()

        skeleton.courseId shouldBe saved.identity.shouldNotBeNull().value
        skeleton.templateName shouldBe "낙타자세 정복하기"
        skeleton.totalStepCount shouldBe 2
        skeleton.completedStepCount shouldBe 0
        skeleton.currentStepOrder shouldBe 1
        skeleton.steps
            .first()
            .exercises
            .map { it.exerciseId } shouldBe listOf(10L)
    }

    @Test
    fun `진행 중인 코스가 없으면 null 이다`() {
        courseQueryRepository.findInProgressCourseSkeleton(MEMBER_ID).shouldBeNull()
    }

    /**
     * 남의 코스와 없는 코스를 같은 결과로 돌려준다. 구분해서 알려주면 존재 여부가 새어나간다.
     */
    @Test
    fun `남의 코스는 식별자로도 읽지 못한다`() {
        val saved = courseRepository.save(recommended())
        val courseId = saved.identity.shouldNotBeNull().value

        courseQueryRepository.findCourseSkeleton(courseId, MEMBER_ID).shouldNotBeNull()
        courseQueryRepository.findCourseSkeleton(courseId, MEMBER_ID + 1).shouldBeNull()
    }

    /**
     * 자세 도전 현황의 `3 / 4` 를 SQL 집계가 실제로 만들어내는지 본다.
     */
    @Test
    fun `도전 현황이 완료 스텝 수를 집계한다`() {
        val saved = courseRepository.save(recommended())
        courseRepository.save(saved.completeStep(stepOrder = 1, at = AT))

        val progress = courseQueryRepository.findAllCourseSkeletons(MEMBER_ID).single()

        progress.completedStepCount shouldBe 1
        progress.totalStepCount shouldBe 2
        progress.currentStepOrder shouldBe 2
        progress.completed shouldBe false
        // 목록은 스텝 내역을 그리지 않는다.
        progress.steps shouldBe emptyList()
    }

    @Test
    fun `모든 스텝을 완료하면 도전 현황이 완성으로 나온다`() {
        var course = courseRepository.save(recommended())
        course = courseRepository.save(course.completeStep(stepOrder = 1, at = AT))
        courseRepository.save(course.completeStep(stepOrder = 2, at = AT))

        val progress = courseQueryRepository.findAllCourseSkeletons(MEMBER_ID).single()

        progress.completed shouldBe true
        progress.completedStepCount shouldBe 2
        progress.currentStepOrder.shouldBeNull()
    }

    private fun recommended(): Course =
        Course.recommend(
            memberId = MEMBER_ID,
            template = courseTemplateRepository.findByTargetPoseId(TARGET_POSE_ID)!!,
            causeCode = "WEAK_BACK",
        )

    private fun insertTemplate(
        templateId: Long,
        targetPoseId: Long,
        name: String,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO course.course_template (template_id, target_pose_id, name, recommendation_reason)
            VALUES (:templateId, :targetPoseId, :name, '등과 골반 근육 강화에 집중해 보세요')
            """.trimIndent(),
        ).param("templateId", templateId)
        .param("targetPoseId", targetPoseId)
        .param("name", name)
        .update()

    private fun insertTemplateStep(
        templateStepId: Long,
        templateId: Long,
        stepOrder: Int,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO course.template_step (template_step_id, template_id, step_order)
            VALUES (:templateStepId, :templateId, :stepOrder)
            """.trimIndent(),
        ).param("templateStepId", templateStepId)
        .param("templateId", templateId)
        .param("stepOrder", stepOrder)
        .update()

    private fun insertTemplateStepExercise(
        templateStepId: Long,
        exerciseId: Long,
        displayOrder: Int,
        durationSeconds: Int?,
        setCount: Int?,
    ) = jdbcClient
        .sql(
            """
            INSERT INTO course.template_step_exercise (template_step_id, exercise_id, display_order,
                                                       duration_seconds, set_count)
            VALUES (:templateStepId, :exerciseId, :displayOrder, :durationSeconds, :setCount)
            """.trimIndent(),
        ).param("templateStepId", templateStepId)
        .param("exerciseId", exerciseId)
        .param("displayOrder", displayOrder)
        .param("durationSeconds", durationSeconds)
        .param("setCount", setCount)
        .update()

    companion object {
        private const val MEMBER_ID = 1L
        private const val TARGET_POSE_ID = 3L
        private val AT: Instant = Instant.parse("2026-08-09T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }
}
