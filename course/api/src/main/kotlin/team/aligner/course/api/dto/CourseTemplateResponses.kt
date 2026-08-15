package team.aligner.course.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.course.model.view.CourseTemplateStepExerciseView
import team.aligner.course.model.view.CourseTemplateStepView
import team.aligner.course.model.view.CourseTemplateView

/**
 * 운영 목록의 코스 템플릿 하나.
 *
 * **회원별 코스가 아니라 마스터다.** `courseId` 가 없고 `templateId` 다. 진행도·도장·칼로리도
 * 없다 — 전부 회원이 있어야 성립하는 값이다.
 */
@Schema(description = "운영 목록용 코스 템플릿")
data class CourseTemplateResponse(
    @field:Schema(description = "코스 템플릿 식별자", example = "1")
    val templateId: Long,
    @field:Schema(description = "이 코스가 겨냥하는 목표 자세 식별자", example = "1")
    val targetPoseId: Long,
    @field:Schema(
        description = "목표 자세 이름. catalog 에서 찾지 못하면 빈 문자열이다 — 도메인 간 FK 가 없어 seed 가 앞서갈 수 있다",
        example = "낙타자세",
    )
    val targetPoseName: String,
    @field:Schema(description = "코스 이름", example = "낙타자세 정복하기")
    val name: String,
    @field:Schema(description = "추천 사유. 온보딩에서 한 번 보여주는 문구다", example = "등과 골반 근육 강화에 집중해 보세요", nullable = true)
    val recommendationReason: String?,
    @field:Schema(description = "스텝 수", example = "7")
    val stepCount: Int,
    @field:Schema(description = "스텝에 편성된 운동 수의 합", example = "7")
    val exerciseCount: Int,
    @field:Schema(description = "스텝 목록. 스텝 순서대로다")
    val steps: List<CourseTemplateStepResponse>,
) {
    companion object {
        fun from(view: CourseTemplateView): CourseTemplateResponse =
            CourseTemplateResponse(
                templateId = view.templateId,
                targetPoseId = view.targetPoseId,
                targetPoseName = view.targetPoseName,
                name = view.name,
                recommendationReason = view.recommendationReason,
                stepCount = view.stepCount,
                exerciseCount = view.exerciseCount,
                steps = view.steps.map(CourseTemplateStepResponse::from),
            )
    }
}

@Schema(description = "코스 템플릿 스텝")
data class CourseTemplateStepResponse(
    @field:Schema(description = "스텝 순서. 1 부터다", example = "1")
    val stepOrder: Int,
    @field:Schema(description = "이 스텝의 운동. 아직 편성되지 않았으면 빈 배열이다")
    val exercises: List<CourseTemplateStepExerciseResponse>,
) {
    companion object {
        fun from(view: CourseTemplateStepView): CourseTemplateStepResponse =
            CourseTemplateStepResponse(
                stepOrder = view.stepOrder,
                exercises = view.exercises.map(CourseTemplateStepExerciseResponse::from),
            )
    }
}

/**
 * 템플릿 스텝의 운동 한 줄.
 *
 * `durationSeconds` `setCount` 는 템플릿 override 가 있으면 그 값, 없으면 catalog 기본값이다.
 * **서버가 해석을 끝내서 내린다** — 화면이 두 곳을 보고 고르지 않는다.
 */
@Schema(description = "코스 템플릿 스텝의 운동")
data class CourseTemplateStepExerciseResponse(
    @field:Schema(description = "운동 식별자", example = "101")
    val exerciseId: Long,
    @field:Schema(description = "운동 이름. catalog 에서 찾지 못하면 빈 문자열이다", example = "턱 당기기")
    val name: String,
    @field:Schema(description = "대표 이미지 asset 키. URL 이 아니다", example = "exercise/cat-cow", nullable = true)
    val imageAssetKey: String?,
    @field:Schema(description = "운동 분류", example = "가동성 웜업", nullable = true)
    val category: String?,
    @field:Schema(description = "스텝 안에서의 표시 순서. 1 부터다", example = "1")
    val displayOrder: Int,
    @field:Schema(description = "수행 시간(초). 횟수로 수행하는 운동이면 null 이다", example = "120", nullable = true)
    val durationSeconds: Int?,
    @field:Schema(description = "세트 수. 시간으로 수행하는 운동이면 null 이다", example = "3", nullable = true)
    val setCount: Int?,
) {
    companion object {
        fun from(view: CourseTemplateStepExerciseView): CourseTemplateStepExerciseResponse =
            CourseTemplateStepExerciseResponse(
                exerciseId = view.exerciseId,
                name = view.name,
                imageAssetKey = view.imageAssetKey,
                category = view.category,
                displayOrder = view.displayOrder,
                durationSeconds = view.durationSeconds,
                setCount = view.setCount,
            )
    }
}
