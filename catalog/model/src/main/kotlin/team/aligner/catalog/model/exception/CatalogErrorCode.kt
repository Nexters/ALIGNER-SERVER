package team.aligner.catalog.model.exception

import team.aligner.support.core.ErrorCode

/**
 * catalog 도메인 에러.
 *
 * 도메인은 HTTP 응답 포맷을 모른다. 상태 코드만 값으로 들고 있고 변환은 support-web 의
 * GlobalExceptionHandler 가 한다 (docs/architecture.md §9).
 */
enum class CatalogErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : ErrorCode {
    EXERCISE_NOT_FOUND(404, "EXERCISE_NOT_FOUND", "운동을 찾을 수 없습니다"),
    TARGET_POSE_NOT_FOUND(404, "TARGET_POSE_NOT_FOUND", "목표 자세를 찾을 수 없습니다"),
}
