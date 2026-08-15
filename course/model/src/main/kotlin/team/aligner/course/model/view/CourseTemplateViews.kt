package team.aligner.course.model.view

/**
 * 운영 목록의 코스 템플릿 하나.
 *
 * **회원별 `Course` 가 아니라 마스터 `CourseTemplate` 이다.** 회원이 시작했는지와 무관하게
 * 서비스가 제공하는 코스 정본이고, 값은 전부 seed 다 (docs/domains.md §4-4).
 *
 * 자세 이름과 운동 이름은 catalog 의 값이라 조회 시점에 port 로 붙인다. 템플릿에는 식별자만
 * 있다 — 도메인 간 FK 가 없다 (docs/domains.md §6).
 *
 * `stepCount` `exerciseCount` 를 함께 내린다. 감수자가 목록에서 "이 코스가 몇 스텝짜리인가" 를
 * 먼저 보므로 화면이 steps 를 세게 하지 않는다.
 */
data class CourseTemplateView(
    val templateId: Long,
    val targetPoseId: Long,
    /** catalog 에서 자세를 찾지 못하면 빈 문자열이다. 목록 전체가 안 보이는 것보다 낫다. */
    val targetPoseName: String,
    val name: String,
    val recommendationReason: String?,
    val stepCount: Int,
    val exerciseCount: Int,
    val steps: List<CourseTemplateStepView>,
)

data class CourseTemplateStepView(
    val stepOrder: Int,
    val exercises: List<CourseTemplateStepExerciseView>,
)

/**
 * 템플릿 스텝 행 하나.
 *
 * `durationSeconds` `setCount` 는 템플릿의 override 가 있으면 그 값이고, 없으면 catalog 의
 * 기본값이다. **해석을 조회 시점에 끝내서 내린다** — `CourseStepExerciseView` 와 같은 판단이다.
 *
 * `estimatedKcal` 이 없다. 칼로리는 회원 몸무게의 함수인데 템플릿에는 회원이 없다.
 */
data class CourseTemplateStepExerciseView(
    val exerciseId: Long,
    val name: String,
    val imageAssetKey: String?,
    val category: String?,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
)
