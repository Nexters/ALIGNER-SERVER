package team.aligner.course.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseSkeleton
import team.aligner.course.infrastructure.ExerciseCatalogEntry
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.TargetPoseCatalogEntry
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.model.Stamp
import team.aligner.course.model.exception.CourseNotFoundException
import team.aligner.course.model.exception.InProgressCourseNotFoundException
import team.aligner.course.model.view.CourseDetailView
import team.aligner.course.model.view.CourseStepExerciseView
import team.aligner.course.model.view.CourseStepView
import team.aligner.course.model.view.TargetPoseProgressSummaryView
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
    ): TargetPoseProgressSummaryView
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
