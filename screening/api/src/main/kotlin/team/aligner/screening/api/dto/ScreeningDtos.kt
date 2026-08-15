package team.aligner.screening.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.screening.model.PerceivedDifficulty
import team.aligner.screening.model.ScreeningAnswer
import team.aligner.screening.model.view.BodyPartView
import team.aligner.screening.model.view.ScreeningCauseView
import team.aligner.screening.model.view.ScreeningResultView
import java.time.Instant

@Schema(description = "부위")
data class BodyPartResponse(
    @field:Schema(description = "부위 코드. 진단 결과 뒤 강화 부위 선택 화면의 선택지다")
    val bodyPartCode: BodyPartCode,
    @field:Schema(description = "표시용 이름", example = "등")
    val name: String,
) {
    companion object {
        fun from(view: BodyPartView) = BodyPartResponse(bodyPartCode = BodyPartCode.from(view.bodyPartCode), name = view.name)
    }
}

/**
 * **부위를 받지 않는다.** 온보딩이 부위를 먼저 묻지 않고 자세 체감만 받는다
 * (docs/domains.md §4-2).
 */
@Schema(description = "자세 체감 선택 제출")
data class SubmitScreeningRequest(
    @field:Schema(
        description = "고른 자세와 체감. 쉬웠던 자세와 어려웠던 자세를 각각 최대 4 개까지 담는다",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val answers: List<ScreeningAnswerRequest>,
) {
    fun toAnswers(): List<ScreeningAnswer> =
        answers.map { ScreeningAnswer(targetPoseId = it.targetPoseId, perceivedDifficulty = it.perceivedDifficulty) }
}

@Schema(description = "자세 하나에 대한 체감")
data class ScreeningAnswerRequest(
    @field:Schema(description = "catalog 자세 그리드에서 받은 식별자", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    val targetPoseId: Long,
    @field:Schema(description = "EASY 는 쉬웠던 자세, HARD 는 어려웠던 자세", requiredMode = Schema.RequiredMode.REQUIRED)
    val perceivedDifficulty: PerceivedDifficulty,
)

@Schema(description = "진단 결과. 원인이 순위로 실린다")
data class ScreeningResultResponse(
    @field:Schema(description = "이 진단의 식별자", example = "3")
    val resultId: Long,
    @field:Schema(description = "판별된 원인. rank 오름차순이다")
    val causes: List<ScreeningCauseResponse>,
    @field:Schema(description = "진단 시각")
    val createdAt: Instant,
) {
    companion object {
        fun from(view: ScreeningResultView) =
            ScreeningResultResponse(
                resultId = view.resultId,
                causes = view.causes.map(ScreeningCauseResponse::from),
                createdAt = view.createdAt,
            )
    }
}

@Schema(description = "판별된 원인 하나")
data class ScreeningCauseResponse(
    @field:Schema(description = "원인 코드", example = "THORACIC_STIFFNESS")
    val causeCode: String,
    @field:Schema(description = "표시용 이름", example = "굳은 흉추")
    val name: String,
    @field:Schema(description = "**원인이 있는 부위.** 회원이 고른 부위와 다를 수 있다")
    val bodyPartCode: BodyPartCode,
    @field:Schema(description = "결과 화면에 보여줄 설명", nullable = true)
    val description: String?,
    @field:Schema(description = "표시 순서. 1 이 가장 유력한 원인이다", example = "1")
    val rank: Int,
    @field:Schema(description = "분기 규칙 가중치의 합. 순위 근거다", example = "7")
    val score: Int,
) {
    companion object {
        fun from(view: ScreeningCauseView) =
            ScreeningCauseResponse(
                causeCode = view.causeCode,
                name = view.name,
                bodyPartCode = BodyPartCode.from(view.bodyPartCode),
                description = view.description,
                rank = view.rank,
                score = view.score,
            )
    }
}
