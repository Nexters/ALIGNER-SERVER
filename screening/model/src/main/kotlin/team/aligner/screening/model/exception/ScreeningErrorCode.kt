package team.aligner.screening.model.exception

import team.aligner.support.core.ErrorCode

/**
 * screening 도메인 에러. 도메인 에러는 각 도메인의 model/exception 에 둔다
 * (docs/architecture.md §9).
 */
enum class ScreeningErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : ErrorCode {
    EMPTY_SCREENING_ANSWER(400, "EMPTY_SCREENING_ANSWER", "자세를 하나 이상 골라야 합니다"),
    TOO_MANY_SCREENING_ANSWERS(400, "TOO_MANY_SCREENING_ANSWERS", "쉬웠던 자세와 어려웠던 자세는 각각 4 개까지 고를 수 있습니다"),
    DUPLICATE_SCREENING_ANSWER(400, "DUPLICATE_SCREENING_ANSWER", "같은 자세를 두 번 고를 수 없습니다"),
    BODY_PART_NOT_FOUND(404, "BODY_PART_NOT_FOUND", "존재하지 않는 부위입니다"),
    SCREENING_RESULT_NOT_FOUND(404, "SCREENING_RESULT_NOT_FOUND", "진단 결과가 없습니다"),

    /**
     * 응답이 어떤 분기 규칙에도 걸리지 않아 원인을 하나도 못 냈다.
     *
     * 회원 잘못이 아니라 seed 가 그 자세 조합을 덮지 못한 것이므로 400 이 아니다. 빈 결과를
     * 저장해 두면 "원인 0 개인 진단" 이 남아 course 가 처방할 것을 못 찾는다.
     */
    CAUSE_NOT_DETERMINED(422, "CAUSE_NOT_DETERMINED", "고른 자세로는 원인을 판별할 수 없습니다"),
}
