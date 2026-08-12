package team.aligner.training.model

import team.aligner.training.model.exception.EmptyCourseStepException
import team.aligner.training.model.exception.UnknownExerciseRecordException
import java.time.Instant

/**
 * 세션 1 회. 애그리거트 루트다.
 *
 * **완수 판정이 여기 없다.** training 은 "무슨 일이 있었나" 만 기록하고 진행도·도장 판단은
 * 전부 course 가 한다 (docs/domains.md §2). 이 클래스에 도장이나 진행도가 생기면 잘못
 * 나눈 것이다.
 *
 * Spring Data JDBC 에는 더티체킹이 없다. [complete] 는 새 인스턴스를 반환하고 호출부가
 * save 를 명시한다 (docs/architecture.md §4).
 */
data class Session(
    val identity: SessionIdentity?,
    val memberId: Long,
    val courseId: Long,
    val stepOrder: Int,
    val status: SessionStatus,
    val records: List<SessionExerciseRecord>,
    val startedAt: Instant?,
    val completedAt: Instant?,
    /**
     * 이 세션의 소모 칼로리. **course 가 계산해 준 값을 담기만 한다** — training 이 계산하면
     * MET·몸무게 port 를 새로 뚫어야 하고, 그 둘을 이미 읽는 쪽이 course 다
     * (docs/domains.md §2, §4-3).
     *
     * 조회할 때마다 다시 계산하지 않고 저장한다. 리포트는 "그날 얼마나 태웠나" 라서 지금
     * 몸무게로 다시 계산하면 지난 기록이 흔들린다.
     */
    val estimatedKcal: Int? = null,
    /** 핀포즈 직후 체감. 아직 답하지 않았으면 null 이다. */
    val perceivedResult: PerceivedResult? = null,
) {
    val completed: Boolean get() = status == SessionStatus.COMPLETED

    /** 완료한 운동 개수. 리포트의 "완료 동작 N 개" 다. */
    val completedExerciseCount: Int get() = records.count { it.completed }

    /**
     * course 가 계산해 준 칼로리를 담는다.
     *
     * **최초 완료에서 한 번만 부른다.** 재시도에서 다시 부르면 최초 계산이 null 이었던 세션에
     * 나중 값이 얹히는데, 그 사이 몸무게를 입력한 회원의 지난 리포트가 "그날 태운 값" 이
     * 아니게 된다. 그 판단은 호출부가 한다 — 여기서는 완료 여부를 알아도 최초인지 재시도인지
     * 구분할 수 없다.
     *
     * 값이 이미 있으면 덮지 않는다. 호출 규칙이 깨져도 저장된 기록은 지키는 두 번째 방어선이다.
     */
    fun withEstimatedKcal(kcal: Int?): Session = if (estimatedKcal != null) this else copy(estimatedKcal = kcal)

    /**
     * 핀포즈 직후 체감을 기록한다.
     *
     * **판단하지 않는다.** `TOO_HARD` 를 받아도 코스를 바꾸거나 자세를 내리지 않는다 —
     * training 은 무슨 일이 있었나만 기록하고(§2), 교체 규칙 자체가 아직 기획 미확정이다.
     * 화면이 그 값을 보고 부위·난이도 재선택으로 보낸다.
     *
     * **다시 답할 수 있다.** 잘못 누른 것을 고치지 못하게 막을 이유가 없다.
     */
    fun recordPerceivedResult(result: PerceivedResult): Session = copy(perceivedResult = result)

    /**
     * 수행 결과를 채우고 세션을 닫는다.
     *
     * **이미 완료된 세션은 그대로 둔다.** 완료 요청이 재시도될 수 있고, 그때 기록을 덮어쓰면
     * 처음 저장한 수행 결과가 사라진다. 진행도 push 는 course 가 멱등하게 흡수한다
     * (docs/domains.md §7-8).
     *
     * 이 세션에 없는 운동이 섞여 오면 막는다. 조용히 무시하면 클라이언트가 잘못 보낸 것을
     * 성공으로 읽는다.
     */
    fun complete(
        results: List<ExerciseResult>,
        at: Instant,
    ): Session {
        if (completed) {
            return this
        }

        val known = records.map { it.courseStepExerciseId }.toSet()
        results.firstOrNull { it.courseStepExerciseId !in known }?.let {
            throw UnknownExerciseRecordException()
        }

        val byId = results.associateBy { it.courseStepExerciseId }
        return copy(
            status = SessionStatus.COMPLETED,
            completedAt = at,
            // 요청에 없는 운동은 수행하지 않은 것으로 남는다. 부분 완료가 정상이라
            // 빠진 것을 오류로 보지 않는다.
            records =
                records.map { record ->
                    byId[record.courseStepExerciseId]?.let {
                        record.copy(
                            completed = it.completed,
                            performedDurationSeconds = it.performedDurationSeconds,
                        )
                    } ?: record
                },
        )
    }

    companion object {
        /**
         * 코스 스텝 구성을 복사해 세션을 연다.
         *
         * **복사하는 이유는 세션 중 코스가 바뀌어도 이 세션이 무엇을 수행했는지가 흔들리면
         * 안 되기 때문**이다.
         *
         * 운동이 없는 스텝으로는 세션을 열지 않는다. 수행할 것이 없는 세션이 남는다.
         */
        fun start(
            memberId: Long,
            courseId: Long,
            stepOrder: Int,
            exercises: List<StepExercise>,
        ): Session {
            if (exercises.isEmpty()) {
                throw EmptyCourseStepException()
            }
            return Session(
                identity = null,
                memberId = memberId,
                courseId = courseId,
                stepOrder = stepOrder,
                status = SessionStatus.IN_PROGRESS,
                records =
                    exercises.map {
                        SessionExerciseRecord(
                            identity = null,
                            courseStepExerciseId = it.courseStepExerciseId,
                            exerciseId = it.exerciseId,
                            displayOrder = it.displayOrder,
                            completed = false,
                            performedDurationSeconds = null,
                        )
                    },
                startedAt = null,
                completedAt = null,
            )
        }
    }
}

/**
 * 세션 안에서 운동 하나를 어떻게 수행했는가.
 *
 * 시작 시점에는 `completed = false` 로 만들어져 있고 완료 요청이 값을 채운다.
 */
data class SessionExerciseRecord(
    val identity: Long?,
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val displayOrder: Int,
    val completed: Boolean,
    val performedDurationSeconds: Int?,
)

/** 세션을 열 때 course 에서 받아오는 스텝 구성. */
data class StepExercise(
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val displayOrder: Int,
)

/** 완료 요청이 실어 보내는 수행 결과 하나. */
data class ExerciseResult(
    val courseStepExerciseId: Long,
    val completed: Boolean,
    val performedDurationSeconds: Int?,
)
