package team.aligner.course.model

/**
 * 코스 진행 상태.
 *
 * 자세 도전 현황 화면의 `도전 중` · `완성` 이 이 둘이다. 감수 데이터가 아니라 닫힌 구조
 * 어휘이므로 코드에 둔다 (catalog 의 MuscleRole 과 같은 판단).
 *
 * DDL 의 `ck_course_status` 가 같은 값 집합을 강제한다.
 */
enum class CourseStatus {
    /** 도전 중. 아직 완료하지 않은 스텝이 있다. */
    IN_PROGRESS,

    /** 완성. 모든 스텝을 완료했고 도장이 붙었다. */
    COMPLETED,
}
