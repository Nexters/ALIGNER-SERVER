package team.aligner.screening.repository.jdbc

import team.aligner.screening.infrastructure.ScreeningResultRepository
import team.aligner.screening.model.PerceivedDifficulty
import team.aligner.screening.model.ScreeningAnswer
import team.aligner.screening.model.ScreeningCause
import team.aligner.screening.model.ScreeningResult
import team.aligner.screening.model.ScreeningResultIdentity
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Entity ↔ Model 변환이 일어나는 유일한 자리다. 도메인은 Entity 를 모른다.
 *
 * 시각은 @EnableJdbcAuditing 대신 여기서 명시적으로 채운다. auditing 은 전역 설정이라
 * 도메인 모듈의 AutoConfiguration 에서 켜면 다른 도메인에 조용히 영향이 간다.
 */
internal class ScreeningResultRepositoryImpl(
    private val screeningResultJdbcRepository: ScreeningResultJdbcRepository,
) : ScreeningResultRepository {
    override fun save(screeningResult: ScreeningResult): ScreeningResult {
        // PostgreSQL TIMESTAMPTZ 는 마이크로초까지만 담는다. Instant.now() 를 그대로 쓰면
        // 나노초가 저장 시 잘려 save() 가 돌려준 모델이 DB 값과 달라진다. macOS 시계는
        // 마이크로초 단위라 로컬에서는 드러나지 않고 Linux CI 에서만 깨진다 (member 와 같은 이유).
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val saved =
            screeningResultJdbcRepository.save(
                ScreeningResultEntity(
                    resultId = screeningResult.identity?.value,
                    memberId = screeningResult.memberId,
                    perceivedBodyPartCode = screeningResult.perceivedBodyPartCode,
                    createdAt = screeningResult.createdAt ?: now,
                    answers =
                        screeningResult.answers
                            .map {
                                ScreeningAnswerEntity(
                                    answerId = null,
                                    targetPoseId = it.targetPoseId,
                                    perceivedDifficulty = it.perceivedDifficulty.name,
                                )
                            }.toSet(),
                    causes =
                        screeningResult.causes
                            .map {
                                ScreeningCauseEntity(
                                    screeningCauseId = null,
                                    causeCode = it.causeCode,
                                    rank = it.rank,
                                    score = it.score,
                                )
                            }.toSet(),
                ),
            )
        return saved.toModel()
    }
}

private fun ScreeningResultEntity.toModel(): ScreeningResult =
    ScreeningResult(
        identity = resultId?.let { ScreeningResultIdentity.of(it) },
        memberId = memberId,
        perceivedBodyPartCode = perceivedBodyPartCode,
        answers =
            answers.map {
                ScreeningAnswer(
                    targetPoseId = it.targetPoseId,
                    perceivedDifficulty = PerceivedDifficulty.valueOf(it.perceivedDifficulty),
                )
            },
        // 순위는 저장 순서가 아니라 값이 정한다. Set 이라 순서가 보존되지 않는다.
        causes =
            causes
                .map { ScreeningCause(causeCode = it.causeCode, rank = it.rank, score = it.score) }
                .sortedBy { it.rank },
        createdAt = createdAt,
    )
