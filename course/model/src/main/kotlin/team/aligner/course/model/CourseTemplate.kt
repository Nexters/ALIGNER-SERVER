package team.aligner.course.model

/**
 * 코스 마스터. 목표 자세 하나에 템플릿 하나다 (docs/domains.md §4-4).
 *
 * 전부 seed 다. 쓰기 경로가 없어 애그리거트로 감싸지 않고 읽기 모델처럼 다룬다 —
 * catalog 가 Command 없이 조회만 갖는 것과 같은 성질이다.
 */
data class CourseTemplate(
    val templateId: Long,
    val targetPoseId: Long,
    val name: String,
    val recommendationReason: String?,
    val steps: List<CourseTemplateStep>,
)

data class CourseTemplateStep(
    val stepOrder: Int,
    val exercises: List<CourseTemplateStepExercise>,
)

data class CourseTemplateStepExercise(
    val exerciseId: Long,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
)
