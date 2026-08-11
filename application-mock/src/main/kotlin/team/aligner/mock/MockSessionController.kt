package team.aligner.mock

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.aligner.training.api.dto.CompleteSessionRequest
import team.aligner.training.api.dto.CourseProgressResponse
import team.aligner.training.api.dto.SessionExerciseRecordResponse
import team.aligner.training.api.dto.SessionResponse
import team.aligner.training.api.dto.StartSessionRequest
import team.aligner.training.model.SessionStatus

/**
 * 세션. 시작·조회는 진행 중, 완료는 완료 상태를 돌려준다.
 *
 * **상태를 저장하지 않는 대신 요청에서 역산한다.** 완료 요청의 `courseStepExerciseId` 는
 * `MockFixtures.COURSE_STEP_EXERCISE_ID_BASE + stepOrder` 규칙을 따르므로, 그것을 되돌리면
 * 어느 스텝이었는지 알 수 있다. 요청을 무시하고 고정값을 내리면 화면 흐름이 끊긴다.
 *
 * **마지막 스텝을 완료하면 `stampAcquired = true` 다.** 프론트가 완성 축하 화면을 만들 수 있다.
 *
 * 다만 **완료가 코스 진행도를 실제로 올리지는 않는다.** 다음 `GET /courses/today` 에는
 * 반영되지 않는다. 목의 한계다.
 */
@RestController
@RequestMapping("/sessions")
internal class MockSessionController {
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun start(
        @RequestBody request: StartSessionRequest,
    ): SessionResponse = session(request.courseId, request.stepOrder, completed = false)

    /**
     * 세션 식별자만으로는 어느 스텝이었는지 알 수 없다. 저장하지 않기 때문이다.
     * 진행 중인 코스의 다음 스텝을 돌려준다 — `GET /courses/today` 의 `currentStepOrder` 와 같다.
     */
    @GetMapping("/{sessionId}")
    fun getSession(
        @PathVariable sessionId: Long,
    ): SessionResponse = session(MockFixtures.IN_PROGRESS_COURSE_ID, stepOrder = currentStepOrder(), completed = false)

    /**
     * 요청에 실린 `courseStepExerciseId` 로 스텝을 역산한다. 마지막 스텝이면 도장이 붙는다.
     */
    @PostMapping("/{sessionId}/complete")
    fun complete(
        @PathVariable sessionId: Long,
        @RequestBody request: CompleteSessionRequest,
    ): SessionResponse {
        val stepOrder =
            request.exerciseRecords
                .minOfOrNull { it.courseStepExerciseId - MockFixtures.COURSE_STEP_EXERCISE_ID_BASE }
                ?.toInt()
                ?.takeIf { it in 1..MockFixtures.TOTAL_STEP_COUNT }
                ?: currentStepOrder()

        return session(MockFixtures.IN_PROGRESS_COURSE_ID, stepOrder = stepOrder, completed = true)
    }

    private fun currentStepOrder(): Int = MockFixtures.COURSES.getValue(MockFixtures.IN_PROGRESS_COURSE_ID).completedSteps + 1

    private fun session(
        courseId: Long,
        stepOrder: Int,
        completed: Boolean,
    ): SessionResponse {
        val exercise = MockFixtures.EXERCISES[(stepOrder - 1).coerceIn(MockFixtures.EXERCISES.indices)]
        return SessionResponse(
            sessionId = MockFixtures.SESSION_ID,
            courseId = courseId,
            stepOrder = stepOrder,
            status = if (completed) SessionStatus.COMPLETED else SessionStatus.IN_PROGRESS,
            startedAt = MockFixtures.NOW,
            completedAt = if (completed) MockFixtures.NOW.plusSeconds(900) else null,
            exerciseRecords =
                listOf(
                    SessionExerciseRecordResponse(
                        courseStepExerciseId = MockFixtures.COURSE_STEP_EXERCISE_ID_BASE + stepOrder,
                        exerciseId = exercise.id,
                        name = exercise.name,
                        category = exercise.category,
                        displayOrder = 1,
                        durationSeconds = exercise.durationSeconds,
                        setCount = exercise.setCount,
                        completed = completed,
                        performedDurationSeconds = if (completed) exercise.durationSeconds else null,
                    ),
                ),
            courseProgress =
                if (!completed) {
                    null
                } else {
                    CourseProgressResponse(
                        completedStepCount = stepOrder,
                        totalStepCount = MockFixtures.TOTAL_STEP_COUNT,
                        courseCompleted = stepOrder >= MockFixtures.TOTAL_STEP_COUNT,
                        stampAcquired = stepOrder >= MockFixtures.TOTAL_STEP_COUNT,
                    )
                },
        )
    }
}
