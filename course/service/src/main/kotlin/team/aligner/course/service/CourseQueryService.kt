package team.aligner.course.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.ExerciseCatalogEntry
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.ExerciseComposition
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.TargetPoseCatalogEntry
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.model.Stamp
import team.aligner.course.model.exception.CourseNotFoundException
import team.aligner.course.model.exception.InProgressCourseNotFoundException
import team.aligner.course.model.view.CourseDetailView
import team.aligner.course.model.view.CourseStepExerciseView
import team.aligner.course.model.view.CourseStepView
import team.aligner.course.model.view.CourseTemplateStepExerciseView
import team.aligner.course.model.view.CourseTemplateStepView
import team.aligner.course.model.view.CourseTemplateView
import team.aligner.course.model.view.TargetPoseProgressSummaryView
import team.aligner.course.model.view.TargetPoseProgressView
import team.aligner.course.model.view.TodayCourseView
import team.aligner.course.model.view.TomorrowCoursePreviewView
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

interface CourseQueryService {
    fun getTodayCourse(memberId: Long): TodayCourseView

    fun getCourseDetail(
        memberId: Long,
        courseId: Long,
    ): CourseDetailView

    /** `completedOnly` 가 true 면 프로필의 "완수한 자세 목록" 이다. */
    fun getTargetPoseProgress(
        memberId: Long,
        completedOnly: Boolean?,
    ): TargetPoseProgressSummaryView

    /**
     * 운영 목록용 코스 템플릿 전체.
     *
     * **memberId 를 받지 않는다.** 회원별 인스턴스가 아니라 마스터라 회원과 무관하다 —
     * 이 인터페이스에서 유일하게 회원을 묻지 않는 조회다.
     */
    fun getAllCourseTemplates(): List<CourseTemplateView>
}

/**
 * CommandService 를 주입받지 않는다. Query 는 조회 모델에 직결한다 (docs/architecture.md §4).
 *
 * **catalog·member 값을 여기서 붙인다.** 리포지토리는 course 스키마만으로 아는 뼈대를
 * 돌려주고, 자세 이름·운동 이름·MET·몸무게는 port 로 받아 조립한다. SQL 이 도메인 경계를
 * 넘지 않게 하려는 것이다 (docs/domains.md §6).
 *
 * `@Transactional` 을 클래스에 붙이는 이유는 CourseCommandServiceImpl 주석 참고.
 */
