package team.aligner.training.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepExerciseLookup
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.ExerciseDetailLookup
import team.aligner.training.infrastructure.ExerciseDetailPort
import team.aligner.training.infrastructure.SessionRepository
import team.aligner.training.model.ExerciseResult
import team.aligner.training.model.Session
import team.aligner.training.model.SessionIdentity
import team.aligner.training.model.StepExercise
import team.aligner.training.model.exception.CourseStepNotFoundException
import team.aligner.training.model.exception.SessionNotFoundException
import team.aligner.training.model.view.CourseProgressView
import team.aligner.training.model.view.SessionExerciseRecordView
import team.aligner.training.model.view.SessionView
import java.time.Instant

interface SessionService {
    fun start(
        memberId: Long,
        command: StartSessionCommand,
    ): SessionView

    fun getSession(
        memberId: Long,
        sessionId: Long,
    ): SessionView

    fun complete(
        memberId: Long,
        sessionId: Long,
        command: CompleteSessionCommand,
    ): SessionView
}

/**
 * Command 와 Query 를 한 서비스에 둔다. 세션은 시작·조회·완료가 같은 애그리거트를 중심으로
 * 돌고, 조회가 애그리거트 밖의 조회 모델을 필요로 하지 않는다 — 화면에 붙는 catalog 값은
 * port 로 받아 조립한다.
 *
 * `@Transactional` 은 **클래스에** 붙인다. kotlin-spring(allopen)이 클래스에 붙은 어노테이션만
 * 보고 open 을 매기기 때문이다. 메서드에만 붙이면 클래스가 final 로 남고 CGLIB 프록시 생성이
 * 실패해 기동이 죽는다.
 */
