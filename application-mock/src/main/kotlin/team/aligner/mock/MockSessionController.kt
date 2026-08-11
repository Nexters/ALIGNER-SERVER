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
 * **완료가 코스 진행도를 실제로 올리지는 않는다.** 응답의 `courseProgress` 는 고정값이고,
 * 다음 `GET /courses/today` 에는 반영되지 않는다. 목의 한계다.
 *
 * 다만 `stampAcquired = true` 로 두어 **완성 축하 화면을 만들 수 있게** 한다.
 */
@RestController
@RequestMapping("/sessions")
internal class MockSessionController {
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun start(
        @RequestBody request: StartSessionRequest,
    ): SessionResponse = session(request.courseId, request.stepOrder, completed = false)

    @GetMapping("/{sessionId}")
    fun getSession(
        @PathVariable sessionId: Long,
    ): SessionResponse = session(MockFixtures.IN_PROGRESS_COURSE_ID, stepOrder = 2, completed = false)

    @PostMapping("/{sessionId}/complete")
    fun complete(
        @PathVariable sessionId: Long,
        @RequestBody request: CompleteSessionRequest,
    ): SessionResponse = session(MockFixtures.IN_PROGRESS_COURSE_ID, stepOrder = 2, completed = true)

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
                        courseStepExerciseId = 50L + stepOrder,
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
