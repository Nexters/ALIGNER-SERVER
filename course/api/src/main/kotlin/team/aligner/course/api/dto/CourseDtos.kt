package team.aligner.course.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.course.model.view.CourseDetailView
import team.aligner.course.model.view.CourseStepExerciseView
import team.aligner.course.model.view.CourseStepView
import team.aligner.course.model.view.TargetPoseProgressView
import team.aligner.course.model.view.TodayCourseView
import java.time.Instant

/**
 * 처방 요청.
 *
 * **자세 식별자를 받지 않는다.** 부위와 난이도만 받고 자세는 서버가 catalog 에서 찾는다 —
 * 클라이언트가 자세를 지정하면 고르지 않은 난이도의 코스를 받아갈 수 있다.
 *
 * **원인도 받지 않는다.** 서버가 최신 진단으로 검증한다 (docs/domains.md §2).
 */
@Schema(description = "코스 처방 요청")
data class PrescribeCourseRequest(
    @field:Schema(
        description = "강화할 부위 코드. `GET /screening/body-parts` 의 값이다",
        example = "BACK",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bodyPartCode: String,
    @field:Schema(
        description = "난이도. 1(하)·2(중)·3(상)이며 **목표 자세의 레벨과 같은 값**이다",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val level: Int,
)

@Schema(description = "처방된 코스 식별자")
data class PrescribeCourseResponse(
    @field:Schema(description = "코스 식별자", example = "20")
    val courseId: Long,
)

/**
 * 홈 카드.
 *
 * `estimatedKcal` 이 **null 일 수 있다.** 회원이 몸무게를 아직 입력하지 않았거나 운동의 MET 이
 * 비어 있으면 계산이 성립하지 않는다. **0 이 아니다** — 0 kcal 은 "운동량 없음" 이라 "모름" 과
 * 다르고 화면이 둘을 구분해야 한다.
 */
@Schema(description = "오늘의 코스. 진행 중인 코스다")
data class TodayCourseResponse(
    @field:Schema(description = "코스 식별자", example = "20")
    val courseId: Long,
    @field:Schema(description = "목표 자세 식별자", example = "3")
    val targetPoseId: Long,
    @field:Schema(description = "목표 자세 이름", example = "낙타 자세")
    val targetPoseName: String,
    @field:Schema(description = "목표 자세 이미지 asset 키. URL 이 아니다", nullable = true)
    val targetPoseImageAssetKey: String?,
    @field:Schema(description = "목표 자세 레벨. 회원이 고른 난이도와 같다", example = "1")
    val targetPoseLevel: Int,
    @field:Schema(description = "코스 이름", example = "낙타자세 정복하기")
    val name: String,
    @field:Schema(description = "코스 추천 이유. 감수 문구다", nullable = true)
    val recommendationReason: String?,
    @field:Schema(description = "다음에 수행할 스텝 순서. 다 했으면 null 이다", example = "2", nullable = true)
    val currentStepOrder: Int?,
    @field:Schema(description = "완료한 스텝 수", example = "1")
    val completedStepCount: Int,
    @field:Schema(description = "전체 스텝 수", example = "6")
    val totalStepCount: Int,
    @field:Schema(description = "운동 개수", example = "6")
    val exerciseCount: Int,
    @field:Schema(description = "세트 합계", example = "6")
    val totalSetCount: Int,
    @field:Schema(description = "예상 수행 시간(초)", example = "900")
    val estimatedDurationSeconds: Int,
    @field:Schema(description = "예상 칼로리. 몸무게나 MET 이 없으면 null 이고 0 이 아니다", example = "69", nullable = true)
    val estimatedKcal: Int?,
) {
    companion object {
        fun from(view: TodayCourseView) =
            TodayCourseResponse(
                courseId = view.courseId,
                targetPoseId = view.targetPoseId,
                targetPoseName = view.targetPoseName,
                targetPoseImageAssetKey = view.targetPoseImageAssetKey,
                targetPoseLevel = view.targetPoseLevel,
                name = view.name,
                recommendationReason = view.recommendationReason,
                currentStepOrder = view.currentStepOrder,
                completedStepCount = view.completedStepCount,
                totalStepCount = view.totalStepCount,
                exerciseCount = view.exerciseCount,
                totalSetCount = view.totalSetCount,
                estimatedDurationSeconds = view.estimatedDurationSeconds,
                estimatedKcal = view.estimatedKcal,
            )
    }
}

@Schema(description = "코스 개요. 스텝과 운동을 함께 싣는다")
data class CourseDetailResponse(
    @field:Schema(description = "코스 식별자", example = "20")
    val courseId: Long,
    @field:Schema(description = "목표 자세 식별자", example = "3")
    val targetPoseId: Long,
    @field:Schema(description = "목표 자세 이름", example = "낙타 자세")
    val targetPoseName: String,
    @field:Schema(description = "코스 이름", example = "낙타자세 정복하기")
    val name: String,
    @field:Schema(description = "코스 추천 이유", nullable = true)
    val recommendationReason: String?,
    @field:Schema(description = "완료한 스텝 수", example = "1")
    val completedStepCount: Int,
    @field:Schema(description = "전체 스텝 수", example = "6")
    val totalStepCount: Int,
    @field:Schema(description = "운동 개수", example = "6")
    val exerciseCount: Int,
    @field:Schema(description = "세트 합계", example = "6")
    val totalSetCount: Int,
    @field:Schema(description = "예상 수행 시간(초)", example = "900")
    val estimatedDurationSeconds: Int,
    @field:Schema(description = "예상 칼로리. 계산할 수 없으면 null 이다", example = "69", nullable = true)
    val estimatedKcal: Int?,
    @field:Schema(description = "스텝. stepOrder 오름차순이다")
    val steps: List<CourseStepResponse>,
) {
    companion object {
        fun from(view: CourseDetailView) =
            CourseDetailResponse(
                courseId = view.courseId,
                targetPoseId = view.targetPoseId,
                targetPoseName = view.targetPoseName,
                name = view.name,
                recommendationReason = view.recommendationReason,
                completedStepCount = view.completedStepCount,
                totalStepCount = view.totalStepCount,
                exerciseCount = view.exerciseCount,
                totalSetCount = view.totalSetCount,
                estimatedDurationSeconds = view.estimatedDurationSeconds,
                estimatedKcal = view.estimatedKcal,
                steps = view.steps.map(CourseStepResponse::from),
            )
    }
}

@Schema(description = "코스 스텝 하나")
data class CourseStepResponse(
    @field:Schema(description = "스텝 식별자", example = "31")
    val courseStepId: Long,
    @field:Schema(description = "스텝 순서. 1 부터다", example = "1")
    val stepOrder: Int,
    @field:Schema(description = "완료 여부", example = "false")
    val completed: Boolean,
    @field:Schema(description = "완료 시각. 아직이면 null 이다", nullable = true)
    val completedAt: Instant?,
    @field:Schema(description = "이 스텝의 운동. displayOrder 오름차순이다")
    val exercises: List<CourseStepExerciseResponse>,
) {
    companion object {
        fun from(view: CourseStepView) =
            CourseStepResponse(
                courseStepId = view.courseStepId,
                stepOrder = view.stepOrder,
                completed = view.completed,
                completedAt = view.completedAt,
                exercises = view.exercises.map(CourseStepExerciseResponse::from),
            )
    }
}

/**
 * `durationSeconds` `setCount` 는 **해석이 끝난 값**이다. 코스에 override 가 있으면 그 값이고
 * 없으면 catalog 기본값이다 — 화면이 두 곳을 보고 고르지 않게 서버가 정리해서 내린다.
 */
@Schema(description = "코스 스텝의 운동 하나")
data class CourseStepExerciseResponse(
    @field:Schema(description = "스텝 운동 식별자", example = "51")
    val courseStepExerciseId: Long,
    @field:Schema(description = "운동 식별자. 운동 가이드 조회에 그대로 쓴다", example = "7")
    val exerciseId: Long,
    @field:Schema(description = "운동 이름", example = "캣카우")
    val name: String,
    @field:Schema(description = "분류. 값 집합이 아직 고정되지 않았다", example = "가동성 웜업", nullable = true)
    val category: String?,
    @field:Schema(description = "표시 순서", example = "1")
    val displayOrder: Int,
    @field:Schema(description = "수행 시간(초). catalog 기본값까지 반영된 값이다", example = "120", nullable = true)
    val durationSeconds: Int?,
    @field:Schema(description = "세트 수. catalog 기본값까지 반영된 값이다", example = "1", nullable = true)
    val setCount: Int?,
    @field:Schema(description = "예상 칼로리. 계산할 수 없으면 null 이다", example = "6", nullable = true)
    val estimatedKcal: Int?,
) {
    companion object {
        fun from(view: CourseStepExerciseView) =
            CourseStepExerciseResponse(
                courseStepExerciseId = view.courseStepExerciseId,
                exerciseId = view.exerciseId,
                name = view.name,
                category = view.category,
                displayOrder = view.displayOrder,
                durationSeconds = view.durationSeconds,
                setCount = view.setCount,
                estimatedKcal = view.estimatedKcal,
            )
    }
}

/**
 * 자세 도전 현황 한 줄. "낙타자세 3 / 4 · 도전 중" 이다.
 *
 * **`3 / 4` 는 코스 안에서 완료한 스텝 개수**다. 자세 포인트 체크가 아니다
 * (docs/domains.md §7-8).
 */
@Schema(description = "자세 도전 현황 한 줄")
data class TargetPoseProgressResponse(
    @field:Schema(description = "이 자세의 코스 식별자", example = "20")
    val courseId: Long,
    @field:Schema(description = "목표 자세 식별자", example = "3")
    val targetPoseId: Long,
    @field:Schema(description = "목표 자세 이름", example = "낙타자세")
    val targetPoseName: String,
    @field:Schema(description = "목표 자세 이미지 asset 키", nullable = true)
    val targetPoseImageAssetKey: String?,
    @field:Schema(description = "완료한 스텝 수", example = "3")
    val completedStepCount: Int,
    @field:Schema(description = "전체 스텝 수", example = "4")
    val totalStepCount: Int,
    @field:Schema(description = "완성 여부. false 면 도전 중이다", example = "false")
    val completed: Boolean,
) {
    companion object {
        fun from(view: TargetPoseProgressView) =
            TargetPoseProgressResponse(
                courseId = view.courseId,
                targetPoseId = view.targetPoseId,
                targetPoseName = view.targetPoseName,
                targetPoseImageAssetKey = view.targetPoseImageAssetKey,
                completedStepCount = view.completedStepCount,
                totalStepCount = view.totalStepCount,
                completed = view.completed,
            )
    }
}
