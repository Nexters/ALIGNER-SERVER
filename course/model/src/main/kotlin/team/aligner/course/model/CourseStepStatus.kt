package team.aligner.course.model

/**
 * 스텝 완료 여부.
 *
 * **진행 중(IN_PROGRESS) 상태를 두지 않는다.** 세션이 스텝 단위로 끝나고 완료만 기록되므로
 * "하다 만 스텝" 을 표현할 자리가 없다. 필요해지면 그때 값을 늘린다
 * (docs/architecture.md §3 "미리 만들지 않는다").
 */
enum class CourseStepStatus {
    NOT_STARTED,
    COMPLETED,
}
