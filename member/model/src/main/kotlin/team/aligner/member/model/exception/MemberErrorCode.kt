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

    /**
     * 부위·난이도는 한 화면에서 같이 고르므로 실패 사유가 셋인데 코드는 하나다 —
     * 한쪽만 보냈거나, 부위 코드가 비었거나 너무 길거나, 난이도가 범위 밖이다.
     * 메시지가 셋을 모두 덮어야 클라이언트가 고칠 필드를 찾을 수 있다.
     */
    INVALID_REINFORCEMENT_SETTING(
        400,
        "INVALID_REINFORCEMENT_SETTING",
        "강화 부위와 난이도는 함께 보내야 하고, 부위 코드는 1자 이상 40자 이하, 난이도는 1 이상 3 이하여야 합니다",
    ),
}
