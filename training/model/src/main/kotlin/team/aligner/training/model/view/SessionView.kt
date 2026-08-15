package team.aligner.training.model.view

import team.aligner.training.model.PerceivedResult
import team.aligner.training.model.SessionStatus
import java.time.Instant

/**
 * 세션 플레이어 화면 하나를 위한 읽기 모델.
 *
 * 시작 응답과 조회 응답이 **같은 형태**다. 화면이 두 경로에서 같은 것을 그리므로 형태가
 * 갈리면 복구 흐름에서만 드러나는 차이가 생긴다.
 *
 * 운동 이름·기본값은 catalog 소유라 조회 시점에 port 로 붙인다 (docs/domains.md §6).
 *
 * `courseProgress` 는 **완료 응답에만 실린다.** 세션이 코스 진행도를 얼마나 올렸는지는
 * 완료 직후에만 화면이 쓴다.
 */
data class SessionView(
    val sessionId: Long,
    val courseId: Long,
    val stepOrder: Int,
    val status: SessionStatus,
    val startedAt: Instant,
    val completedAt: Instant?,
    /** 완료 리포트의 "완료 동작 N 개". 화면이 기록을 세지 않게 서버가 정리해서 내린다. */
    val completedExerciseCount: Int,
    /** 이 세션의 소모 칼로리. course 가 계산해 준 값이다. 계산이 안 되면 null 이다. */
    val estimatedKcal: Int?,
    /** 핀포즈 직후 체감. 아직 답하지 않았으면 null 이다. */
    val perceivedResult: PerceivedResult?,
    val exerciseRecords: List<SessionExerciseRecordView>,
    val courseProgress: CourseProgressView?,
)

/**
 * `durationSeconds` `setCount` 는 **해석이 끝난 값**이다. 코스에 override 가 있으면 그 값이고
 * 없으면 catalog 기본값이다 — 화면이 두 곳을 보고 고르지 않게 서버가 정리해서 내린다.
 */
data class SessionExerciseRecordView(
    val courseStepExerciseId: Long,
    val exerciseId: Long,
    val name: String,
    val category: String?,
    val displayOrder: Int,
    val durationSeconds: Int?,
    val setCount: Int?,
    val completed: Boolean,
    val performedDurationSeconds: Int?,
)

/**
 * 세션 완료가 코스에 반영된 결과. **training 이 계산하지 않는다** — course 가 판단해 돌려준
 * 값을 그대로 싣는다 (docs/domains.md §2).
 *
 * 완료 리포트의 헤더(`골반 난이도 상 · 낙타자세`)와 파이어로그 카드가 이 값으로 그려진다.
 */
data class CourseProgressView(
    val completedStepCount: Int,
    val totalStepCount: Int,
    /** 이번 회차의 모든 스텝을 끝냈는지. **자세 완성과 다르다** — 완성은 4 회 완주다. */
    val courseCompleted: Boolean,
    /** 이 호출로 이번 회차의 도장이 새로 붙었는지. 재시도에서는 false 다. */
    val stampAcquired: Boolean,
    val targetPoseId: Long,
    val targetPoseName: String,
    val bodyPartCode: String?,
    val level: Int?,
    /** 이 자세를 지금까지 완주한 횟수. 리포트의 "파이어로그 N / 4회" 의 N 이다. */
    val acquiredStampCount: Int,
    val requiredStampCount: Int,
    /** 도장을 다 채웠는지. 자세 완성 축하 화면의 신호다. */
    val targetPoseCompleted: Boolean,
)
