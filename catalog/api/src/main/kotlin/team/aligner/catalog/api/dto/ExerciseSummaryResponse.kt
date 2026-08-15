package team.aligner.catalog.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.catalog.model.view.ExerciseSummaryView
import java.math.BigDecimal

/**
 * 운영 목록의 운동 한 줄.
 *
 * 근육·음성 큐·주의사항을 싣지 않는다. 목록에 자식을 실으면 조회에 조인이 행 수만큼 붙고
 * 응답도 그만큼 커진다 — 하나를 자세히 볼 때는 `GET /catalog/exercises/{exerciseId}` 다.
 *
 * `videoUrl` 이 없다. 재생 URL 은 48 시간 만료라 요청 시점에 YMove 로 채우는 값이고,
 * 목록에서 행 수만큼 외부 호출을 낼 수 없다 (docs/domains.md §4-3-1).
 */
@Schema(description = "운영 목록용 운동 요약")
data class ExerciseSummaryResponse(
    @field:Schema(description = "운동 식별자", example = "101")
    val exerciseId: Long,
    @field:Schema(description = "운동 이름", example = "턱 당기기")
    val name: String,
    @field:Schema(
        description = "대표 이미지 asset 키. **URL 이 아니다** — 파일은 프론트가 갖고 키로 매핑한다",
        example = "exercise/cat-cow",
        nullable = true,
    )
    val imageAssetKey: String?,
    @field:Schema(
        description =
            "영상 포스터 프레임 URL. **imageAssetKey 와 달리 그대로 열리는 URL 이다** — " +
                "목록에서 그림을 확인할 때 쓴다. videoUrl 과 달리 서명도 만료도 없는 seed 값이라 외부 호출이 없다",
        example = "https://exercise-api.ymove.app/thumbnail/cat-cow-pose.jpg",
        nullable = true,
    )
    val thumbnailUrl: String?,
    @field:Schema(description = "권장 세트 수. 시간으로 수행하는 운동이면 null 이다", example = "3", nullable = true)
    val defaultSetCount: Int?,
    @field:Schema(description = "세트당 권장 반복 수. 시간으로 수행하는 운동이면 null 이다", example = "10", nullable = true)
    val defaultRepCount: Int?,
    @field:Schema(description = "권장 수행 시간(초). 횟수로 수행하는 운동이면 null 이다", example = "30", nullable = true)
    val defaultDurationSeconds: Int?,
    @field:Schema(
        description = "운동 강도(MET). kcal 은 회원 몸무게의 함수라 catalog 가 계산하지 않는다",
        example = "2.5",
        nullable = true,
    )
    val metValue: BigDecimal?,
    @field:Schema(description = "난이도. 감수 전 데이터라 아직 값 집합을 고정하지 않았다", example = "EASY", nullable = true)
    val difficulty: String?,
    @field:Schema(
        description = "코스 스텝에 표시하는 분류. 감수 전 데이터라 아직 값 집합을 고정하지 않았다",
        example = "가동성 웜업",
        nullable = true,
    )
    val category: String?,
) {
    companion object {
        fun from(view: ExerciseSummaryView): ExerciseSummaryResponse =
            ExerciseSummaryResponse(
                exerciseId = view.exerciseId,
                name = view.name,
                imageAssetKey = view.imageAssetKey,
                thumbnailUrl = view.thumbnailUrl,
                defaultSetCount = view.defaultSetCount,
                defaultRepCount = view.defaultRepCount,
                defaultDurationSeconds = view.defaultDurationSeconds,
                metValue = view.metValue,
                difficulty = view.difficulty,
                category = view.category,
            )
    }
}
