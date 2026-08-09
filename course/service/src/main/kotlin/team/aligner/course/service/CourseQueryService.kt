package team.aligner.course.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseSkeleton
import team.aligner.course.infrastructure.ExerciseCatalogEntry
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.TargetPoseCatalogEntry
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.model.exception.CourseNotFoundException
import team.aligner.course.model.exception.InProgressCourseNotFoundException
import team.aligner.course.model.view.CourseDetailView
import team.aligner.course.model.view.CourseStepExerciseView
import team.aligner.course.model.view.CourseStepView
import team.aligner.course.model.view.TargetPoseProgressView
import team.aligner.course.model.view.TodayCourseView

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
    ): List<TargetPoseProgressView>
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
    private val targetPoseCatalogPort: TargetPoseCatalogPort,
    private val exerciseCatalogPort: ExerciseCatalogPort,
    private val memberBodyPort: MemberBodyPort,
) : CourseQueryService {
    /**
     * 홈 카드. **"오늘의 코스" 는 진행 중인 코스의 다른 이름**이다 — 일자 개념이 없다
     * (docs/domains.md §4-4).
     */
    override fun getTodayCourse(memberId: Long): TodayCourseView {
        val skeleton =
            courseQueryRepository.findInProgressCourseSkeleton(memberId)
                ?: throw InProgressCourseNotFoundException()

        val pose = targetPoseCatalogPort.findAllByIds(listOf(skeleton.targetPoseId)).firstOrNull()
        val exercises = loadExercises(skeleton)
        val weightKg = memberBodyPort.findWeightKg(memberId)
        val totals = totalsOf(skeleton, exercises, weightKg)

        return TodayCourseView(
            courseId = skeleton.courseId,
            targetPoseId = skeleton.targetPoseId,
            targetPoseName = pose.displayName(),
            targetPoseImageAssetKey = pose?.imageAssetKey,
            targetPoseLevel = pose?.level ?: 0,
            name = skeleton.templateName,
            recommendationReason = skeleton.recommendationReason,
            currentStepOrder = skeleton.currentStepOrder,
            completedStepCount = skeleton.completedStepCount,
            totalStepCount = skeleton.totalStepCount,
            exerciseCount = totals.exerciseCount,
            totalSetCount = totals.setCount,
            estimatedDurationSeconds = totals.durationSeconds,
            estimatedKcal = totals.kcal,
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
        val exercises = loadExercises(skeleton)
        val weightKg = memberBodyPort.findWeightKg(memberId)
        val totals = totalsOf(skeleton, exercises, weightKg)

        return CourseDetailView(
            courseId = skeleton.courseId,
            targetPoseId = skeleton.targetPoseId,
            targetPoseName = pose.displayName(),
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
     * 자세 도전 현황. 프로필의 "완수한 자세 목록" 도 `completedOnly` 로 같은 조회를 쓴다 —
     * 별도 API 를 만들지 않는다.
     */
    override fun getTargetPoseProgress(
        memberId: Long,
        completedOnly: Boolean?,
    ): List<TargetPoseProgressView> {
        val skeletons = courseQueryRepository.findAllCourseSkeletons(memberId)
        val filtered =
            when (completedOnly) {
                null -> skeletons
                else -> skeletons.filter { it.completed == completedOnly }
            }
        if (filtered.isEmpty()) {
            return emptyList()
        }

        // 자세 수만큼 catalog 를 치지 않는다 (docs/domains.md §4-3-1).
        val poses =
            targetPoseCatalogPort
                .findAllByIds(filtered.map { it.targetPoseId })
                .associateBy { it.targetPoseId }

        return filtered.map { skeleton ->
            val pose = poses[skeleton.targetPoseId]
            TargetPoseProgressView(
                courseId = skeleton.courseId,
                targetPoseId = skeleton.targetPoseId,
                targetPoseName = pose.displayName(),
                targetPoseImageAssetKey = pose?.imageAssetKey,
                completedStepCount = skeleton.completedStepCount,
                totalStepCount = skeleton.totalStepCount,
                completed = skeleton.completed,
            )
        }
    }

    /**
     * 코스에 실린 운동을 한 번에 읽는다. 스텝마다 부르면 조회가 스텝 수만큼 늘어난다.
     */
    private fun loadExercises(skeleton: CourseSkeleton): Map<Long, ExerciseCatalogEntry> {
        val ids = skeleton.steps.flatMap { step -> step.exercises.map { it.exerciseId } }.distinct()
        if (ids.isEmpty()) {
            return emptyMap()
        }
        return exerciseCatalogPort.findAllByIds(ids).associateBy { it.exerciseId }
    }

    /**
     * 코스 카드의 합계. **칼로리는 스텝 합**이다 (docs/domains.md §4-3).
     */
    private fun totalsOf(
        skeleton: CourseSkeleton,
        exercises: Map<Long, ExerciseCatalogEntry>,
        weightKg: Int?,
    ): CourseTotals {
        val rows = skeleton.steps.flatMap { it.exercises }
        var duration = 0
        val kcals = mutableListOf<Int?>()
        var setCount = 0

        rows.forEach { row ->
            val catalog = exercises[row.exerciseId]
            val seconds = row.durationSeconds ?: catalog?.defaultDurationSeconds
            duration += seconds ?: 0
            setCount += row.setCount ?: catalog?.defaultSetCount ?: 0
            kcals += CalorieCalculator.calculate(catalog?.metValue, weightKg, seconds)
        }

        return CourseTotals(
            exerciseCount = rows.size,
            setCount = setCount,
            durationSeconds = duration,
            kcal = CalorieCalculator.sum(kcals),
        )
    }

    private data class CourseTotals(
        val exerciseCount: Int,
        val setCount: Int,
        val durationSeconds: Int,
        val kcal: Int?,
    )

    private companion object {
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
