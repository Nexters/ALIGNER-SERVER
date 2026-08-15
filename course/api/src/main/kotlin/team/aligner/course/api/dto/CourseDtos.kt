package team.aligner.course.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.course.model.view.CourseDetailView
import team.aligner.course.model.view.CourseStepExerciseView
import team.aligner.course.model.view.CourseStepView
import team.aligner.course.model.view.TargetPoseProgressSummaryView
import team.aligner.course.model.view.TargetPoseProgressView
import team.aligner.course.model.view.TodayCourseView
import team.aligner.course.model.view.TomorrowCoursePreviewView
import java.time.Instant

/**
 * 추천 요청.
 *
 * **자세 식별자를 받지 않는다.** 부위와 난이도만 받고 자세는 서버가 catalog 에서 찾는다 —
 * 클라이언트가 자세를 지정하면 고르지 않은 난이도의 코스를 받아갈 수 있다.
 *
 * **원인도 받지 않는다.** 서버가 최신 진단으로 검증한다 (docs/domains.md §2).
 */
@Schema(description = "코스 추천 요청")
data class RecommendCourseRequest(
    @field:Schema(
        description = "강화할 부위 코드. `GET /screening/body-parts` 의 값이다",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bodyPartCode: BodyPartCode,
    @field:Schema(
        description = "난이도. 1(하)·2(중)·3(상)이며 **목표 자세의 레벨과 같은 값**이다",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val level: Int,
)

@Schema(description = "추천된 코스 식별자")
data class RecommendCourseResponse(
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
    @field:Schema(
        description = "목표 자세 레벨. 회원이 고른 난이도와 같다. catalog 에서 자세를 찾지 못하면 null 이다",
        example = "1",
        nullable = true,
    )
    val targetPoseLevel: Int?,
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
    @field:Schema(
        description = "예상 수행 시간(초). 운동 하나라도 시간을 모르면 null 이고 0 이 아니다",
        example = "900",
        nullable = true,
    )
    val estimatedDurationSeconds: Int?,
    @field:Schema(description = "예상 칼로리. 몸무게나 MET 이 없으면 null 이고 0 이 아니다", example = "69", nullable = true)
    val estimatedKcal: Int?,
    @field:Schema(
        description = "오늘 이 코스를 완주했는지. true 면 화면은 완료 상태 홈을 그린다",
        example = "false",
    )
    val completed: Boolean,
    @field:Schema(
        description = "「내일 운동 미리보기」. **`completed` 가 true 일 때만 있다.** 진행 중이면 null 이다",
        nullable = true,
    )
    val tomorrowPreview: TomorrowCoursePreviewResponse?,
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
                completed = view.completed,
                tomorrowPreview = view.tomorrowPreview?.let(TomorrowCoursePreviewResponse::from),
            )
    }
}

/**
 * 「내일 운동 미리보기」 카드.
 *
 * **예약이 아니라 제안이다.** 같은 부위에서 아직 4 번 완수하지 못한 자세 중 하나를 무작위로
 * 고른 것이고 서버에 저장하지 않는다. 다만 같은 날 같은 회원에게는 같은 자세가 나온다 —
 * 홈을 다시 불러올 때마다 카드가 바뀌면 안 되기 때문이다. 방금 완주한 자세도 후보에 남는다 —
 * 자세 하나를 완성하려면 같은 코스를 4 번 완주해야 한다.
 *
 * **`courseId` 가 없다.** 아직 시작하지 않은 자세는 코스 자체가 없고, 이미 완주한 코스를
 * 식별자로 열면 끝난 상태가 보인다. 이 카드를 눌렀을 때는 `bodyPartCode` · `level` 로
 * `POST /courses` 를 호출한다 — 추천은 멱등하고 완주한 코스는 그때 다음 회차로 다시 열린다.
 */