@Transactional(readOnly = true)
internal class CourseQueryServiceImpl(
    private val courseQueryRepository: CourseQueryRepository,
    // 운영 목록이 읽는 템플릿 마스터다. 쓰기가 없는 seed 라 Command 전용 리포지토리가 아니고,
    // 여기서 읽어도 Command 를 Query 로 끌어오는 것이 아니다 (CourseTemplateRepository 주석).
    private val courseTemplateRepository: CourseTemplateRepository,
    private val targetPoseCatalogPort: TargetPoseCatalogPort,
    private val exerciseCatalogPort: ExerciseCatalogPort,
    private val memberBodyPort: MemberBodyPort,
    /**
     * "오늘" 의 경계를 정하는 데만 쓴다. 기본값을 두는 것은 이 판단이 설정이 아니라 도메인
     * 규칙이어서다 — 배포 서버의 시간대가 무엇이든 회원의 하루는 한국 기준이다.
     */
    private val clock: Clock = Clock.system(SEOUL),
) : CourseQueryService {
    /**
     * 홈 카드. **"오늘의 코스" 는 진행 중인 코스의 다른 이름**이다 — 일자 개념이 없다
     * (docs/domains.md §4-4).
     *
     * 다만 **오늘 완주한 코스까지는 오늘의 코스로 본다.** 마지막 스텝을 끝내는 순간 코스가
     * COMPLETED 가 되는데 거기서 404 를 내면 홈이 완료 상태를 그릴 수 없다. 완주한 코스에는
     * 「내일 운동 미리보기」가 함께 실린다.
     */
    override fun getTodayCourse(memberId: Long): TodayCourseView {
        val skeleton =
            courseQueryRepository.findTodayCourseSkeleton(memberId, startOfToday())
                ?: throw InProgressCourseNotFoundException()

        val pose = targetPoseCatalogPort.findAllByIds(listOf(skeleton.targetPoseId)).firstOrNull()
        val rows = skeleton.steps.flatMap { it.exercises }
        val weightKg = memberBodyPort.findWeightKg(memberId)
        val totals = totalsOf(rows, loadExercises(rows), weightKg)

        return TodayCourseView(
            courseId = skeleton.courseId,
            targetPoseId = skeleton.targetPoseId,
            targetPoseName = pose.displayName(),
            targetPoseImageAssetKey = pose?.imageAssetKey,
            targetPoseLevel = pose?.level,
            name = skeleton.templateName,
            recommendationReason = skeleton.recommendationReason,
            currentStepOrder = skeleton.currentStepOrder,
            completedStepCount = skeleton.completedStepCount,
            totalStepCount = skeleton.totalStepCount,
            exerciseCount = totals.exerciseCount,
            totalSetCount = totals.setCount,
            estimatedDurationSeconds = totals.durationSeconds,
            estimatedKcal = totals.kcal,
            completed = skeleton.completed,
            // 진행 중인 코스에는 미리보기가 없다. 오늘 할 일이 남아 있는데 내일 것을 먼저
            // 보여줄 자리가 화면에 없다.
            tomorrowPreview =
                when {
                    skeleton.completed -> findTomorrowPreview(memberId, pose, weightKg)
                    else -> null
                },
        )
    }

    /**
     * 「내일 운동 미리보기」의 후보를 고르고 카드를 만든다.
     *
     * **같은 부위에서 아직 4 번 완수하지 못한 자세 중 하나를 무작위로** 집는다. 방금 완주한
     * 자세도 후보에 남긴다 — 자세 하나를 완성하려면 같은 코스를 4 번 완주해야 하므로 같은
     * 자세가 다시 나오는 것이 정상 루프다.
     *
     * **난수는 회원과 날짜로 고정한다.** 매번 새로 뽑으면 홈을 다시 불러올 때마다 카드의
     * 자세가 바뀐다 — 회원은 그것을 "내일 할 운동" 으로 읽으므로 하루 안에서는 같은 답이
     * 나와야 한다. 저장하지 않고 씨앗만 고정하는 것이라 예약이 생긴 것은 아니다.
     *
     * **미리보기가 없어도 홈은 그려진다.** 부위를 알 수 없거나(catalog 에 자세가 없다) 그
     * 부위를 모두 완성했거나 코스 템플릿 seed 가 없으면 null 이다. 미리보기 하나 때문에
     * 완료 화면 전체를 실패시키지 않는다.
     */
    private fun findTomorrowPreview(
        memberId: Long,
        todayPose: TargetPoseCatalogEntry?,
        weightKg: Int?,
    ): TomorrowCoursePreviewView? {
        val bodyPartCode = todayPose?.bodyPartCode ?: return null
        val stampCounts =
            courseQueryRepository
                .findStampCounts(memberId)
                .associate { it.targetPoseId to it.acquiredStampCount }

        val picked =
            targetPoseCatalogPort
                .findAll()
                .filter { pose ->
                    pose.bodyPartCode == bodyPartCode &&
                        (stampCounts[pose.targetPoseId] ?: 0) < Stamp.REQUIRED_COUNT
                }.randomOrNull(Random(previewSeed(memberId))) ?: return null

        val composition = compositionOf(memberId, picked.targetPoseId) ?: return null
        val totals = totalsOf(composition.exercises, loadExercises(composition.exercises), weightKg)

        return TomorrowCoursePreviewView(
            targetPoseId = picked.targetPoseId,
            targetPoseName = picked.name,
            targetPoseImageAssetKey = picked.imageAssetKey,
            bodyPartCode = picked.bodyPartCode,
            level = picked.level,
            name = composition.name,
            recommendationReason = composition.recommendationReason,
            totalStepCount = composition.totalStepCount,
            exerciseCount = totals.exerciseCount,
            totalSetCount = totals.setCount,
            estimatedDurationSeconds = totals.durationSeconds,
            estimatedKcal = totals.kcal,
        )
    }

    /**
     * 미리보기가 셀 코스 구성. **회원의 코스가 있으면 그것이 먼저다.**
     *
     * 코스 스텝은 추천 시점에 템플릿에서 복사되므로 seed 가 나중에 바뀌면 둘이 갈린다.
     * 회원이 내일 실제로 수행할 것은 복사본 쪽이라, 템플릿 숫자를 보여주면 카드와 실제
     * 코스가 어긋난다. 한 번도 열지 않은 자세만 템플릿에서 센다.
     */
    private fun compositionOf(
        memberId: Long,
        targetPoseId: Long,
    ): PreviewComposition? {
        courseQueryRepository.findCourseSkeletonByTargetPoseId(memberId, targetPoseId)?.let { course ->
            return PreviewComposition(
                name = course.templateName,
                recommendationReason = course.recommendationReason,
                totalStepCount = course.totalStepCount,
                exercises = course.steps.flatMap { it.exercises },
            )
        }

        val template = courseQueryRepository.findTemplateSkeleton(targetPoseId) ?: return null
        return PreviewComposition(
            name = template.templateName,
            recommendationReason = template.recommendationReason,
            totalStepCount = template.totalStepCount,
            exercises = template.exercises,
        )
    }

    override fun getCourseDetail(
        memberId: Long,
        courseId: Long,
    ): CourseDetailView {
        val skeleton =
            courseQueryRepository.findCourseSkeleton(courseId, memberId)
                ?: throw CourseNotFoundException()

        val pose = targetPoseCatalogPort.findAllByIds(listOf(skeleton.targetPoseId)).firstOrNull()
        val rows = skeleton.steps.flatMap { it.exercises }
        val exercises = loadExercises(rows)
        val weightKg = memberBodyPort.findWeightKg(memberId)
        val totals = totalsOf(rows, exercises, weightKg)

        return CourseDetailView(
            courseId = skeleton.courseId,
            targetPoseId = skeleton.targetPoseId,
            targetPoseName = pose.displayName(),
            targetPoseImageAssetKey = pose?.imageAssetKey,
            name = skeleton.templateName,
            recommendationReason = skeleton.recommendationReason,
            completedStepCount = skeleton.completedStepCount,
            totalStepCount = skeleton.totalStepCount,
            exerciseCount = totals.exerciseCount,
            totalSetCount = totals.setCount,
            estimatedDurationSeconds = totals.durationSeconds,
            estimatedKcal = totals.kcal,
            steps =
                skeleton.steps.map { step ->
                    CourseStepView(
                        courseStepId = step.courseStepId,
                        stepOrder = step.stepOrder,
                        completed = step.completed,
                        completedAt = step.completedAt,
                        exercises =
                            step.exercises.map { exercise ->
                                val catalog = exercises[exercise.exerciseId]
                                val duration = exercise.durationSeconds ?: catalog?.defaultDurationSeconds
                                CourseStepExerciseView(
                                    courseStepExerciseId = exercise.courseStepExerciseId,
                                    exerciseId = exercise.exerciseId,
                                    name = catalog?.name ?: UNKNOWN_NAME,
                                    imageAssetKey = catalog?.imageAssetKey,
                                    thumbnailUrl = catalog?.thumbnailUrl,
                                    category = catalog?.category,
                                    displayOrder = exercise.displayOrder,
                                    durationSeconds = duration,
                                    setCount = exercise.setCount ?: catalog?.defaultSetCount,
                                    estimatedKcal =
                                        CalorieCalculator.calculate(catalog?.metValue, weightKg, duration),
                                )
                            },
                    )
                },
        )
    }

    /**
     * 자세 도전 현황.
     *
     * **카탈로그의 자세 전체에서 출발하고 회원의 코스를 그 위에 얹는다.** 반대가 아니다 —
     * 코스는 추천이라 회원이 아직 시작하지 않은 자세도 화면에 나와야 한다. 시작하지 않은
     * 자세는 코스 쪽 값이 전부 null 로 나간다.
     *
     * **`3 / 4` 는 파이어로그다.** 코스를 한 번 완주할 때마다 도장이 하나 붙고, 4 개를 채워야
     * `completed` 다 — 코스 안에서 완료한 스텝 수가 아니다. 그래서 코스 뼈대만으로는 이 화면을
     * 그릴 수 없고 도장 수를 함께 읽는다.
     *
     * 집계는 **거르기 전 전체**로 센다. 칩 세 개가 언제나 함께 보이므로 걸러진 목록으로
     * 세면 나머지 칩을 그릴 수 없다.
     *
     * 프로필의 "완수한 자세 목록" 도 `completedOnly` 로 같은 조회를 쓴다 — 별도 API 를
     * 만들지 않는다.
     */
    override fun getTargetPoseProgress(
        memberId: Long,
        completedOnly: Boolean?,
    ): TargetPoseProgressSummaryView {
        // 회원 코스는 자세 하나에 최대 하나다 — (member_id, target_pose_id) 유니크가 DB 에서
        // 그것을 강제한다. 그래서 associateBy 가 값을 덮어쓸 걱정이 없다.
        val coursesByTargetPoseId =
            courseQueryRepository
                .findAllCourseSkeletons(memberId)
                .associateBy { it.targetPoseId }
        val stampCountsByTargetPoseId =
            courseQueryRepository
                .findStampCounts(memberId)
                .associate { it.targetPoseId to it.acquiredStampCount }

        // catalog 가 (부위, 레벨, 식별자) 순으로 정렬해 준다. 부위 섹션의 노출 순서는
        // 화면이 GET /screening/body-parts 로 정하므로 여기서 다시 정렬하지 않는다.
        val all =
            targetPoseCatalogPort.findAll().map { pose ->
                val course = coursesByTargetPoseId[pose.targetPoseId]
                // 도장이 없으면 0 이다. 다만 코스를 아직 시작하지 않았으면 0 이 아니라 null 로
                // 내린다 — 0/4 는 "시작했는데 아직 완주 못 함" 이라 뜻이 다르다.
                val stampCount = stampCountsByTargetPoseId[pose.targetPoseId] ?: 0
                TargetPoseProgressView(
                    targetPoseId = pose.targetPoseId,
                    targetPoseName = pose.name,
                    targetPoseImageAssetKey = pose.imageAssetKey,
                    bodyPartCode = pose.bodyPartCode,
                    level = pose.level,
                    courseId = course?.courseId,
                    completedStepCount = course?.completedStepCount,
                    totalStepCount = course?.totalStepCount,
                    acquiredStampCount = course?.let { stampCount },
                    requiredStampCount = Stamp.REQUIRED_COUNT,
                    completed = stampCount >= Stamp.REQUIRED_COUNT,
                )
            }

        return TargetPoseProgressSummaryView(
            totalCount = all.size,
            inProgressCount = all.count { it.inProgress },
            completedCount = all.count { it.completed },
            targetPoses =
                when (completedOnly) {
                    null -> all
                    else -> all.filter { it.completed == completedOnly }
                },
        )
    }

    /** 오늘의 시작 시각. 회원의 하루는 배포 서버의 시간대가 아니라 한국 기준이다. */
    private fun startOfToday(): Instant = today().atStartOfDay(SEOUL).toInstant()

    private fun today(): LocalDate = LocalDate.ofInstant(clock.instant(), SEOUL)

    /**
     * 미리보기 난수의 씨앗. 회원과 날짜가 같으면 같은 자세가 나오고 날이 바뀌면 달라진다.
     *
     * 회원 식별자만 쓰면 그 회원에게 영원히 같은 자세만 나오고, 날짜만 쓰면 모든 회원이 같은
     * 자세를 본다. 둘을 섞는다.
     */
    private fun previewSeed(memberId: Long): Int = (memberId * 31 + today().toEpochDay()).toInt()

    /**
     * 코스 템플릿 전체. 감수자가 적재된 코스 정본을 눈으로 확인하는 목록이다.
     *
     * **자세·운동을 템플릿마다 부르지 않는다.** 전체에서 식별자를 한 번에 모아 port 를 두 번만
     * 친다 — 템플릿 9 개 × 스텝 7 개면 그러지 않을 경우 호출이 수십 번이 된다.
     */
    override fun getAllCourseTemplates(): List<CourseTemplateView> {
        val templates = courseTemplateRepository.findAll()
        if (templates.isEmpty()) {
            return emptyList()
        }

        val poses =
            targetPoseCatalogPort
                .findAllByIds(templates.map { it.targetPoseId }.distinct())
                .associateBy { it.targetPoseId }
        val exerciseIds =
            templates
                .flatMap { template -> template.steps.flatMap { step -> step.exercises.map { it.exerciseId } } }
                .distinct()
        val exercises =
            when {
                exerciseIds.isEmpty() -> emptyMap()
                else -> exerciseCatalogPort.findAllByIds(exerciseIds).associateBy { it.exerciseId }
            }

        return templates.map { template ->
            CourseTemplateView(
                templateId = template.templateId,
                targetPoseId = template.targetPoseId,
                targetPoseName = poses[template.targetPoseId].displayName(),
                name = template.name,
                recommendationReason = template.recommendationReason,
                stepCount = template.steps.size,
                exerciseCount = template.steps.sumOf { it.exercises.size },
                steps =
                    template.steps.map { step ->
                        CourseTemplateStepView(
                            stepOrder = step.stepOrder,
                            exercises =
                                step.exercises.map { exercise ->
                                    val catalog = exercises[exercise.exerciseId]
                                    CourseTemplateStepExerciseView(
                                        exerciseId = exercise.exerciseId,
                                        name = catalog?.name ?: UNKNOWN_NAME,
                                        imageAssetKey = catalog?.imageAssetKey,
                                        category = catalog?.category,
                                        displayOrder = exercise.displayOrder,
                                        durationSeconds =
                                            exercise.durationSeconds ?: catalog?.defaultDurationSeconds,
                                        setCount = exercise.setCount ?: catalog?.defaultSetCount,
                                    )
                                },
                        )
                    },
            )
        }
    }

    /**
     * 코스에 실린 운동을 한 번에 읽는다. 스텝마다 부르면 조회가 스텝 수만큼 늘어난다.
     */
    private fun loadExercises(rows: List<ExerciseComposition>): Map<Long, ExerciseCatalogEntry> {
        val ids = rows.map { it.exerciseId }.distinct()
        if (ids.isEmpty()) {
            return emptyMap()
        }
        return exerciseCatalogPort.findAllByIds(ids).associateBy { it.exerciseId }
    }

    /**
     * 코스 카드의 합계. **칼로리는 스텝 합**이다 (docs/domains.md §4-3).
     *
     * 회원의 코스 스텝과 템플릿 스텝이 같은 계산을 탄다. 미리보기가 두 출처를 오가는데
     * 합계 규칙이 갈리면 같은 화면에 다른 숫자가 나온다.
     */
    private fun totalsOf(
        rows: List<ExerciseComposition>,
        exercises: Map<Long, ExerciseCatalogEntry>,
        weightKg: Int?,
    ): CourseTotals {
        val durations = mutableListOf<Int?>()
        val kcals = mutableListOf<Int?>()
        var setCount = 0

        rows.forEach { row ->
            val catalog = exercises[row.exerciseId]
            val seconds = row.durationSeconds ?: catalog?.defaultDurationSeconds
            durations += seconds
            setCount += row.setCount ?: catalog?.defaultSetCount ?: 0
            kcals += CalorieCalculator.calculate(catalog?.metValue, weightKg, seconds)
        }

        return CourseTotals(
            exerciseCount = rows.size,
            setCount = setCount,
            // 시간도 칼로리와 같은 판단이다. 모르는 운동을 0 으로 더하면 실제보다 짧은 합계가
            // 나가고 화면은 그것을 코스 전체 시간으로 읽는다.
            durationSeconds = CalorieCalculator.sum(durations),
            kcal = CalorieCalculator.sum(kcals),
        )
    }

    private data class CourseTotals(
        val exerciseCount: Int,
        val setCount: Int,
        val durationSeconds: Int?,
        val kcal: Int?,
    )

    /** 미리보기 카드가 필요한 만큼의 코스 구성. 회원 코스와 템플릿이 같은 형태로 들어온다. */
    private data class PreviewComposition(
        val name: String,
        val recommendationReason: String?,
        val totalStepCount: Int,
        val exercises: List<ExerciseComposition>,
    )

    private companion object {
        /**
         * 회원의 하루 경계. 서버가 어디에 뜨든 "오늘" 은 한국 기준이다.
         *
         * MVP 사용자가 국내라 상수로 둔다. 회원별 시간대가 생기면 member 가 갖는 값이 되고
         * 그때 이 상수가 사라진다.
         */
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

        /**
         * catalog 에 없는 자세·운동이 코스에 남아 있을 수 있다. 도메인 간 FK 가 없어
         * course seed 가 앞서갈 수 있기 때문이다 (docs/domains.md §6).
         *
         * 그때 조회를 실패시키지 않는다. 코스 전체가 안 보이는 것보다 한 줄이 비는 편이 낫다.
         */
        const val UNKNOWN_NAME = ""

        fun TargetPoseCatalogEntry?.displayName(): String = this?.name ?: UNKNOWN_NAME
    }
}
