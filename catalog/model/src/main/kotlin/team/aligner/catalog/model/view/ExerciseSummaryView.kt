package team.aligner.catalog.model.view

import java.math.BigDecimal

/**
 * 코스 스텝 목록에 쓰는 운동 요약.
 *
 * 근육과 음성 큐를 싣지 않는다. 홈·코스개요는 스텝을 개수만큼 그리므로, 요약에 자식을 실으면
 * 목록 1 회 조회에 조인이 스텝 수만큼 붙고 응답도 그만큼 커진다 (docs/domains.md §4-3-1).
 *
 * default* 는 course.course_step_exercise 가 비어 있을 때 쓰는 기본값이다 (§4-4).
 *
 * category 를 싣지 않는다. 코스 개요가 스텝마다 분류를 그리지만 그것을 소비할 course 가
 * 아직 없다. 계약 형태가 정해지는 course adapter 구현 시점에 함께 넣는다
 * (docs/architecture.md §3 "미리 만들지 않는다", §7 "계약은 좁게").
 */
data class ExerciseSummaryView(
    val exerciseId: Long,
    val name: String,
    val defaultSetCount: Int?,
    val defaultRepCount: Int?,
    val defaultDurationSeconds: Int?,
    val metValue: BigDecimal?,
    val difficulty: String?,
)
