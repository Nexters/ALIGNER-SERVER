package team.aligner.training.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.training.model.PerceivedResult
import team.aligner.training.model.SessionStatus
import team.aligner.training.model.view.CourseProgressView
import team.aligner.training.model.view.SessionExerciseRecordView
import team.aligner.training.model.view.SessionView
import team.aligner.training.service.CompleteSessionCommand
import team.aligner.training.service.ExerciseResultCommand
import team.aligner.training.service.StartSessionCommand
import java.time.Instant

/**
 * 세션 시작 요청.
 *
 * 코스 식별자를 본문으로 받는다. 경로를 `/courses/{courseId}/sessions` 로 두지 않는 것은
 * 이 저장소가 경로 앞부분으로 도메인을 가르고 있어서다 — `/sessions` 아래는 training 소유다.
 *
 * (주석에 `/` 와 `*` 를 붙여 쓰지 않는다. Kotlin 은 블록 주석 중첩을 지원해서 KDoc 안의
 * 그 조합이 주석을 하나 더 연다.)
 */
@Schema(description = "세션 시작 요청")
data class StartSessionRequest(
    @field:Schema(description = "수행할 코스 식별자", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    val courseId: Long,
    @field:Schema(description = "수행할 스텝 순서. 1 부터다", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val stepOrder: Int,
) {
    fun toCommand(): StartSessionCommand = StartSessionCommand(courseId = courseId, stepOrder = stepOrder)
}

/**
 * 세션 완료 요청.
 *
 * **요청에 없는 운동은 수행하지 않은 것으로 남는다.** 부분 완료가 정상이라 빠진 것을 오류로
 * 보지 않는다. 반대로 이 세션에 없는 `courseStepExerciseId` 가 섞이면 400 이다 — 조용히
 * 무시하면 클라이언트가 잘못 보낸 것을 성공으로 읽는다.
 */
@Schema(description = "세션 완료 요청")
data class CompleteSessionRequest(
    @field:Schema(description = "운동별 수행 결과", requiredMode = Schema.RequiredMode.REQUIRED)
    val exerciseRecords: List<ExerciseResultRequest>,
) {
    fun toCommand(): CompleteSessionCommand =
        CompleteSessionCommand(
            exerciseRecords =
                exerciseRecords.map {
                    ExerciseResultCommand(
                        courseStepExerciseId = it.courseStepExerciseId,
                        completed = it.completed,
                        performedDurationSeconds = it.performedDurationSeconds,
                    )
                },
        )
}

@Schema(description = "운동 하나의 수행 결과")
data class ExerciseResultRequest(
    @field:Schema(description = "세션 응답의 courseStepExerciseId 를 그대로 쓴다", example = "51")
    val courseStepExerciseId: Long,
    @field:Schema(description = "수행 완료 여부", example = "true")
    val completed: Boolean,
    @field:Schema(description = "실제 수행 시간(초). 수행하지 않았으면 null 이다", example = "120", nullable = true)
    val performedDurationSeconds: Int?,
)

/**
 * 세션 응답. **시작·조회·완료가 모두 같은 형태**다.
 *
 * 화면이 세 경로에서 같은 것을 그리므로 형태가 갈리면 복구 흐름에서만 드러나는 차이가 생긴다.
 */
@Schema(description = "세션 상태")
data class SessionResponse(
    @field:Schema(description = "세션 식별자", example = "100")
    val sessionId: Long,
    @field:Schema(description = "코스 식별자", example = "20")
    val courseId: Long,
    @field:Schema(description = "수행 중인 스텝 순서", example = "1")
    val stepOrder: Int,
    @field:Schema(description = "세션 상태", allowableValues = ["IN_PROGRESS", "COMPLETED"])
    val status: SessionStatus,
    @field:Schema(description = "시작 시각")
    val startedAt: Instant,
    @field:Schema(description = "완료 시각. 진행 중이면 null 이다", nullable = true)
    val completedAt: Instant?,
    @field:Schema(description = "완료한 운동 개수. 리포트의 \"완료 동작 N 개\" 다", example = "8")
    val completedExerciseCount: Int,
    @field:Schema(
        description =
            "이 세션의 소모 칼로리. 완료 시점에 계산해 **저장한 값**이라 나중에 몸무게가 바뀌어도 " +
                "지난 리포트가 흔들리지 않는다. 몸무게·MET·수행 시간 중 하나라도 모르면 0 이 아니라 null 이다",
        example = "63",
        nullable = true,
    )
    val estimatedKcal: Int?,
    @field:Schema(
        description = "핀포즈 직후 체감. 아직 답하지 않았으면 null 이다",
        allowableValues = ["SUCCEEDED", "STILL_HARD", "TOO_HARD"],
        nullable = true,
    )
    val perceivedResult: PerceivedResult?,
    @field:Schema(description = "운동별 수행 기록. displayOrder 오름차순이다")
    val exerciseRecords: List<SessionExerciseRecordResponse>,
    @field:Schema(
        description =
            "이 세션 완료가 코스 진행도에 반영된 결과. " +
                "완료 요청 응답과 완료된 세션 조회에서 동일한 스냅샷 값이 반환된다. 진행 중인 세션에서는 null 이다.",
        nullable = true,
    )
    val courseProgress: CourseProgressResponse?,
) {
    companion object {
        fun from(view: SessionView) =
            SessionResponse(
                sessionId = view.sessionId,
                courseId = view.courseId,
                stepOrder = view.stepOrder,
                status = view.status,
                startedAt = view.startedAt,
                completedAt = view.completedAt,
                completedExerciseCount = view.completedExerciseCount,
                estimatedKcal = view.estimatedKcal,
                perceivedResult = view.perceivedResult,
                exerciseRecords = view.exerciseRecords.map(SessionExerciseRecordResponse::from),
                courseProgress = view.courseProgress?.let(CourseProgressResponse::from),
            )
    }
}

/**
 * `durationSeconds` `setCount` 는 **해석이 끝난 값**이다. 코스 override 가 있으면 그 값이고
 * 없으면 catalog 기본값이다.
 */
@Schema(description = "세션 안의 운동 하나")
data class SessionExerciseRecordResponse(
    @field:Schema(description = "완료 요청에 그대로 넣을 식별자", example = "51")
    val courseStepExerciseId: Long,
    @field:Schema(description = "운동 식별자. 운동 가이드 조회에 쓴다", example = "7")
    val exerciseId: Long,
    @field:Schema(description = "운동 이름", example = "캣카우")
    val name: String,
    @field:Schema(description = "분류", example = "가동성 웜업", nullable = true)
    val category: String?,
    @field:Schema(description = "표시 순서", example = "1")
    val displayOrder: Int,
    @field:Schema(description = "수행 시간(초)", example = "120", nullable = true)
    val durationSeconds: Int?,
    @field:Schema(description = "세트 수", example = "1", nullable = true)
    val setCount: Int?,
    @field:Schema(description = "수행 완료 여부. 시작 직후에는 전부 false 다", example = "false")
    val completed: Boolean,
    @field:Schema(description = "실제 수행 시간(초)", nullable = true)
    val performedDurationSeconds: Int?,
) {
    companion object {
        fun from(view: SessionExerciseRecordView) =
            SessionExerciseRecordResponse(
                courseStepExerciseId = view.courseStepExerciseId,
                exerciseId = view.exerciseId,
                name = view.name,
                category = view.category,
                displayOrder = view.displayOrder,
                durationSeconds = view.durationSeconds,
                setCount = view.setCount,
                completed = view.completed,
                performedDurationSeconds = view.performedDurationSeconds,
            )
    }
}

/**
 * **서버가 course 에서 받아온 값이다.** training 이 계산하지 않는다 (docs/domains.md §2).
 *
 * 완료 리포트 한 화면이 여기서 다 나온다 — 헤더의 `골반 난이도 상 · 낙타자세`,
 * `낙타자세 해냈어요! 1 / 4회` 세그먼트, 자세 완성 축하 화면의 신호까지다. 자세 정보를 얻으려고
 * `GET /courses/{courseId}` 나 `GET /catalog/target-poses/{id}` 를 다시 부르지 않아도 된다.
 */
@Schema(description = "세션 완료가 코스 진행도에 반영된 결과")
data class CourseProgressResponse(
    @field:Schema(description = "이번 회차에서 완료한 스텝 수. **파이어로그 세그먼트가 아니다**", example = "2")
    val completedStepCount: Int,
    @field:Schema(description = "이 코스의 전체 스텝 수", example = "6")
    val totalStepCount: Int,
    @field:Schema(
        description = "이번 회차의 스텝을 전부 끝냈는지. **자세 완성과 다르다** — 완성은 4 회 완주다",
        example = "false",
    )
    val courseCompleted: Boolean,
    @field:Schema(
        description =
            "이 세션의 완료로 이번 회차의 도장을 획득했는지. " +
                "완료 리포트에 저장되는 스냅샷 값이므로 재조회에서도 동일하다.",
        example = "false",
    )
    val stampAcquired: Boolean,
    @field:Schema(description = "이 코스의 목표 자세 식별자", example = "3")
    val targetPoseId: Long,
    @field:Schema(
        description =
            "목표 자세의 **운동 식별자**. `targetPoseId` 와 다른 값이다 — 같은 자세가 " +
                "`catalog.target_pose` 와 `catalog.exercise` 양쪽에 행을 갖고, 영상·음성 큐는 " +
                "운동 쪽에만 있다. 핀포즈 영상을 재생하려면 이 값으로 " +
                "`GET /catalog/exercises/{exerciseId}` 를 부른다. " +
                "연결이 없으면 null 이고, 그때 화면은 영상 없이 그린다.",
        example = "110",
        nullable = true,
    )
    val targetPoseExerciseId: Long?,
    @field:Schema(description = "목표 자세 이름. 리포트 헤더와 파이어로그 카드에 쓴다", example = "낙타자세")
    val targetPoseName: String,
    @field:Schema(description = "목표 자세의 부위. catalog 에 자세가 없으면 null 이다", nullable = true)
    val bodyPartCode: BodyPartCode?,
    @field:Schema(description = "목표 자세의 난이도 단계", example = "3", nullable = true)
    val level: Int?,
    @field:Schema(
        description =
            "이 자세를 지금까지 완주한 횟수 = 붙은 도장 수. **`1 / 4회` 의 분자다.** " +
                "코스를 한 번 완주할 때마다 하나씩 오른다",
        example = "1",
    )
    val acquiredStampCount: Int,
    @field:Schema(description = "완성에 필요한 완주 횟수. `1 / 4회` 의 분모다", example = "4")
    val requiredStampCount: Int,
    @field:Schema(
        description = "이 자세를 완성했는지. `stampAcquired` 와 함께 true 면 방금 완성한 것이라 축하 화면을 띄운다",
        example = "false",
    )
    val targetPoseCompleted: Boolean,
) {
    companion object {
        fun from(view: CourseProgressView) =
            CourseProgressResponse(
                completedStepCount = view.completedStepCount,
                totalStepCount = view.totalStepCount,
                courseCompleted = view.courseCompleted,
                stampAcquired = view.stampAcquired,
                targetPoseId = view.targetPoseId,
                targetPoseExerciseId = view.targetPoseExerciseId,
                targetPoseName = view.targetPoseName,
                bodyPartCode = view.bodyPartCode?.let(BodyPartCode::from),
                level = view.level,
                acquiredStampCount = view.acquiredStampCount,
                requiredStampCount = view.requiredStampCount,
                targetPoseCompleted = view.targetPoseCompleted,
            )
    }
}
