package team.aligner.screening.model.view

import java.time.Instant

/**
 * 진단 결과 화면. 회원이 고른 부위와 판별된 원인을 순위로 함께 싣는다.
 *
 * 애그리거트를 그대로 내리지 않는다. 화면은 `cause` 의 이름·설명을 원인 코드와 함께 필요로
 * 하는데, 그건 마스터 seed 와의 조인이라 Command 모델에 넣을 것이 아니다.
 */
data class ScreeningResultView(
    val resultId: Long,
    val perceivedBodyPartCode: String,
    val causes: List<ScreeningCauseView>,
    val createdAt: Instant,
)

/**
 * 판별된 원인 하나. `bodyPartCode` 는 **원인이 있는 부위**이고 회원이 고른 부위와 다를 수 있다.
 */
data class ScreeningCauseView(
    val causeCode: String,
    val name: String,
    val bodyPartCode: String,
    val description: String?,
    val rank: Int,
    val score: Int,
)
