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
 *
 * **진행도는 MockFixtures.COURSES 한 곳에서만 가져온다.** 도전 현황과 코스 개요가 각자 값을
 * 들고 있으면 같은 코스의 진행도가 화면마다 달라진다.
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
        val course = MockFixtures.COURSES.getValue(MockFixtures.IN_PROGRESS_COURSE_ID)
        val pose = course.targetPose
        return TodayCourseResponse(
            courseId = course.courseId,
            targetPoseId = pose.id,
            targetPoseName = pose.name,
            targetPoseImageAssetKey = pose.assetKey,
            targetPoseLevel = pose.level,
            name = "낙타자세 정복하기",
            recommendationReason = "등과 골반 근육 강화에 집중해 보세요",
            currentStepOrder = course.completedSteps + 1,
            completedStepCount = course.completedSteps,
            totalStepCount = MockFixtures.TOTAL_STEP_COUNT,
            exerciseCount = MockFixtures.TOTAL_STEP_COUNT,
            totalSetCount = MockFixtures.TOTAL_SET_COUNT,
            estimatedDurationSeconds = MockFixtures.TOTAL_DURATION_SECONDS,
            estimatedKcal = MockFixtures.TOTAL_KCAL,
        )
    }

    /**
     * 도전 현황도 코스 개요와 **같은 픽스처에서 진행도를 가져온다.** 각자 값을 들고 있으면
     * 현황에서 상세로 넘어갈 때 같은 코스의 진행도가 달라진다.
     */
    @GetMapping("/progress/target-poses")
    fun getTargetPoseProgress(
        @RequestParam(required = false) completed: Boolean?,
    ): List<TargetPoseProgressResponse> {
        val all = MockFixtures.COURSES.values.map(::progress)
        return if (completed == null) all else all.filter { it.completed == completed }
    }

    /**
     * **픽스처에 없는 식별자는 404 다.** 실제 서버와 같은 오류 계약을 내야 프론트가 오류
     * 경로까지 목으로 만들 수 있다.
     */
    @GetMapping("/{courseId}")
    fun getCourseDetail(
        @PathVariable courseId: Long,
    ): CourseDetailResponse {
        val course =
            MockFixtures.COURSES[courseId]
                ?: throw MockNotFoundException("COURSE_NOT_FOUND", "코스를 찾을 수 없습니다")
        val completedSteps = course.completedSteps
        val pose = course.targetPose

        return CourseDetailResponse(
            courseId = course.courseId,
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
                                    courseStepExerciseId = MockFixtures.COURSE_STEP_EXERCISE_ID_BASE + order,
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

    private fun progress(course: MockFixtures.CourseState) =
        TargetPoseProgressResponse(
            courseId = course.courseId,
            targetPoseId = course.targetPose.id,
            targetPoseName = course.targetPose.name,
            targetPoseImageAssetKey = course.targetPose.assetKey,
            completedStepCount = course.completedSteps,
            totalStepCount = MockFixtures.TOTAL_STEP_COUNT,
            completed = course.completed,
        )
}
