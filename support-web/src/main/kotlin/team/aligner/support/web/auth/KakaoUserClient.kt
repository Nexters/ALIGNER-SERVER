package team.aligner.support.web.auth

/**
 * 카카오 액세스 토큰으로 사용자 정보를 확인한다.
 *
 * 인터페이스로 분리한 것은 테스트가 실제 카카오를 치지 않게 하기 위해서다. 모듈까지 쪼개지는
 * 않는다 — catalog 의 adapter-ymove 가 별도 모듈인 이유는 그쪽 infrastructure 에 HTTP 타입이
 * 아예 없어서인데, support-web 에는 그 제약이 없다 (docs/domains.md §4-3-1 대비).
 */
interface KakaoUserClient {
    fun fetchUser(kakaoAccessToken: String): KakaoUser
}

/**
 * nickname·profileImageUrl 은 null 일 수 있다. 카카오 프로필 제공 동의 항목이라 미동의 시 온다.
 */
data class KakaoUser(
    val kakaoId: String,
    val nickname: String?,
    val profileImageUrl: String?,
)
