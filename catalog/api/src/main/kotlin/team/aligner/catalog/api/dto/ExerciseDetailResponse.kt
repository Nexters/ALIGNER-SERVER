package team.aligner.catalog.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseVoiceCueView
import java.math.BigDecimal

/**
 * 운동 가이드 화면 응답.
 *
 * 칼로리를 내리지 않고 metValue 만 내린다. kcal 은 회원 몸무게의 함수인데 몸무게는 member
 * 소유이고 catalog 는 member 를 의존할 수 없다 (docs/domains.md §1, §4-3).
 *
 * **`videoUrl` 은 YMove 연동 전까지 항상 null 이다.** 자리를 먼저 만들어 두는 것은 프론트가
 * 플레이어를 미리 짤 수 있게 하기 위해서다 (§7-4·5·6).
 */
@Schema(description = "운동 가이드 화면 전체")
data class ExerciseDetailResponse(
    @field:Schema(description = "운동 식별자", example = "1")
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
        description = "재생 영상 URL. 소스가 YMove 라 우리가 파일을 갖지 않는다. **연동 전까지 항상 null 이다**",
        example = "https://ymove.example.com/v/cat-cow.mp4",
        nullable = true,
    )
    val videoUrl: String?,
    @field:Schema(description = "권장 세트 수. 시간으로 수행하는 운동이면 null 이다", example = "3", nullable = true)
    val defaultSetCount: Int?,
    @field:Schema(description = "세트당 권장 반복 수. 시간으로 수행하는 운동이면 null 이다", example = "10", nullable = true)
    val defaultRepCount: Int?,
    @field:Schema(description = "권장 수행 시간(초). 횟수로 수행하는 운동이면 null 이다", example = "30", nullable = true)
    val defaultDurationSeconds: Int?,
    @field:Schema(
        description = "운동 강도(MET). kcal 은 회원 몸무게의 함수인데 몸무게는 member 소유라 서버가 계산하지 않는다",
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
    @field:Schema(description = "수행 시 주의사항", example = "목에 통증이 오면 즉시 멈춘다", nullable = true)
    val cautionNote: String?,
    @field:Schema(description = "근육맵 탭에 쓰는 근육 목록")
    val muscles: List<MuscleResponse>,
    @field:Schema(description = "음성 큐잉 대본. displayOrder 오름차순이다")
    val voiceCues: List<VoiceCueResponse>,
) {
    companion object {
        fun from(view: ExerciseDetailView): ExerciseDetailResponse =
            ExerciseDetailResponse(
                exerciseId = view.exerciseId,
                name = view.name,
                imageAssetKey = view.imageAssetKey,
                videoUrl = view.videoUrl,
                defaultSetCount = view.defaultSetCount,
                defaultRepCount = view.defaultRepCount,
                defaultDurationSeconds = view.defaultDurationSeconds,
                metValue = view.metValue,
                difficulty = view.difficulty,
                category = view.category,
                cautionNote = view.cautionNote,
                muscles = view.muscles.map(MuscleResponse::from),
                voiceCues = view.voiceCues.map(VoiceCueResponse::from),
            )
    }
}

/**
 * 두 offset 은 타임코드가 확정되기 전까지 null 이다. 클라이언트는 null 이면 displayOrder
 * 순차 재생으로 읽는다 (docs/domains.md §4-3).
 *
 * endOffsetSeconds 는 유지 구간이 없는 큐에서 null 로 남는다.
 */
@Schema(description = "음성 큐 한 줄")
data class VoiceCueResponse(
    @field:Schema(description = "재생 순서. 작을수록 먼저다", example = "1")
    val displayOrder: Int,
    @field:Schema(
        description = "재생 시작 지점(초). 타임코드가 확정되기 전이면 null 이고, 그때는 displayOrder 순차 재생으로 읽는다",
        example = "0",
        nullable = true,
    )
    val startOffsetSeconds: Int?,
    @field:Schema(description = "재생 종료 지점(초). 유지 구간이 없는 큐에서는 null 이다", example = "5", nullable = true)
    val endOffsetSeconds: Int?,
    @field:Schema(description = "읽어줄 문장", example = "턱을 뒤로 당겨 이중 턱을 만듭니다")
    val content: String,
) {
    companion object {
        fun from(view: ExerciseVoiceCueView): VoiceCueResponse =
            VoiceCueResponse(
                displayOrder = view.displayOrder,
                startOffsetSeconds = view.startOffsetSeconds,
                endOffsetSeconds = view.endOffsetSeconds,
                content = view.content,
            )
    }
}
