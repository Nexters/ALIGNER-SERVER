package team.aligner.course.model.view

import java.time.Instant

/**
 * 홈 카드 하나를 위한 읽기 모델.
 *
 * 목표 자세 이름·이미지와 운동 개수는 catalog 의 값이라 조회 시점에 port 로 붙인다.
 * 애그리거트에는 식별자만 있다 (docs/domains.md §6).
 *
 * **모르는 값에 0 을 넣지 않는다.** `estimatedKcal` `estimatedDurationSeconds`
 * `targetPoseLevel` 은 계산·조회가 성립하지 않으면 null 이다. 0 kcal 은 "운동량 없음",
 * 0 초는 "순식간", 레벨 0 은 "0 단계 코스" 로 읽히므로 "모름" 과 구분돼야 한다.
 *
 * catalog 에서 자세나 운동을 찾지 못하는 경우가 실제로 있다 — 도메인 간 FK 가 없어
 * course seed 가 catalog 보다 앞서갈 수 있다 (docs/domains.md §6).
 */
data class TodayCourseView(
    val courseId: Long,
    val targetPoseId: Long,
    val targetPoseName: String,
    val targetPoseImageAssetKey: String?,
    val targetPoseLevel: Int?,
    val name: String,
    val recommendationReason: String?,
    val currentStepOrder: Int?,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val exerciseCount: Int,
    val totalSetCount: Int,
    val estimatedDurationSeconds: Int?,
    val estimatedKcal: Int?,
)

/**
 * 코스 개요 화면. 스텝마다 운동을 싣는다.
 */
data class CourseDetailView(
    val courseId: Long,
    val targetPoseId: Long,
    val targetPoseName: String,
    /** 개요 상단의 히어로 이미지. 홈 카드와 같은 그림이라 같은 키다. */
    val targetPoseImageAssetKey: String?,
    val name: String,
    val recommendationReason: String?,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val exerciseCount: Int,
    val totalSetCount: Int,
    val estimatedDurationSeconds: Int?,
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
    val imageAssetKey: String?,
    val category: String?,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
    val estimatedKcal: Int?,
)

/**
 * 자세 도전 현황 한 줄. "낙타자세 3 / 4 체크 · 도전 중" 이다.
 *
 * **회원이 시작한 코스가 아니라 서비스가 제공하는 핀포즈 전체가 한 줄씩 나온다.** 코스는
 * 추천이지 처방이 아니다 — 온보딩에서 한 번 제안할 뿐이고, 이 화면은 전체를 펼쳐두고 회원이
 * 아무거나 골라 시작하게 한다.
 *
 * 그래서 코스 쪽 값 셋이 nullable 이다. **아직 시작하지 않은 자세는 `0 / 4` 가 아니라 null**
 * 이어야 한다. 0/4 는 "시작했는데 아직 한 스텝도 안 함" 이고 null 은 "아직 열지 않음" 이라
 * 화면이 둘을 다르게 그린다.
 *
 * 프로필의 "완수한 자세 목록" 도 같은 모델을 상태로 걸러 쓴다.
 */
data class TargetPoseProgressView(
    val targetPoseId: Long,
    val targetPoseName: String,
    val targetPoseImageAssetKey: String?,
    val bodyPartCode: String,
    val level: Int,
    val courseId: Long?,
    val completedStepCount: Int?,
    val totalStepCount: Int?,
    val completed: Boolean,
) {
    /** 시작했고 아직 완성하지 않은 상태. 화면의 "도전 중" 칩이 세는 값이다. */
    val inProgress: Boolean get() = courseId != null && !completed
}

/**
 * 자세 도전 현황 전체.
 *
 * **집계는 `completedOnly` 필터와 무관하게 언제나 전체 기준이다.** 화면의 칩 세 개가
 * `전체 9 / 도전 중 3 / 완성 2` 를 항상 함께 보여주므로, 걸러진 목록으로 세면 나머지 칩의
 * 숫자를 낼 수 없다.
 *
 * `totalCount` 는 `inProgressCount + completedCount` 가 아니다. 아직 시작하지 않은 자세가
 * 그 차이만큼 있다.
 */
data class TargetPoseProgressSummaryView(
    val totalCount: Int,
    val inProgressCount: Int,
    val completedCount: Int,
    val targetPoses: List<TargetPoseProgressView>,
)
