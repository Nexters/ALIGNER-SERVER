package team.aligner.screening.service

import team.aligner.screening.model.ScreeningAnswer

/**
 * 자세 체감 선택 제출.
 *
 * `memberId` 를 명령에 담지 않는다. api 가 `AlignerPrincipal` 에서 꺼내 **파라미터로** 넘긴다 —
 * 명령에 섞으면 클라이언트가 보낸 본문으로 남의 회원 식별자를 넣을 여지가 생긴다
 * (docs/architecture.md §9).
 */
data class SubmitScreeningCommand(
    val perceivedBodyPartCode: String,
    val answers: List<ScreeningAnswer>,
)
