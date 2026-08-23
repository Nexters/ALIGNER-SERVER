package team.aligner.mock

import team.aligner.support.core.BaseException
import team.aligner.support.core.ErrorCode

/**
 * 목도 **실제 서버와 같은 오류 포맷**을 내야 한다. BaseException 을 타면
 * GlobalExceptionHandler 가 `code`·`message` 로 바꿔준다 (docs/architecture.md §9).
 *
 * 프론트가 오류 분기까지 목으로 만들 수 있어야 실제 서버로 옮길 때 고칠 것이 없다.
 */
internal class MockNotFoundException(
    errorCode: String,
    errorMessage: String,
) : BaseException(MockErrorCode(errorCode, errorMessage))

private data class MockErrorCode(
    override val code: String,
    override val message: String,
) : ErrorCode {
    override val status: Int = 404
}
