package team.aligner.training.model.exception

import team.aligner.support.core.ErrorCode

/**
 * training 도메인 에러.
 *
 * 도메인은 HTTP 응답 포맷을 모른다. 상태 코드만 값으로 들고 있고 변환은 support-web 의
 * GlobalExceptionHandler 가 한다 (docs/architecture.md §9).
 */
enum class TrainingErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : ErrorCode {
    SESSION_NOT_FOUND(404, "SESSION_NOT_FOUND", "세션을 찾을 수 없습니다"),

    /**
     * 세션을 시작하려는 코스 스텝이 없다. 남의 코스이거나 없는 스텝이다.
     */
    COURSE_STEP_NOT_FOUND(404, "COURSE_STEP_NOT_FOUND", "코스 스텝을 찾을 수 없습니다"),

    /**
     * 스텝에 운동이 하나도 편성돼 있지 않다. seed 문제라 회원 잘못이 아니다.
     */
    EMPTY_COURSE_STEP(422, "EMPTY_COURSE_STEP", "코스 스텝에 운동이 없습니다"),

    /**
     * 완료 요청에 이 세션의 것이 아닌 수행 기록이 섞여 있다.
     */
    UNKNOWN_EXERCISE_RECORD(400, "UNKNOWN_EXERCISE_RECORD", "이 세션에 없는 운동입니다"),
}