@Schema(description = "내일 운동 미리보기. 오늘의 코스를 완주했을 때만 내려온다")
data class TomorrowCoursePreviewResponse(
    @field:Schema(description = "목표 자세 식별자", example = "4")
    val targetPoseId: Long,
    @field:Schema(description = "목표 자세 이름", example = "비둘기 자세")
    val targetPoseName: String,
    @field:Schema(description = "목표 자세 이미지 asset 키. URL 이 아니다", nullable = true)
    val targetPoseImageAssetKey: String?,
    @field:Schema(description = "이 자세의 부위. 오늘의 코스와 같은 부위다. **코스 추천 호출에 그대로 쓴다**")
    val bodyPartCode: BodyPartCode,
    @field:Schema(description = "난이도. **코스 추천의 level 로 그대로 쓴다**", example = "2")
    val level: Int,
    @field:Schema(description = "코스 이름", example = "비둘기자세 정복하기")
    val name: String,
    @field:Schema(description = "코스 추천 이유. 감수 문구다", nullable = true)
    val recommendationReason: String?,
    @field:Schema(description = "전체 스텝 수", example = "6")
    val totalStepCount: Int,
    @field:Schema(description = "운동 개수", example = "6")
    val exerciseCount: Int,
    @field:Schema(description = "세트 합계", example = "6")
    val totalSetCount: Int,
    @field:Schema(
        description = "예상 수행 시간(초). 운동 하나라도 시간을 모르면 null 이고 0 이 아니다",
        example = "900",
        nullable = true,
    )
    val estimatedDurationSeconds: Int?,
    @field:Schema(description = "예상 칼로리. 몸무게나 MET 이 없으면 null 이고 0 이 아니다", example = "72", nullable = true)
    val estimatedKcal: Int?,
) {
    companion object {
        fun from(view: TomorrowCoursePreviewView) =
            TomorrowCoursePreviewResponse(
                targetPoseId = view.targetPoseId,
                targetPoseName = view.targetPoseName,
                targetPoseImageAssetKey = view.targetPoseImageAssetKey,
                bodyPartCode = BodyPartCode.from(view.bodyPartCode),
                level = view.level,
                name = view.name,
                recommendationReason = view.recommendationReason,
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
    @field:Schema(
        description = "목표 자세 이미지 asset 키. 개요 상단 히어로에 쓴다. URL 이 아니다",
        nullable = true,
    )
    val targetPoseImageAssetKey: String?,
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
    @field:Schema(
        description = "예상 수행 시간(초). 운동 하나라도 시간을 모르면 null 이고 0 이 아니다",
        example = "900",
        nullable = true,
    )
    val estimatedDurationSeconds: Int?,
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
                targetPoseImageAssetKey = view.targetPoseImageAssetKey,
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
    @field:Schema(
        description = "운동 이미지 asset 키. 코스 순서 카드의 썸네일이다. URL 이 아니다",
        example = "exercise/cat-cow",
        nullable = true,
    )
    val imageAssetKey: String?,
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
                imageAssetKey = view.imageAssetKey,
                category = view.category,
                displayOrder = view.displayOrder,
                durationSeconds = view.durationSeconds,
                setCount = view.setCount,
                estimatedKcal = view.estimatedKcal,
            )
    }
}

/**
 * 자세 도전 현황 전체.
 *
 * **집계 셋은 `completed` 필터와 무관하게 언제나 전체 기준이다.** 화면의 칩이
 * `전체 9 / 도전 중 3 / 완성 2` 를 함께 보여주므로 걸러진 목록으로 세면 나머지 칩을
 * 그릴 수 없다.
 */
