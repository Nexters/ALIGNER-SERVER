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
    /** 자세 영상의 포스터 프레임. 위 키와 달리 URL 이고 파일은 YMove 가 갖는다. */
    val targetPoseThumbnailUrl: String?,
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
    /** 오늘 이 코스를 완주했는지. 화면이 완료 상태 홈으로 갈아타는 신호다. */
    val completed: Boolean,
    /** 완주하지 않았으면 null 이다. [TomorrowCoursePreviewView] 참고. */
    val tomorrowPreview: TomorrowCoursePreviewView?,
)

/**
 * 「내일 운동 미리보기」. **오늘의 코스를 완주했을 때만 있다.**
 *
 * 같은 부위에서 아직 4 번 완수하지 못한 자세 중 하나를 무작위로 고른 것이다. 다음에 무엇을
 * 할지 서버가 정해 주는 예약이 아니라 **제안**이다 — 저장하지 않고 조회할 때마다 다시
 * 고른다. 코스에 일자 개념이 생긴 것이 아니다 — `course.scheduled_on` 은 여전히 없다
 * (docs/domains.md §7-9).
 *
 * 다만 **같은 날 같은 회원에게는 같은 자세가 나온다.** 난수의 씨앗을 회원과 날짜로 고정해서,
 * 홈을 다시 불러올 때마다 카드가 바뀌지 않는다.
 *
 * **방금 완주한 자세도 후보다.** 자세 하나를 완성하려면 같은 코스를 4 번 완주해야 하므로
 * 같은 자세가 다시 나오는 것이 정상 루프다.
 *
 * **코스 식별자를 싣지 않는다.** 아직 시작하지 않은 자세는 코스가 없고, 이미 완주한 코스는
 * 그대로 열면 끝난 상태가 보인다. 화면은 `bodyPartCode` · `level` 로 코스 추천을 호출한다 —
 * 추천은 멱등하고 완주한 코스는 그때 다음 회차로 다시 열린다.
 */
data class TomorrowCoursePreviewView(
    val targetPoseId: Long,
    val targetPoseName: String,
    val targetPoseImageAssetKey: String?,
    /** 자세 영상의 포스터 프레임. 위 키와 달리 URL 이고 파일은 YMove 가 갖는다. */
    val targetPoseThumbnailUrl: String?,
    val bodyPartCode: String,
    val level: Int,
    val name: String,
    val recommendationReason: String?,
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
    /** 자세 영상의 포스터 프레임. 위 키와 달리 URL 이고 파일은 YMove 가 갖는다. */
    val targetPoseThumbnailUrl: String?,
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
    /**
     * 영상 포스터 프레임. `imageAssetKey` 와 자리가 다르다 — 그림은 프론트 정적 자산의 키이고
     * 이쪽은 YMove 자산의 URL 이다. 둘 다 있을 수 있고 화면이 무엇을 그릴지 고른다.
     */
    val thumbnailUrl: String?,
    val category: String?,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
    val estimatedKcal: Int?,
)

/**
 * 자세 도전 현황 한 줄. "낙타자세 3 / 4 · 도전 중" 이다.
 *
 * **`3 / 4` 는 파이어로그다** — 코스를 한 번 완주할 때마다 하나씩 오르고, 4 개를 채우면
 * `completed` 다. 코스 안에서 완료한 스텝 수(`completedStepCount`)와 다르다.
 *
 * **회원이 시작한 코스가 아니라 서비스가 제공하는 핀포즈 전체가 한 줄씩 나온다.** 코스는
 * 추천이지 처방이 아니다 — 온보딩에서 한 번 제안할 뿐이고, 이 화면은 전체를 펼쳐두고 회원이
 * 아무거나 골라 시작하게 한다.
 *
 * 그래서 코스 쪽 값이 nullable 이다. **아직 시작하지 않은 자세는 `0 / 4` 가 아니라 null**
 * 이어야 한다. 0/4 는 "시작했는데 아직 한 번도 완주하지 못함" 이고 null 은 "아직 열지 않음"
 * 이라 화면이 둘을 다르게 그린다.
 *
 * 프로필의 "완수한 자세 목록" 도 같은 모델을 상태로 걸러 쓴다.
 */
data class TargetPoseProgressView(
    val targetPoseId: Long,
    val targetPoseName: String,
    val targetPoseImageAssetKey: String?,
    /** 자세 영상의 포스터 프레임. 위 키와 달리 URL 이고 파일은 YMove 가 갖는다. */
    val targetPoseThumbnailUrl: String?,
    val bodyPartCode: String,
    val level: Int,
    val courseId: Long?,
    /** 이번 회차에서 완료한 스텝 수. 코스 개요의 진행도다. */
    val completedStepCount: Int?,
    val totalStepCount: Int?,
    /** 지금까지 완주한 횟수 = 붙은 도장 수. 화면의 `3 / 4` 의 3 이다. */
    val acquiredStampCount: Int?,
    /** 세그먼트 개수. 화면이 4 를 하드코딩하지 않게 서버가 함께 내린다. */
    val requiredStampCount: Int,
    /** 도장을 다 채웠는지. 화면의 "완성" 이다. */
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
