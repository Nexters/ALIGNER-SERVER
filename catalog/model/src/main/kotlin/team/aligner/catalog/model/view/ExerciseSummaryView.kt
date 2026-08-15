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
 * category 는 코스 개요가 스텝마다 운동 이름 아래에 그리는 분류다. course 가 소비하게 되어
 * 요약에도 싣는다 — 상세 조회를 스텝 수만큼 다시 하지 않기 위해서다.
 *
 * imageAssetKey 도 같은 이유로 싣는다. 코스 순서 카드가 스텝마다 그림을 그린다.
 * **videoUrl 은 싣지 않는다** — 목록에서 재생하는 화면이 없다.
 *
 * thumbnailUrl 은 운영 목록이 쓴다. imageAssetKey 는 URL 이 아니라 프론트가 정적 파일로 매핑하는
 * 키라, 감수자가 브라우저에서 목록을 열었을 때 그림을 볼 수 있는 값은 이쪽뿐이다.
 * **videoUrl 과 달리 외부 호출이 없다** — seed 로 저장해 둔 값이라 목록에서 행 수만큼 읽어도
 * YMove 를 치지 않는다 (docs/domains.md §4-3-1).
 */
data class ExerciseSummaryView(
    val exerciseId: Long,
    val name: String,
    val imageAssetKey: String?,
    val thumbnailUrl: String?,
    val defaultSetCount: Int?,
    val defaultRepCount: Int?,
    val defaultDurationSeconds: Int?,
    val metValue: BigDecimal?,
    val difficulty: String?,
    val category: String?,
)
