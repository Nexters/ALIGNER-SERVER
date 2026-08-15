package team.aligner.support.web

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.support.core.ErrorCode

/**
 * 모든 실패 응답의 공통 포맷.
 *
 * Spring 의 `org.springframework.web.ErrorResponse` 와 단순명이 겹치지 않도록 `Api` 접두사를 붙였다.
 * GlobalExceptionHandler 가 상속하는 ResponseEntityExceptionHandler 의 시그니처에 그 타입이
 * 실제로 등장해서, 이름이 같으면 자동 import 가 조용히 다른 쪽을 고를 수 있는 자리다.
 */
@Schema(
    name = "ApiErrorResponse",
    description = "모든 실패 응답의 공통 포맷. 클라이언트는 HTTP 상태가 아니라 code 로 분기한다",
)
data class ApiErrorResponse(
    @field:Schema(description = "분기에 쓰는 에러 코드", example = "MEMBER_NOT_FOUND")
    val code: String,
    @field:Schema(description = "사람이 읽는 설명. 그대로 노출해도 되는 문장이다", example = "회원을 찾을 수 없습니다")
    val message: String,
) {
    companion object {
        fun from(errorCode: ErrorCode): ApiErrorResponse = ApiErrorResponse(errorCode.code, errorCode.message)
    }
}
