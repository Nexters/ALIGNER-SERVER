package team.aligner.support.core

/**
 * 모든 도메인 예외의 부모.
 *
 * 각 도메인은 model/exception/ 아래에서 이 클래스를 상속하고,
 * 자기 ErrorCode 를 넘긴다 (docs/architecture.md §3).
 */
abstract class BaseException(
    val errorCode: ErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)
