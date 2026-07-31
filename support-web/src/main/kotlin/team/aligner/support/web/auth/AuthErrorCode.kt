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
    KAKAO_TOKEN_INVALID(401, "KAKAO_TOKEN_INVALID", "카카오 액세스 토큰이 유효하지 않습니다"),
    KAKAO_UNAVAILABLE(502, "KAKAO_UNAVAILABLE", "카카오 인증 서버에 연결하지 못했습니다"),
}
