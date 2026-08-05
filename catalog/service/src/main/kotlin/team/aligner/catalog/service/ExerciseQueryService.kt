package team.aligner.catalog.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.exception.ExerciseNotFoundException
import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseSummaryView

interface ExerciseQueryService {
    fun getDetail(exerciseIdentity: ExerciseIdentity): ExerciseDetailView

    fun getAll(exerciseIdentities: List<ExerciseIdentity>): List<ExerciseSummaryView>
}

/**
 * CommandService 가 없다. catalog 는 감수 seed 만 있는 조회 전용 도메인이라 쓰기 경로가 없다
 * (docs/domains.md §4-3). 이 모듈에 CommandService 가 생기면 잘못 나눈 것이다.
 *
 * `@Transactional` 을 클래스에 붙인다. allopen 플러그인이 열어주는 대상이 클래스라 메서드에만
 * 붙이면 프록시가 걸리지 않는다 (docs/architecture.md §10).
 */
@Transactional(readOnly = true)
internal class ExerciseQueryServiceImpl(
    private val exerciseQueryRepository: ExerciseQueryRepository,
) : ExerciseQueryService {
    override fun getDetail(exerciseIdentity: ExerciseIdentity): ExerciseDetailView =
        exerciseQueryRepository.findDetail(exerciseIdentity)
            ?: throw ExerciseNotFoundException()

    /**
     * 빈 목록이 들어오면 DB 를 치지 않는다. `IN (:ids)` 에 빈 리스트를 넘기면 SQL 이 깨진다.
     *
     * 없는 식별자가 섞여 있어도 예외를 던지지 않는다. 판단은 호출부가 한다.
     */
    override fun getAll(exerciseIdentities: List<ExerciseIdentity>): List<ExerciseSummaryView> {
        if (exerciseIdentities.isEmpty()) {
            return emptyList()
        }
        return exerciseQueryRepository.findAllByIdentities(exerciseIdentities)
    }
}
