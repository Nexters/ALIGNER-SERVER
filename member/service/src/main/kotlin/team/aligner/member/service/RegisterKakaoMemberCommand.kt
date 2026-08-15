package team.aligner.member.service

/**
 * 카카오 가입·재로그인 입력.
 *
 * 여기 들어오는 kakaoId 는 이미 검증된 값이다. 카카오 액세스 토큰 검증은 인증 웹 계층이
 * 담당하고 member 는 "이 카카오 식별자의 회원을 찾거나 만든다"만 책임진다
 * (docs/architecture.md §9).
 */
data class RegisterKakaoMemberCommand(
    val kakaoId: String,
    val nickname: String?,
    val profileImageUrl: String?,
)