@Schema(description = "자세 도전 현황")
data class TargetPoseProgressResponse(
    @field:Schema(description = "서비스가 제공하는 자세 전체 개수. 화면의 \"전체\" 칩이다", example = "9")
    val totalCount: Int,
    @field:Schema(description = "시작했고 아직 완성하지 않은 자세 수. 화면의 \"도전 중\" 칩이다", example = "3")
    val inProgressCount: Int,
    @field:Schema(description = "완성한 자세 수. 화면의 \"완성\" 칩이다", example = "2")
    val completedCount: Int,
    @field:Schema(
        description =
            "부위·레벨 순으로 정렬된 자세 목록. **부위 섹션의 노출 순서는 `GET /screening/body-parts` 가 정한다.** " +
                "`completed` 파라미터를 주면 이 목록만 걸러지고 위의 집계 셋은 그대로다",
    )
    val targetPoses: List<TargetPoseProgressItem>,
) {
    companion object {
        fun from(view: TargetPoseProgressSummaryView) =
            TargetPoseProgressResponse(
                totalCount = view.totalCount,
                inProgressCount = view.inProgressCount,
                completedCount = view.completedCount,
                targetPoses = view.targetPoses.map(TargetPoseProgressItem::from),
            )
    }
}

/**
 * 자세 도전 현황 한 줄. "낙타자세 3 / 4 · 도전 중" 이다.
 *
 * **`3 / 4` 는 파이어로그다** — `acquiredStampCount / requiredStampCount` 이고, 코스를 한 번
 * 완주할 때마다 하나씩 오른다. 코스 안에서 완료한 스텝 수(`completedStepCount`)가 아니다.
 *
 * **아직 시작하지 않은 자세는 코스 쪽 값이 전부 null** 이다. `0 / 4` 가 아니다 — 0/4 는
 * "시작했는데 아직 한 번도 완주하지 못함" 이고 null 은 "아직 열지 않음" 이라 화면이 둘을
 * 다르게 그린다.
 */
@Schema(description = "자세 도전 현황 한 줄")
data class TargetPoseProgressItem(
    @field:Schema(description = "목표 자세 식별자", example = "3")
    val targetPoseId: Long,
    @field:Schema(description = "목표 자세 이름", example = "낙타자세")
    val targetPoseName: String,
    @field:Schema(description = "목표 자세 이미지 asset 키", nullable = true)
    val targetPoseImageAssetKey: String?,
    @field:Schema(description = "이 자세가 속한 부위. 화면의 섹션 구분이다")
    val bodyPartCode: BodyPartCode,
    @field:Schema(description = "난이도 단계. 부위 안에서 작을수록 쉽다", example = "1")
    val level: Int,
    @field:Schema(description = "이 자세의 코스 식별자. 아직 시작하지 않았으면 null 이다", example = "20", nullable = true)
    val courseId: Long?,
    @field:Schema(
        description = "이번 회차에서 완료한 스텝 수. **화면의 `3 / 4` 가 아니다.** 아직 시작하지 않았으면 null 이다",
        example = "3",
        nullable = true,
    )
    val completedStepCount: Int?,
    @field:Schema(description = "이 코스의 전체 스텝 수. 아직 시작하지 않았으면 null 이다", example = "4", nullable = true)
    val totalStepCount: Int?,
    @field:Schema(
        description =
            "완주 횟수 = 붙은 도장 수. **화면의 `3 / 4` 의 분자다.** 코스를 한 번 완주할 때마다 " +
                "하나씩 오른다. 아직 시작하지 않았으면 null 이고 0 이 아니다",
        example = "3",
        nullable = true,
    )
    val acquiredStampCount: Int?,
    @field:Schema(description = "완성에 필요한 완주 횟수. 화면의 `3 / 4` 의 분모다", example = "4")
    val requiredStampCount: Int,
    @field:Schema(description = "완성 여부. 완주 횟수를 다 채웠는지다. 시작하지 않았으면 false 다", example = "false")
    val completed: Boolean,
) {
    companion object {
        fun from(view: TargetPoseProgressView) =
            TargetPoseProgressItem(
                targetPoseId = view.targetPoseId,
                targetPoseName = view.targetPoseName,
                targetPoseImageAssetKey = view.targetPoseImageAssetKey,
                bodyPartCode = BodyPartCode.from(view.bodyPartCode),
                level = view.level,
                courseId = view.courseId,
                completedStepCount = view.completedStepCount,
                totalStepCount = view.totalStepCount,
                acquiredStampCount = view.acquiredStampCount,
                requiredStampCount = view.requiredStampCount,
                completed = view.completed,
            )
    }
}
