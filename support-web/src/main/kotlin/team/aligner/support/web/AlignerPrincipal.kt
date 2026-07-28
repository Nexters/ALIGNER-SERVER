package team.aligner.support.web

/**
 * "이 요청을 누가 보냈나"를 웹 계층에서 표현한다.
 *
 * member 도메인과 혼동하지 않는다. 회원의 실제 정보·가입·프로필은 member 가 소유하고,
 * 여기에는 식별자 등 인증에 필요한 최소한만 담는다 (docs/architecture.md §9).
 *
 * api 모듈은 이 값을 @AuthenticationPrincipal 로 받아 service 에 **파라미터로** 넘긴다.
 * service 시그니처에 Authentication·Principal 이 등장하면 잘못된 것이다.
 */
data class AlignerPrincipal(
    val memberId: Long,
)