@Transactional
internal class SessionServiceImpl(
    private val sessionRepository: SessionRepository,
    private val courseStepPort: CourseStepPort,
    private val courseProgressPort: CourseProgressPort,
    private val exerciseDetailPort: ExerciseDetailPort,
) : SessionService {
    /**
     * 코스 스텝 구성을 복사해 세션을 연다.
     *
     * **이미 완료한 스텝으로도 세션을 열 수 있다.** 회원이 같은 스텝을 다시 수행하는 것을
     * 막을 이유가 없고, 완료 push 는 course 가 멱등하게 흡수한다.
     */
    override fun start(
        memberId: Long,
        command: StartSessionCommand,
    ): SessionView {
        val step =
            courseStepPort.findStep(command.courseId, command.stepOrder)
                ?: throw CourseStepNotFoundException()

        val saved =
            sessionRepository.save(
                Session.start(
                    memberId = memberId,
                    courseId = command.courseId,
                    stepOrder = command.stepOrder,
                    exercises =
                        step.exercises.map {
                            StepExercise(
                                courseStepExerciseId = it.courseStepExerciseId,
                                exerciseId = it.exerciseId,
                                displayOrder = it.displayOrder,
                            )
                        },
                ),
            )
        return saved.toView(step.exercises, courseProgress = null)
    }

    override fun getSession(
        memberId: Long,
        sessionId: Long,
    ): SessionView {
        val session = findOwned(memberId, sessionId)
        return session.toView(lookupStepExercises(session), courseProgress = null)
    }

    /**
     * 수행 결과를 저장하고 **코스에 push** 한다.
     *
     * 진행도·도장 판단을 여기서 하지 않는다. course 가 판단해 돌려준 값을 그대로 실어 보낼
     * 뿐이다 — 그 로직이 training 에 생기면 잘못 나눈 것이다 (docs/domains.md §2, §4-5).
     *
     * **멱등하다.** 이미 완료된 세션이면 기록을 덮어쓰지 않고, push 는 다시 하되 course 가
     * 흡수해 진행도가 두 번 오르지 않는다. 재시도로 들어온 호출에서는 `stampAcquired` 가
     * false 로 돌아온다.
     */
    override fun complete(
        memberId: Long,
        sessionId: Long,
        command: CompleteSessionCommand,
    ): SessionView {
        val session = findOwned(memberId, sessionId)

        val completed =
            session.complete(
                results =
                    command.exerciseRecords.map {
                        ExerciseResult(
                            courseStepExerciseId = it.courseStepExerciseId,
                            completed = it.completed,
                            performedDurationSeconds = it.performedDurationSeconds,
                        )
                    },
                at = Instant.now(),
            )
        // 이미 완료된 세션이면 complete 가 같은 인스턴스를 돌려준다. 그래도 저장하는 것은
        // 분기를 하나 줄이기 위해서이고, 값이 같으므로 덮어써도 달라지는 것이 없다.
        val saved = sessionRepository.save(completed)

        val progress =
            courseProgressPort.completeSession(
                memberId = memberId,
                courseId = saved.courseId,
                stepOrder = saved.stepOrder,
            )

        return saved.toView(
            lookupStepExercises(saved),
            courseProgress =
                CourseProgressView(
                    completedStepCount = progress.completedStepCount,
                    totalStepCount = progress.totalStepCount,
                    courseCompleted = progress.courseCompleted,
                    stampAcquired = progress.stampAcquired,
                ),
        )
    }

    /**
     * 남의 세션과 없는 세션을 같은 404 로 돌려준다. 구분해서 알려주면 존재 여부가 새어나간다
     * (다른 도메인과 같은 판단).
     */
    private fun findOwned(
        memberId: Long,
        sessionId: Long,
    ): Session =
        sessionRepository
            .findByIdentity(SessionIdentity.of(sessionId))
            ?.takeIf { it.memberId == memberId }
            ?: throw SessionNotFoundException()

    /**
     * 세션이 수행한 스텝의 코스 구성을 다시 읽는다. `durationSeconds` 같은 override 가
     * 세션에 복사돼 있지 않기 때문이다 — 세션은 "무엇을 수행했나" 만 들고 있다.
     *
     * 코스 스텝이 사라졌으면 빈 목록이다. 세션 기록 자체는 남아야 하므로 조회를 실패시키지
     * 않는다.
     */
    private fun lookupStepExercises(session: Session): List<CourseStepExerciseLookup> =
        courseStepPort.findStep(session.courseId, session.stepOrder)?.exercises ?: emptyList()

    /**
     * catalog 값을 붙여 화면 모델로 만든다. 운동마다 부르지 않고 한 번에 읽는다.
     */
    private fun Session.toView(
        stepExercises: List<CourseStepExerciseLookup>,
        courseProgress: CourseProgressView?,
    ): SessionView {
        val overrides = stepExercises.associateBy { it.courseStepExerciseId }
        val details = lookupDetails(records.map { it.exerciseId })

        return SessionView(
            sessionId = checkNotNull(identity) { "저장된 세션에 식별자가 없다" }.value,
            courseId = courseId,
            stepOrder = stepOrder,
            status = status,
            startedAt = checkNotNull(startedAt) { "저장된 세션에 시작 시각이 없다" },
            completedAt = completedAt,
            exerciseRecords =
                records
                    .sortedBy { it.displayOrder }
                    .map { record ->
                        val override = overrides[record.courseStepExerciseId]
                        val detail = details[record.exerciseId]
                        SessionExerciseRecordView(
                            courseStepExerciseId = record.courseStepExerciseId,
                            exerciseId = record.exerciseId,
                            name = detail?.name ?: "",
                            category = detail?.category,
                            displayOrder = record.displayOrder,
                            // 코스 override 가 없으면 catalog 기본값이다.
                            durationSeconds = override?.durationSeconds ?: detail?.defaultDurationSeconds,
                            setCount = override?.setCount ?: detail?.defaultSetCount,
                            completed = record.completed,
                            performedDurationSeconds = record.performedDurationSeconds,
                        )
                    },
            courseProgress = courseProgress,
        )
    }

    private fun lookupDetails(exerciseIds: List<Long>): Map<Long, ExerciseDetailLookup> {
        val ids = exerciseIds.distinct()
        if (ids.isEmpty()) {
            return emptyMap()
        }
        return exerciseDetailPort.findAllByIds(ids).associateBy { it.exerciseId }
    }
}

/**
 * 세션 시작 입력.
 *
 * `memberId` 를 명령에 담지 않는다. api 가 `AlignerPrincipal` 에서 꺼내 **파라미터로** 넘긴다
 * (docs/architecture.md §9).
 */
data class StartSessionCommand(
    val courseId: Long,
    val stepOrder: Int,
)

/**
 * 세션 완료 입력.
 *
 * 요청에 없는 운동은 수행하지 않은 것으로 남는다. 부분 완료가 정상이므로 빠진 것을 오류로
 * 보지 않는다.
 */
data class CompleteSessionCommand(
    val exerciseRecords: List<ExerciseResultCommand>,
)

data class ExerciseResultCommand(
    val courseStepExerciseId: Long,
    val completed: Boolean,
    val performedDurationSeconds: Int?,
)
