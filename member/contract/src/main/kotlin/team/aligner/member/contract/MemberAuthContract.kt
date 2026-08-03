package team.aligner.member.contract

/**
 * 인증 계층이 member 도메인에 요구하는 계약. 통합 전용이라 좁게 만든다
 * (docs/architecture.md §7).
 *
 * 구현체는 internal 로 member:service 에 두고 Bean 도 거기서 등록한다.
 * 카카오 액세스 토큰 검증은 이 계약 바깥(support-web)에서 이미 끝났다 — 여기 들어오는
 * kakaoId 는 검증된 값이다.
 */
interface MemberAuthContract {
    fun findOrRegisterByKakao(command: KakaoMemberCommand): AuthenticatedMemberResponse
}

/**
 * nickname·profileImageUrl 은 null 일 수 있다. 카카오 프로필 제공 미동의 시 오지 않는다.
 */
data class KakaoMemberCommand(
    val kakaoId: String,
    val nickname: String?,
    val profileImageUrl: String?,
)

data class AuthenticatedMemberResponse(
    val memberId: Long,
)
