package team.aligner.training.service

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.annotation.Transactional
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepExerciseLookup
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.ExerciseDetailLookup
import team.aligner.training.infrastructure.ExerciseDetailPort
import team.aligner.training.infrastructure.PerformedExerciseLookup
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import team.aligner.training.infrastructure.SessionRepository
import team.aligner.training.model.ExerciseResult
import team.aligner.training.model.PerceivedResult
import team.aligner.training.model.Session
import team.aligner.training.model.SessionIdentity
import team.aligner.training.model.StepExercise
import team.aligner.training.model.exception.CourseStepNotFoundException
import team.aligner.training.model.exception.SessionNotFoundException
import team.aligner.training.model.view.AchievementDayView
import team.aligner.training.model.view.AchievementView
import team.aligner.training.model.view.CourseProgressView
import team.aligner.training.model.view.SessionExerciseRecordView
import team.aligner.training.model.view.SessionView
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    fun recordPerceivedResult(
        memberId: Long,
        sessionId: Long,
        perceivedResult: PerceivedResult,
    ): SessionView

    fun getAchievement(memberId: Long): AchievementView
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
    private val sessionAchievementQueryRepository: SessionAchievementQueryRepository,
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
        // 소유권 조건을 course 가 걸도록 memberId 를 넘긴다. 남의 코스면 없는 코스와 같은
        // 404 다 — 구분해서 알려주면 존재 여부가 새어나간다.
        val step =
            courseStepPort.findStep(memberId, command.courseId, command.stepOrder)
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
     *
     * **push 는 재시도에서도 한다.** course 계약이 재호출을 멱등하게 흡수하고, 그것이 진행도
     * 반영의 유일한 경로다 — 여기서 건너뛰면 첫 요청이 실패한 경우 진행도가 영구히 안 오른다.
     */
    override fun complete(
        memberId: Long,
        sessionId: Long,
        command: CompleteSessionCommand,
    ): SessionView {
        val (saved, retry) = saveCompleted(memberId, sessionId, command)

        val progress =
            courseProgressPort.completeSession(
                memberId = memberId,
                courseId = saved.courseId,
                stepOrder = saved.stepOrder,
                // 실제로 수행한 것만 넘긴다. 수행하지 않은 운동을 담으면 0 분으로 계산되어
                // "운동량 없음" 과 "안 했음" 이 뭉개진다.
                performedExercises =
                    saved.records
                        .filter { it.completed }
                        .map {
                            PerformedExerciseLookup(
                                courseStepExerciseId = it.courseStepExerciseId,
                                performedDurationSeconds = it.performedDurationSeconds,
                            )
                        },
            )

        // **칼로리는 course 가 계산해 준 값을 받아 저장한다** (docs/domains.md §2, §4-3).
        // 리포트를 새로고침해도 같은 값이 나와야 하는데, 조회할 때마다 다시 계산하면 그 사이
        // 몸무게가 바뀐 회원의 지난 기록이 흔들린다.
        //
        // **최초 완료에서만 담는다.** 재시도에도 push 는 그대로 하지만(course 가 멱등하게
        // 흡수한다) 그 결과는 버린다 — 최초 계산이 null 이었던 세션에 나중 값이 얹히면,
        // 그 사이 몸무게를 입력한 회원의 지난 리포트가 "그날 태운 값" 이 아니게 된다.
        val reported =
            if (retry) saved else sessionRepository.save(saved.withEstimatedKcal(progress.estimatedKcal))

        return reported.toView(
            lookupStepExercises(reported),
            courseProgress =
                CourseProgressView(
                    completedStepCount = progress.completedStepCount,
                    totalStepCount = progress.totalStepCount,
                    courseCompleted = progress.courseCompleted,
                    stampAcquired = progress.stampAcquired,
                    targetPoseId = progress.targetPoseId,
                    targetPoseName = progress.targetPoseName,
                    bodyPartCode = progress.bodyPartCode,
                    level = progress.level,
                    acquiredStampCount = progress.acquiredStampCount,
                    requiredStampCount = progress.requiredStampCount,
                    targetPoseCompleted = progress.targetPoseCompleted,
                ),
        )
    }

    /**
     * 수행 결과를 세션에 적용해 저장한다. 낙관적 락 충돌이면 **다시 읽어 한 번 재시도**한다
     * (course 의 saveCompletedStep 과 같은 형태다).
     *
     * 애그리거트를 통째로 저장하므로, 두 완료 요청이 동시에 같은 `IN_PROGRESS` 세션을 읽으면
     * 나중 저장이 앞선 수행 기록을 덮는다. `version` 이 그것을 실패로 바꾸고, 여기서 최신
     * 상태를 다시 읽어 적용한다. 다시 읽으면 앞선 완료가 보이므로 두 번째 시도는 기록을
     * 덮지 않고 **재시도로 판정된다.**
     *
     * 두 번째도 충돌하면 그대로 올린다 — 계속 미루기보다 클라이언트가 재시도하는 편이 낫다.
     *
     * 두 번째 값이 "재시도인가" 다. **`complete` 전에 봐야 한다** — 완료된 세션은 `complete`
     * 가 같은 인스턴스를 돌려주므로 그 뒤로는 최초 완료와 재시도를 구분할 수 없다.
     */
    private fun saveCompleted(
        memberId: Long,
        sessionId: Long,
        command: CompleteSessionCommand,
    ): Pair<Session, Boolean> =
        try {
            applyCompletion(memberId, sessionId, command)
        } catch (_: OptimisticLockingFailureException) {
            applyCompletion(memberId, sessionId, command)
        }

    private fun applyCompletion(
        memberId: Long,
        sessionId: Long,
        command: CompleteSessionCommand,
    ): Pair<Session, Boolean> {
        val session = findOwned(memberId, sessionId)
        val retry = session.completed

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
        return sessionRepository.save(completed) to retry
    }

    /**
     * 핀포즈 직후 체감을 기록한다. **기록만 하고 판단하지 않는다** (docs/domains.md §2).
     *
     * `TOO_HARD` 를 받아도 코스를 바꾸거나 자세를 내리지 않는다. 어떤 자세로 옮길지가 기획
     * 미확정이라, 화면이 그 값을 보고 부위·난이도 재선택으로 보낸다.
     */
    override fun recordPerceivedResult(
        memberId: Long,
        sessionId: Long,
        perceivedResult: PerceivedResult,
    ): SessionView {
        val saved = sessionRepository.save(findOwned(memberId, sessionId).recordPerceivedResult(perceivedResult))
        return saved.toView(lookupStepExercises(saved), courseProgress = null)
    }

    /**
     * 연속 달성. **저장된 집계가 아니라 완료 기록에서 센다** — 집계 테이블을 두면 세션과
     * 어긋난 상태가 생길 자리가 하나 늘어난다.
     *
     * 날짜는 `Asia/Seoul` 기준이다. 저장은 UTC 지만 "며칠 연속" 은 회원이 사는 날짜로 세야
     * 한다 (`AchievementView`).
     */
    override fun getAchievement(memberId: Long): AchievementView {
        val today = LocalDate.now(ACHIEVEMENT_ZONE)
        val weekStart = today.with(DayOfWeek.MONDAY)

        // 연속 일수는 이번 주보다 더 거슬러 올라갈 수 있다. 상한을 두는 것은 오래된 기록까지
        // 훑을 이유가 없어서다 — 화면이 보여주는 것은 "N 일 연속" 하나뿐이다.
        val from = minOf(weekStart, today.minusDays(MAX_STREAK_DAYS.toLong()))
        val achievedDates = sessionAchievementQueryRepository.findCompletedDates(memberId, from).toSet()

        return AchievementView(
            currentStreakDays = streakDaysOf(achievedDates, today),
            weeklyAchievedCount = (0..<DAYS_PER_WEEK).count { weekStart.plusDays(it.toLong()) in achievedDates },
            days =
                (0..<DAYS_PER_WEEK).map {
                    val date = weekStart.plusDays(it.toLong())
                    AchievementDayView(date = date, achieved = date in achievedDates)
                },
        )
    }

    /**
     * **오늘 아직 안 했어도 끊기지 않는다.** 어제까지 이어져 있으면 그 값을 유지한다 —
     * 하루가 지나기도 전에 0 이 되면 화면이 회원을 잘못 다그친다.
     */
    private fun streakDaysOf(
        achievedDates: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        val start = if (today in achievedDates) today else today.minusDays(1)
        var days = 0
        var cursor = start
        while (cursor in achievedDates && days < MAX_STREAK_DAYS) {
            days++
            cursor = cursor.minusDays(1)
        }
        return days
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
        courseStepPort.findStep(session.memberId, session.courseId, session.stepOrder)?.exercises ?: emptyList()

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
            completedExerciseCount = completedExerciseCount,
            estimatedKcal = estimatedKcal,
            perceivedResult = perceivedResult,
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

    private companion object {
        /** 회원이 사는 날짜로 센다. 밤 늦게 한 운동이 UTC 로는 다음 날이 된다. */
        private val ACHIEVEMENT_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

        private const val DAYS_PER_WEEK = 7

        /**
         * 연속 일수 상한. 화면이 보여주는 것은 "N 일 연속" 하나뿐이라 그 이상 거슬러 올라가도
         * 쓰이지 않는다. 상한이 없으면 오래 쓴 회원일수록 조회가 무거워진다.
         */
        private const val MAX_STREAK_DAYS = 365
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
