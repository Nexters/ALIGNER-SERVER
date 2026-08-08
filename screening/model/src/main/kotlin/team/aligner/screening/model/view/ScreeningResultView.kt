package team.aligner.screening.model.view

import java.time.Instant

/**
 * 진단 결과 화면. 판별된 원인을 순위로 싣는다.
 *
 * 애그리거트를 그대로 내리지 않는다. 화면은 `cause` 의 이름·설명을 원인 코드와 함께 필요로
 * 하는데, 그건 마스터 seed 와의 조인이라 Command 모델에 넣을 것이 아니다.
 */
data class ScreeningResultView(
    val resultId: Long,
    val causes: List<ScreeningCauseView>,
    val createdAt: Instant,
)

/**
 * 판별된 원인 하나. `bodyPartCode` 는 **원인이 있는 부위**다.
 *
 * 결과 화면이 이 부위들을 보여주고, 회원은 그다음 화면에서 강화할 부위를 고른다.
 */
data class ScreeningCauseView(
    val causeCode: String,
    val name: String,
    val bodyPartCode: String,
    val description: String?,
    val rank: Int,
    val score: Int,
)
