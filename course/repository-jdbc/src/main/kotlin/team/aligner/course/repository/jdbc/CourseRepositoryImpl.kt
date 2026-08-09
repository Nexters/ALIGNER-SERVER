package team.aligner.course.repository.jdbc

import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.model.Course
import team.aligner.course.model.CourseIdentity
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.CourseStep
import team.aligner.course.model.CourseStepExercise
import team.aligner.course.model.CourseStepStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Entity ↔ Model 변환이 일어나는 유일한 자리다. 도메인은 Entity 를 모른다.
 *
 * 시각은 @EnableJdbcAuditing 대신 여기서 명시적으로 채운다. auditing 은 전역 설정이라
 * 도메인 모듈의 AutoConfiguration 에서 켜면 다른 도메인에 조용히 영향이 간다.
 */
internal class CourseRepositoryImpl(
    private val courseJdbcRepository: CourseJdbcRepository,
) : CourseRepository {
    override fun save(course: Course): Course {
        // PostgreSQL TIMESTAMPTZ 는 마이크로초까지만 담는다. Instant.now() 를 그대로 쓰면
        // 나노초가 저장 시 잘려 save() 가 돌려준 모델이 DB 값과 달라진다. macOS 시계는
        // 마이크로초 단위라 로컬에서는 드러나지 않고 Linux CI 에서만 깨진다.
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val saved = courseJdbcRepository.save(course.toEntity(now))
        return saved.toModel()
    }

    override fun findByIdentity(courseIdentity: CourseIdentity): Course? =
        courseJdbcRepository.findById(courseIdentity.value).orElse(null)?.toModel()

    override fun findByMemberIdAndTargetPoseId(
        memberId: Long,
        targetPoseId: Long,
    ): Course? = courseJdbcRepository.findByMemberIdAndTargetPoseId(memberId, targetPoseId)?.toModel()
}

private fun Course.toEntity(now: Instant): CourseEntity =
    CourseEntity(
        courseId = identity?.value,
        memberId = memberId,
        templateId = templateId,
        targetPoseId = targetPoseId,
        causeCode = causeCode,
        status = status.name,
        createdAt = createdAt ?: now,
        completedAt = completedAt?.truncatedTo(ChronoUnit.MICROS),
        steps =
            steps
                .map { step ->
                    CourseStepEntity(
                        courseStepId = step.identity,
                        stepOrder = step.stepOrder,
                        status = step.status.name,
                        completedAt = step.completedAt?.truncatedTo(ChronoUnit.MICROS),
                        exercises =
                            step.exercises
                                .map {
                                    CourseStepExerciseEntity(
                                        courseStepExerciseId = it.identity,
                                        exerciseId = it.exerciseId,
                                        displayOrder = it.displayOrder,
                                        durationSeconds = it.durationSeconds,
                                        setCount = it.setCount,
                                    )
                                }.toSet(),
                    )
                }.toSet(),
    )

private fun CourseEntity.toModel(): Course =
    Course(
        identity = courseId?.let { CourseIdentity.of(it) },
        memberId = memberId,
        templateId = templateId,
        targetPoseId = targetPoseId,
        causeCode = causeCode,
        // DDL 의 CHECK 이 값 집합을 강제하므로 valueOf 가 실패하면 스키마가 어긋난 것이다.
        status = CourseStatus.valueOf(status),
        // Set 이라 순서가 보존되지 않는다. 순서는 저장 순서가 아니라 값이 정한다
        // (screening 의 causes 와 같은 판단).
        steps =
            steps
                .map { step ->
                    CourseStep(
                        identity = step.courseStepId,
                        stepOrder = step.stepOrder,
                        status = CourseStepStatus.valueOf(step.status),
                        completedAt = step.completedAt,
                        exercises =
                            step.exercises
                                .map { exercise ->
                                    CourseStepExercise(
                                        identity = exercise.courseStepExerciseId,
                                        exerciseId = exercise.exerciseId,
                                        displayOrder = exercise.displayOrder,
                                        durationSeconds = exercise.durationSeconds,
                                        setCount = exercise.setCount,
                                    )
                                }.sortedBy { it.displayOrder },
                    )
                }.sortedBy { it.stepOrder },
        createdAt = createdAt,
        completedAt = completedAt,
    )
