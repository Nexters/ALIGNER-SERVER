package team.aligner.screening.service

import team.aligner.screening.model.ScreeningAnswer

/**
 * 자세 체감 선택 제출.
 *
 * **부위가 없다.** 온보딩이 부위를 먼저 묻지 않고, 판별된 원인을 본 뒤에 강화할 부위를
 * 고르는 순서다 (docs/domains.md §4-2). 그 선택은 회원의 지속 설정이라 member 가 갖는다.
 *
 * `memberId` 를 명령에 담지 않는다. api 가 `AlignerPrincipal` 에서 꺼내 **파라미터로** 넘긴다 —
 * 명령에 섞으면 클라이언트가 보낸 본문으로 남의 회원 식별자를 넣을 여지가 생긴다
 * (docs/architecture.md §9).
 */
data class SubmitScreeningCommand(
    val answers: List<ScreeningAnswer>,
)
