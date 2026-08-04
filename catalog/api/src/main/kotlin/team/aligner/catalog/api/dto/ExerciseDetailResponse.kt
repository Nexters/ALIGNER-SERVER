package team.aligner.catalog.api.dto

import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseVoiceCueView
import java.math.BigDecimal

/**
 * 운동 가이드 화면 응답.
 *
 * 칼로리를 내리지 않고 metValue 만 내린다. kcal 은 회원 몸무게의 함수인데 몸무게는 member
 * 소유이고 catalog 는 member 를 의존할 수 없다 (docs/domains.md §1, §4-3).
 *
 * 재생 URL 과 썸네일이 없다. YMove 연동은 후속 이슈다 (§7-4·5·6).
 */
data class ExerciseDetailResponse(
    val exerciseId: Long,
    val name: String,
    val defaultSetCount: Int?,
    val defaultRepCount: Int?,
    val defaultDurationSeconds: Int?,
    val metValue: BigDecimal?,
    val difficulty: String?,
    val cautionNote: String?,
    val muscles: List<MuscleResponse>,
    val voiceCues: List<VoiceCueResponse>,
) {
    companion object {
        fun from(view: ExerciseDetailView): ExerciseDetailResponse =
            ExerciseDetailResponse(
                exerciseId = view.exerciseId,
                name = view.name,
                defaultSetCount = view.defaultSetCount,
                defaultRepCount = view.defaultRepCount,
                defaultDurationSeconds = view.defaultDurationSeconds,
                metValue = view.metValue,
                difficulty = view.difficulty,
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
data class VoiceCueResponse(
    val displayOrder: Int,
    val startOffsetSeconds: Int?,
    val endOffsetSeconds: Int?,
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
