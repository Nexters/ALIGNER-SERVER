package team.aligner.catalog.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.infrastructure.PoseVideoPort
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.exception.ExerciseNotFoundException
import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseSummaryView

interface ExerciseQueryService {
    fun getDetail(exerciseIdentity: ExerciseIdentity): ExerciseDetailView

    fun getAll(exerciseIdentities: List<ExerciseIdentity>): List<ExerciseSummaryView>

    /**
     * 운영 목록용 전체 조회.
     *
     * **`getAll(emptyList())` 와 뜻이 다르다.** 인자를 받는 쪽은 "지정한 것만" 이라 빈 목록이면
     * 빈 결과이고, 이쪽은 "적재된 전부" 다.
     */
    fun getAll(): List<ExerciseSummaryView>
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
    private val poseVideoPort: PoseVideoPort,
) : ExerciseQueryService {
    /**
     * 재생 URL 은 여기서만 채운다. `getAll`(코스 스텝 목록)에는 붙이지 않는다 — 목록 조회에서
     * YMove 를 스텝 수만큼 치면 안 되고(docs/domains.md §4-3-1), `ExerciseSummaryView` 에
     * `videoUrl` 이 아예 없다.
     *
     * slug 가 없으면(seed 미적재) port 를 부르지 않는다. 조회가 실패해도 예외가 아니라
     * `videoUrl = null` 이다 — 그 화면은 이미 프론트 계약에 있다.
     */
    override fun getDetail(exerciseIdentity: ExerciseIdentity): ExerciseDetailView {
        val detail =
            exerciseQueryRepository.findDetail(exerciseIdentity)
                ?: throw ExerciseNotFoundException()

        val slug =
            exerciseQueryRepository.findYmoveSlugs(listOf(exerciseIdentity))[exerciseIdentity.value]
                ?: return detail
        val playback =
            poseVideoPort.findPlayback(listOf(slug))[slug]
                ?: return detail

        return detail.copy(videoUrl = playback.videoUrl)
    }

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

    /**
     * 재생 URL 을 붙이지 않는다. 목록에서 YMove 를 행 수만큼 치면 안 되고, 요약 뷰에
     * `videoUrl` 이 아예 없다 — `getDetail` 과 같은 판단이다.
     */
    override fun getAll(): List<ExerciseSummaryView> = exerciseQueryRepository.findAll()
}
