package team.aligner.support.web

import team.aligner.support.core.ErrorCode

/**
 * 모든 실패 응답의 공통 포맷.
 *
 * Spring 의 `org.springframework.web.ErrorResponse` 와 단순명이 겹치지 않도록 `Api` 접두사를 붙였다.
 * GlobalExceptionHandler 가 상속하는 ResponseEntityExceptionHandler 의 시그니처에 그 타입이
 * 실제로 등장해서, 이름이 같으면 자동 import 가 조용히 다른 쪽을 고를 수 있는 자리다.
 */
data class ApiErrorResponse(
    val code: String,
    val message: String,
) {
    companion object {
        fun from(errorCode: ErrorCode): ApiErrorResponse = ApiErrorResponse(errorCode.code, errorCode.message)
    }
}
