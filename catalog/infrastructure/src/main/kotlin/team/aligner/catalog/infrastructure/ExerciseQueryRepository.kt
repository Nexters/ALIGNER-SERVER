package team.aligner.catalog.infrastructure

import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseSummaryView

/**
 * 읽기 out-port. 화면 하나에 대응하는 조회만 둔다 (docs/architecture.md §4).
 *
 * **쓰기 port 를 두지 않는다.** catalog 는 감수 전 마스터 데이터라 쓰기가 없고, 값은 전부
 * Liquibase seed changeset 으로 들어간다 (docs/domains.md §4-3). 여기에 save 가 생기면
 * 잘못 나눈 것이다.
 */
interface ExerciseQueryRepository {
    /** 운동 가이드 화면. 근육과 음성 큐를 함께 싣는다. */
    fun findDetail(exerciseIdentity: ExerciseIdentity): ExerciseDetailView?

    /**
     * 코스 스텝 구성에 쓰는 일괄 조회. ExerciseContract 가 이것만 쓴다.
     *
     * 존재하지 않는 식별자가 섞여 들어와도 예외를 던지지 않고 찾은 것만 돌려준다.
     * 도메인 간 FK 가 없어 course 의 seed 가 앞서갈 수 있기 때문이다 (docs/domains.md §6).
     */
    fun findAllByIdentities(exerciseIdentities: List<ExerciseIdentity>): List<ExerciseSummaryView>

    /**
     * 재생 URL 을 얻기 위한 YMove 연결 고리 조회 (docs/domains.md §4-3-1).
     *
     * `ExerciseDetailView` 에 `ymoveSlug` 를 싣지 않기로 했으므로 — 외부 시스템 식별자가 화면
     * 계층까지 새어 나갈 이유가 없다 — 별도 조회다. PK 조회가 한 번 더 붙지만, 뷰에 외부
     * 식별자를 실어 api 계층까지 노출하는 것보다 싸다.
     *
     * `PoseVideoPort.findPlayback` 이 리스트를 받으므로 여기도 배치 형태로 둔다.
     *
     * **`ymove_slug` 가 NULL 인 운동은 맵에서 빠진다.** seed 가 아직 slug 를 안 채운 지금이
     * 곧 그 경로다 — 빈 맵 → port 를 부르지 않음 → `videoUrl = null`.
     */
    fun findYmoveSlugs(exerciseIdentities: List<ExerciseIdentity>): Map<Long, String>
}
