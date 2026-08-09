package team.aligner.course.model.view

import java.time.Instant

/**
 * 홈 카드 하나를 위한 읽기 모델.
 *
 * 목표 자세 이름·이미지와 운동 개수는 catalog 의 값이라 조회 시점에 port 로 붙인다.
 * 애그리거트에는 식별자만 있다 (docs/domains.md §6).
 *
 * `estimatedKcal` 이 **null 일 수 있다.** 회원이 아직 몸무게를 입력하지 않았거나 운동의
 * MET 이 비어 있으면 계산이 성립하지 않는다. **0 으로 만들지 않는다** — 0 kcal 은
 * "운동량 없음" 이라 "모름" 과 다르고 화면이 둘을 구분해야 한다.
 */
data class TodayCourseView(
    val courseId: Long,
    val targetPoseId: Long,
    val targetPoseName: String,
    val targetPoseImageAssetKey: String?,
    val targetPoseLevel: Int,
    val name: String,
    val recommendationReason: String?,
    val currentStepOrder: Int?,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val exerciseCount: Int,
    val totalSetCount: Int,
    val estimatedDurationSeconds: Int,
    val estimatedKcal: Int?,
)

/**
 * 코스 개요 화면. 스텝마다 운동을 싣는다.
 */
data class CourseDetailView(
    val courseId: Long,
    val targetPoseId: Long,
    val targetPoseName: String,
    val name: String,
    val recommendationReason: String?,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val exerciseCount: Int,
    val totalSetCount: Int,
    val estimatedDurationSeconds: Int,
    val estimatedKcal: Int?,
    val steps: List<CourseStepView>,
)

data class CourseStepView(
    val courseStepId: Long,
    val stepOrder: Int,
    val completed: Boolean,
    val completedAt: Instant?,
    val exercises: List<CourseStepExerciseView>,
)

/**
 * 코스 스텝 행 하나. 이름·분류·MET 은 catalog 가 갖는 값이라 port 로 붙인다.
 *
 * `durationSeconds` `setCount` 는 코스의 override 가 있으면 그 값이고, 없으면 catalog 의
 * 기본값이다. **해석을 조회 시점에 끝내서 내린다** — 화면이 두 곳을 보고 고르게 하지 않는다.
 */
data class CourseStepExerciseView(
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val name: String,
    val category: String?,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
    val estimatedKcal: Int?,
)

/**
 * 자세 도전 현황 한 줄. "낙타자세 3 / 4 체크 · 도전 중" 이다.
 *
 * 프로필의 "완수한 자세 목록" 도 같은 모델을 상태로 걸러 쓴다.
 */
data class TargetPoseProgressView(
    val courseId: Long,
    val targetPoseId: Long,
    val targetPoseName: String,
    val targetPoseImageAssetKey: String?,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val completed: Boolean,
)
