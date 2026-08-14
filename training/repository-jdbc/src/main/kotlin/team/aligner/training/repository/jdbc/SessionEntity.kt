package team.aligner.training.repository.jdbc

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * schema 를 명시하지 않으면 public 을 친다 (docs/architecture.md §6).
 *
 * 수행 기록을 @MappedCollection 으로 매단다. 애그리거트 저장이 한 번에 끝나야 "세션은 있는데
 * 기록이 없는" 중간 상태가 생기지 않는다 (§4).
 *
 * status 를 enum 이 아니라 String 으로 둔다. DDL 의 CHECK 이 값 집합을 강제하고 변환은
 * 모델 경계에서 한다.
 */
@Table(schema = "training", name = "session")
internal data class SessionEntity(
    @Id
    val sessionId: Long?,
    val memberId: Long,
    val courseId: Long,
    val stepOrder: Int,
    val status: String,
    val startedAt: Instant,
    val completedAt: Instant?,
    val estimatedKcal: Int?,
    val perceivedResult: String?,
    @MappedCollection(idColumn = "session_id")
    val records: Set<SessionExerciseRecordEntity>,
)

@Table(schema = "training", name = "session_exercise_record")
internal data class SessionExerciseRecordEntity(
    @Id
    val recordId: Long?,
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val displayOrder: Int,
    val completed: Boolean,
    val performedDurationSeconds: Int?,
)
