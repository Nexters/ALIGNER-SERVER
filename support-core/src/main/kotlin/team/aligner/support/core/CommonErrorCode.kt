package team.aligner.support.core

/**
 * 어느 도메인에도 속하지 않는 에러.
 *
 * 도메인이 자기 에러를 여기에 추가하지 않는다. 도메인 에러는 그 도메인의
 * model/exception/ 에 자기 ErrorCode 구현으로 둔다.
 */
enum class CommonErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "서버에서 처리하지 못한 오류가 발생했습니다"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
}
