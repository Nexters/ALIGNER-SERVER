package team.aligner.course.repository.jdbc

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * schema 를 명시하지 않으면 public 을 친다 (docs/architecture.md §6).
 *
 * courseId 가 null 이면 Spring Data JDBC 가 insert 로 판단한다.
 *
 * 스텝과 그 아래 운동을 @MappedCollection 으로 매단다. 애그리거트 저장이 한 번에 끝나야
 * "코스는 있는데 스텝이 없는" 중간 상태가 생기지 않는다 (docs/architecture.md §4).
 *
 * **keyColumn 을 쓰지 않고 순서를 명시 필드로 둔다.** keyColumn 은 List 인덱스를 순서 컬럼에
 * 넣는데 인덱스가 0 부터라 DDL 의 `CHECK (step_order > 0)` 에 걸린다. screening 의
 * ScreeningAnswerEntity 와 같은 형태다 — Set 으로 담고 순서는 값이 정한다.
 *
 * status 를 enum 이 아니라 String 으로 둔다. DDL 의 CHECK 이 값 집합을 강제하고 변환은
 * 모델 경계에서 한다 (screening 의 perceivedDifficulty 와 같은 형태).
 */
@Table(schema = "course", name = "course")
internal data class CourseEntity(
    @Id
    val courseId: Long?,
    val memberId: Long,
    val templateId: Long,
    val targetPoseId: Long,
    val causeCode: String?,
    val status: String,
    val createdAt: Instant,
    val completedAt: Instant?,
    /** 몇 번째 도전인가. 완주한 코스를 다시 시작하면 하나 오른다. */
    val attemptNo: Int,
    /**
     * 낙관적 락. **@Version 이 붙으면 Spring Data JDBC 가 "새 행인가" 를 식별자가 아니라 이
     * 값으로 판단한다** — null 이면 insert, 값이 있으면 version 을 조건에 넣은 update 다.
     *
     * 두 세션 완료가 동시에 들어와 나중 저장이 앞선 완료를 덮는 것을 막는다. 충돌하면
     * OptimisticLockingFailureException 이고 서비스가 다시 읽어 재시도한다.
     */
    @Version
    val version: Long?,
    @MappedCollection(idColumn = "course_id")
    val steps: Set<CourseStepEntity>,
)

@Table(schema = "course", name = "course_step")
internal data class CourseStepEntity(
    @Id
    val courseStepId: Long?,
    val stepOrder: Int,
    val status: String,
    val completedAt: Instant?,
    @MappedCollection(idColumn = "course_step_id")
    val exercises: Set<CourseStepExerciseEntity>,
)

@Table(schema = "course", name = "course_step_exercise")
internal data class CourseStepExerciseEntity(
    @Id
    val courseStepExerciseId: Long?,
    val exerciseId: Long,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
)

@Table(schema = "course", name = "stamp")
internal data class StampEntity(
    @Id
    val stampId: Long?,
    val memberId: Long,
    val targetPoseId: Long,
    val courseId: Long,
    val attemptNo: Int,
    val acquiredAt: Instant,
)
