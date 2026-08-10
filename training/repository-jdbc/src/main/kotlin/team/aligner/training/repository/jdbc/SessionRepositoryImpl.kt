package team.aligner.training.repository.jdbc

import team.aligner.training.infrastructure.SessionRepository
import team.aligner.training.model.Session
import team.aligner.training.model.SessionExerciseRecord
import team.aligner.training.model.SessionIdentity
import team.aligner.training.model.SessionStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Entity ↔ Model 변환이 일어나는 유일한 자리다. 도메인은 Entity 를 모른다.
 *
 * 시각은 @EnableJdbcAuditing 대신 여기서 명시적으로 채운다. auditing 은 전역 설정이라
 * 도메인 모듈의 AutoConfiguration 에서 켜면 다른 도메인에 조용히 영향이 간다.
 */
internal class SessionRepositoryImpl(
    private val sessionJdbcRepository: SessionJdbcRepository,
) : SessionRepository {
    override fun save(session: Session): Session {
        // PostgreSQL TIMESTAMPTZ 는 마이크로초까지만 담는다. 나노초를 그대로 넣으면 save() 가
        // 돌려준 모델이 DB 값과 달라진다. macOS 에서는 드러나지 않고 Linux CI 에서만 깨진다.
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val saved =
            sessionJdbcRepository.save(
                SessionEntity(
                    sessionId = session.identity?.value,
                    memberId = session.memberId,
                    courseId = session.courseId,
                    stepOrder = session.stepOrder,
                    status = session.status.name,
                    startedAt = session.startedAt ?: now,
                    completedAt = session.completedAt?.truncatedTo(ChronoUnit.MICROS),
                    records =
                        session.records
                            .map {
                                SessionExerciseRecordEntity(
                                    recordId = it.identity,
                                    courseStepExerciseId = it.courseStepExerciseId,
                                    exerciseId = it.exerciseId,
                                    displayOrder = it.displayOrder,
                                    completed = it.completed,
                                    performedDurationSeconds = it.performedDurationSeconds,
                                )
                            }.toSet(),
                ),
            )
        return saved.toModel()
    }

    override fun findByIdentity(sessionIdentity: SessionIdentity): Session? =
        sessionJdbcRepository.findById(sessionIdentity.value).orElse(null)?.toModel()
}

private fun SessionEntity.toModel(): Session =
    Session(
        identity = sessionId?.let { SessionIdentity.of(it) },
        memberId = memberId,
        courseId = courseId,
        stepOrder = stepOrder,
        // DDL 의 CHECK 이 값 집합을 강제하므로 valueOf 가 실패하면 스키마가 어긋난 것이다.
        status = SessionStatus.valueOf(status),
        // Set 이라 순서가 보존되지 않는다. 순서는 저장 순서가 아니라 값이 정한다.
        records =
            records
                .map {
                    SessionExerciseRecord(
                        identity = it.recordId,
                        courseStepExerciseId = it.courseStepExerciseId,
                        exerciseId = it.exerciseId,
                        displayOrder = it.displayOrder,
                        completed = it.completed,
                        performedDurationSeconds = it.performedDurationSeconds,
                    )
                }.sortedBy { it.displayOrder },
        startedAt = startedAt,
        completedAt = completedAt,
    )
