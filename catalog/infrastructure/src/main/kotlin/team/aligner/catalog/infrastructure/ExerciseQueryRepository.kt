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
}
