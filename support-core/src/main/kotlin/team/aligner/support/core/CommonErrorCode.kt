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
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다"),
    BAD_REQUEST(400, "BAD_REQUEST", "요청이 올바르지 않습니다"),
    NOT_FOUND(404, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다"),
    ;

    companion object {
        /**
         * 프레임워크가 이미 상태 코드를 정한 예외를 공통 포맷으로 되돌릴 때 쓴다
         * (support-web 의 GlobalExceptionHandler).
         *
         * 매칭되는 값이 없으면 4xx 는 BAD_REQUEST, 나머지는 INTERNAL_ERROR 로 접는다.
         * 여기 없는 상태 코드를 응답에 쓰고 싶으면 도메인 ErrorCode 로 명시한다.
         */
        fun ofStatus(status: Int): CommonErrorCode =
            entries.firstOrNull { it.status == status }
                ?: if (status in 400..499) BAD_REQUEST else INTERNAL_ERROR
    }
}
