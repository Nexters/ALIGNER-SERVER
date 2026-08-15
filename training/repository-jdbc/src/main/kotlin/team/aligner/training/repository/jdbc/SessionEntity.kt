package team.aligner.training.repository.jdbc

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
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
    /**
     * 낙관적 락. **@Version 이 붙으면 Spring Data JDBC 가 "새 행인가" 를 식별자가 아니라 이
     * 값으로 판단한다** — null 이면 insert, 값이 있으면 version 을 조건에 넣은 update 다.
     *
     * 동시에 들어온 두 완료 요청이 서로의 수행 기록을 덮는 것을 막는다. 충돌하면
     * OptimisticLockingFailureException 이고 서비스가 다시 읽어 재시도한다 (CourseEntity 와
     * 같은 형태다).
     */
    @Version
    val version: Long?,
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
