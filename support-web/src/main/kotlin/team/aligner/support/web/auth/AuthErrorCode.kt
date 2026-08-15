package team.aligner.support.web.auth

import team.aligner.support.core.ErrorCode

/**
 * 인증 웹 계층 에러. 어느 도메인의 것도 아니라 support-web 이 갖는다.
 *
 * 도메인 에러는 각 도메인의 model/exception 에 둔다 (docs/architecture.md §9).
 */
enum class AuthErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : ErrorCode {
    /**
     * 인가 코드가 잘못됐거나 만료·재사용됐다. 인가 코드는 1 회용이라 재시도가 아니라
     * `Kakao.Auth.authorize()` 부터 다시 태워야 한다.
     *
     * KAKAO_TOKEN_INVALID 와 합치지 않는다. 프론트가 "로그인부터 다시" 와 "카카오 쪽 문제" 를
     * 구분하지 못하게 된다.
     */
    KAKAO_AUTH_CODE_INVALID(401, "KAKAO_AUTH_CODE_INVALID", "카카오 인가 코드가 유효하지 않습니다"),
    KAKAO_TOKEN_INVALID(401, "KAKAO_TOKEN_INVALID", "카카오 액세스 토큰이 유효하지 않습니다"),
    KAKAO_UNAVAILABLE(502, "KAKAO_UNAVAILABLE", "카카오 인증 서버에 연결하지 못했습니다"),
}
