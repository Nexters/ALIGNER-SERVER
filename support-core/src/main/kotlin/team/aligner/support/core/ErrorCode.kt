package team.aligner.support.core

/**
 * 도메인 예외가 들고 다니는 에러 계약.
 *
 * 도메인은 HTTP 응답 포맷을 모른다. 상태 코드만 값으로 들고 있고,
 * 실제 응답 변환은 support-web 의 GlobalExceptionHandler 가 한다 (docs/architecture.md §9).
 */
interface ErrorCode {
    /** HTTP 상태 코드. Spring 타입을 쓰지 않으려고 Int 로 둔다. */
    val status: Int

    /** 클라이언트가 분기에 쓰는 코드. 사람이 읽을 수 있게 대문자 스네이크로 쓴다. */
    val code: String

    val message: String
}
