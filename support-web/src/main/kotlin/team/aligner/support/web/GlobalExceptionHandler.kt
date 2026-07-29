package team.aligner.support.web

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import team.aligner.support.core.BaseException
import team.aligner.support.core.CommonErrorCode

/**
 * 예외 → HTTP 응답 변환은 여기 한 곳에서만 한다.
 *
 * BaseException 과 ErrorCode 만 보고 처리한다. 도메인별 분기를 여기에 추가하지 않는다 —
 * 그 순간 support-web 이 도메인을 알게 된다 (docs/architecture.md §9).
 *
 * ComponentScan 을 쓰지 않으므로 @RestControllerAdvice 만으로는 등록되지 않는다.
 * SupportWebAutoConfiguration 이 @Bean 으로 등록한다 (§5).
 *
 * **ResponseEntityExceptionHandler 를 상속하는 이유.** 아래 catch-all 이 단독으로 있으면
 * ExceptionHandlerExceptionResolver 가 DefaultHandlerExceptionResolver 보다 먼저 돌기 때문에
 * 400(본문 파싱 실패·검증 실패)·404·405 가 전부 500 으로 나가고, 클라이언트 입력 오류가
 * error 로그를 오염시킨다. 부모가 프레임워크 예외를 구체 타입으로 먼저 잡는다.
 *
 * 다만 부모는 본문을 ProblemDetail(application/problem+json) 로 만든다. 그대로 두면
 * 실패 응답이 두 포맷으로 갈려서 클라이언트가 분기에 쓰는 code 필드가 프레임워크 예외에만
 * 없어진다. handleExceptionInternal 을 덮어 본문을 ApiErrorResponse 로 통일한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        // 상태 코드와 헤더는 부모 판단을 따르고 본문만 우리 포맷으로 바꾼다.
        // headers 를 버리면 405 의 Allow 나 406 의 협상 헤더가 함께 사라져서
        // 클라이언트가 무엇을 고쳐야 하는지 알 수 없게 된다.
        // 예외 메시지는 클라이언트 입력을 되비추므로 싣지 않는다.
        val errorCode = CommonErrorCode.ofStatus(statusCode.value())
        return ResponseEntity
            .status(statusCode)
            .headers(headers)
            .body(ApiErrorResponse.from(errorCode))
    }

    @ExceptionHandler(BaseException::class)
    fun handle(exception: BaseException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(exception.errorCode.status)
            .body(ApiErrorResponse.from(exception.errorCode))

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ApiErrorResponse> {
        // 원인을 삼키면 운영에서 못 찾는다. 응답에는 내부 메시지를 싣지 않는다.
        log.error("처리되지 않은 예외", exception)
        return ResponseEntity
            .status(CommonErrorCode.INTERNAL_ERROR.status)
            .body(ApiErrorResponse.from(CommonErrorCode.INTERNAL_ERROR))
    }
}
