package team.aligner.course.model.exception

import team.aligner.support.core.ErrorCode

/**
 * course 도메인 에러.
 *
 * 도메인은 HTTP 응답 포맷을 모른다. 상태 코드만 값으로 들고 있고 변환은 support-web 의
 * GlobalExceptionHandler 가 한다 (docs/architecture.md §9).
 */
enum class CourseErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : ErrorCode {
    COURSE_NOT_FOUND(404, "COURSE_NOT_FOUND", "코스를 찾을 수 없습니다"),
    COURSE_STEP_NOT_FOUND(404, "COURSE_STEP_NOT_FOUND", "코스 스텝을 찾을 수 없습니다"),
    IN_PROGRESS_COURSE_NOT_FOUND(404, "IN_PROGRESS_COURSE_NOT_FOUND", "진행 중인 코스가 없습니다"),

    /**
     * 고른 부위·난이도에 해당하는 목표 자세의 템플릿이 없다.
     *
     * 회원 잘못이 아니라 seed 가 그 조합을 덮지 못한 것이므로 400 이 아니다.
     * screening 의 CAUSE_NOT_DETERMINED 와 같은 성격이다.
     */
    COURSE_TEMPLATE_NOT_FOUND(422, "COURSE_TEMPLATE_NOT_FOUND", "고른 부위와 난이도로는 코스를 만들 수 없습니다"),

    /**
     * 진단하지 않은 회원이 처방을 요청했다. 화면은 이때 온보딩으로 보낸다.
     */
    SCREENING_REQUIRED(409, "SCREENING_REQUIRED", "먼저 자가 스크리닝을 받아야 합니다"),

    /**
     * 회원이 고른 부위가 자기 진단 결과에 없다.
     *
     * 클라이언트가 원인을 들고 오지 않고 서버가 최신 진단으로 검증한다 (docs/domains.md §2).
     * 이 검증이 없으면 원인을 위조해 아무 코스나 받아갈 수 있다.
     */

    EMPTY_COURSE_TEMPLATE(422, "EMPTY_COURSE_TEMPLATE", "코스 템플릿에 스텝이 없습니다"),
}
