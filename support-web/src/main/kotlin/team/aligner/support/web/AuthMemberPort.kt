package team.aligner.support.web

/**
 * 웹 계층이 회원 도메인에 요구하는 최소 계약.
 *
 * **구현체는 여기 없다.** member/adapter-auth 가 member:contract 에 연결해 구현한다.
 * support-web 이 member:service·member:model 을 직접 의존하지 않게 하는 장치다
 * (docs/architecture.md §9).
 *
 * 이슈 #3 범위에서는 **인터페이스 선언만** 둔다. 이 port 를 주입받는 Bean 을 만들면
 * member 가 없는 지금은 기동이 실패한다.
 */
interface AuthMemberPort {
    fun findOrRegisterByKakao(command: KakaoLoginCommand): AuthenticatedMember
}

data class KakaoLoginCommand(
    val kakaoId: String,
    val nickname: String?,
    val profileImageUrl: String?,
)

data class AuthenticatedMember(
    val memberId: Long,
)
