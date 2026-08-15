package team.aligner.screening.model.view

/**
 * 온보딩 첫 화면의 부위 선택지. `JdbcClient` 가 직접 채우는 조회 모델이다
 * (docs/architecture.md §4).
 */
data class BodyPartView(
    val bodyPartCode: String,
    val name: String,
)
