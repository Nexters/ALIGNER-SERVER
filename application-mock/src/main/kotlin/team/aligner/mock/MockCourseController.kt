package team.aligner.mock

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.aligner.course.api.dto.CourseDetailResponse
import team.aligner.course.api.dto.CourseStepExerciseResponse
import team.aligner.course.api.dto.CourseStepResponse
import team.aligner.course.api.dto.PrescribeCourseRequest
import team.aligner.course.api.dto.PrescribeCourseResponse
import team.aligner.course.api.dto.TargetPoseProgressResponse
import team.aligner.course.api.dto.TodayCourseResponse

/**
 * 코스. **상태를 쌓지 않는 대신 식별자로 상태를 나눈다.**
 *
 * - 코스 20 — 진행 중 `1 / 6`
 * - 코스 21 — 완성 `6 / 6`
 *
 * 프론트가 두 화면을 모두 만들 수 있다. 세션을 완료해도 20 번 코스의 진행도는 그대로다.
 */
@RestController
@RequestMapping("/courses")
internal class MockCourseController {
    /**
     * 어떤 부위·난이도로 요청해도 진행 중인 코스를 돌려준다. 저장하지 않는다.
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun prescribe(
        @RequestBody request: PrescribeCourseRequest,
    ): PrescribeCourseResponse = PrescribeCourseResponse(courseId = MockFixtures.IN_PROGRESS_COURSE_ID)

    @GetMapping("/today")
    fun getTodayCourse(): TodayCourseResponse {
        val pose = MockFixtures.TARGET_POSES.first()
        return TodayCourseResponse(
            courseId = MockFixtures.IN_PROGRESS_COURSE_ID,
            targetPoseId = pose.id,
            targetPoseName = pose.name,
            targetPoseImageAssetKey = pose.assetKey,
            targetPoseLevel = pose.level,
            name = "낙타자세 정복하기",
            recommendationReason = "등과 골반 근육 강화에 집중해 보세요",
            currentStepOrder = MockFixtures.COMPLETED_STEP_COUNT + 1,
            completedStepCount = MockFixtures.COMPLETED_STEP_COUNT,
            totalStepCount = MockFixtures.TOTAL_STEP_COUNT,
            exerciseCount = MockFixtures.TOTAL_STEP_COUNT,
            totalSetCount = MockFixtures.TOTAL_SET_COUNT,
            estimatedDurationSeconds = MockFixtures.TOTAL_DURATION_SECONDS,
            estimatedKcal = MockFixtures.TOTAL_KCAL,
        )
    }

    @GetMapping("/progress/target-poses")
    fun getTargetPoseProgress(
        @RequestParam(required = false) completed: Boolean?,
    ): List<TargetPoseProgressResponse> {
        val all =
            listOf(
                progress(MockFixtures.TARGET_POSES[0], MockFixtures.IN_PROGRESS_COURSE_ID, 3, 4, false),
                progress(MockFixtures.TARGET_POSES[2], 22L, 2, 4, false),
                progress(MockFixtures.TARGET_POSES[3], 23L, 1, 4, false),
                progress(MockFixtures.TARGET_POSES[8], MockFixtures.COMPLETED_COURSE_ID, 4, 4, true),
            )
        return if (completed == null) all else all.filter { it.completed == completed }
    }

    @GetMapping("/{courseId}")
    fun getCourseDetail(
        @PathVariable courseId: Long,
    ): CourseDetailResponse {
        val done = courseId == MockFixtures.COMPLETED_COURSE_ID
        val completedSteps = if (done) MockFixtures.TOTAL_STEP_COUNT else MockFixtures.COMPLETED_STEP_COUNT
        val pose = MockFixtures.TARGET_POSES.first()

        return CourseDetailResponse(
            courseId = courseId,
            targetPoseId = pose.id,
            targetPoseName = pose.name,
            name = "낙타자세 정복하기",
            recommendationReason = "등과 골반 근육 강화에 집중해 보세요",
            completedStepCount = completedSteps,
            totalStepCount = MockFixtures.TOTAL_STEP_COUNT,
            exerciseCount = MockFixtures.TOTAL_STEP_COUNT,
            totalSetCount = MockFixtures.TOTAL_SET_COUNT,
            estimatedDurationSeconds = MockFixtures.TOTAL_DURATION_SECONDS,
            estimatedKcal = MockFixtures.TOTAL_KCAL,
            steps =
                MockFixtures.COURSE_STEPS.map { (order, exercise) ->
                    CourseStepResponse(
                        courseStepId = 30L + order,
                        stepOrder = order,
                        completed = order <= completedSteps,
                        completedAt = if (order <= completedSteps) MockFixtures.NOW else null,
                        exercises =
                            listOf(
                                CourseStepExerciseResponse(
                                    courseStepExerciseId = 50L + order,
                                    exerciseId = exercise.id,
                                    name = exercise.name,
                                    category = exercise.category,
                                    displayOrder = 1,
                                    durationSeconds = exercise.durationSeconds,
                                    setCount = exercise.setCount,
                                    estimatedKcal = exercise.kcal,
                                ),
                            ),
                    )
                },
        )
    }

    private fun progress(
        pose: MockFixtures.TargetPose,
        courseId: Long,
        completedSteps: Int,
        totalSteps: Int,
        done: Boolean,
    ) = TargetPoseProgressResponse(
        courseId = courseId,
        targetPoseId = pose.id,
        targetPoseName = pose.name,
        targetPoseImageAssetKey = pose.assetKey,
        completedStepCount = completedSteps,
        totalStepCount = totalSteps,
        completed = done,
    )
}
