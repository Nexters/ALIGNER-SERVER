package team.aligner.member.model.exception

import team.aligner.support.core.ErrorCode

/**
 * member 도메인 에러.
 *
 * 도메인은 HTTP 응답 포맷을 모른다. 상태 코드만 값으로 들고 있고 변환은 support-web 의
 * GlobalExceptionHandler 가 한다 (docs/architecture.md §9).
 */
enum class MemberErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : ErrorCode {
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다"),
    INVALID_NICKNAME(400, "INVALID_NICKNAME", "닉네임은 1자 이상 50자 이하여야 합니다"),
    INVALID_HEIGHT(400, "INVALID_HEIGHT", "키는 100cm 이상 250cm 이하여야 합니다"),
    INVALID_WEIGHT(400, "INVALID_WEIGHT", "몸무게는 20kg 이상 300kg 이하여야 합니다"),
    INVALID_REINFORCEMENT_SETTING(400, "INVALID_REINFORCEMENT_SETTING", "강화 난이도는 1 이상 3 이하여야 합니다"),
}
