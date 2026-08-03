package team.aligner.support.web.auth

import team.aligner.support.core.BaseException
import team.aligner.support.core.ErrorCode

/**
 * GlobalExceptionHandler 가 BaseException 을 이미 잡으므로 별도 핸들러가 필요 없다.
 */
class AuthenticationFailedException(
    errorCode: ErrorCode,
    cause: Throwable? = null,
) : BaseException(errorCode, cause)
