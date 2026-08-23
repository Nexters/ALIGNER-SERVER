package team.aligner.mock

import java.time.Instant

/**
 * 목 응답이 공유하는 고정 값. **한 세트로 일관돼야 한다.**
 *
 * 엔드포인트마다 값을 따로 만들면 프론트가 화면을 이을 수 없다 — 자세 그리드에서 고른
 * 식별자를 제출에 쓰고, 처방받은 코스로 개요를 여는 흐름이 성립해야 한다.
 *
 * **상태를 쌓지 않는다.** 대신 식별자로 상태를 나눈다 — 코스 20 은 진행 중, 21 은 완성이다.
 * 프론트가 두 화면을 모두 만들 수 있다.
 *
 * **값을 컨트롤러에 흩지 않고 여기 모은다.** 같은 코스의 진행도가 화면마다 달라지는 것을
 * 막고, 이 모듈을 지울 때 지울 것이 한 파일에 모여 있게 한다.
 *
 * 이 값들은 `docs/architecture.md` §6 이 말하는 "감수 전 데이터" 가 아니다. 감수를 기다리는
 * 정본 후보가 아니라 **곧 버릴 가짜**이고, seed 가 들어오면 이 모듈 전체가 사라진다.
 */
internal object MockFixtures {
    const val MEMBER_ID = 1L

    /** 진행 중인 코스. GET /courses/today 가 이것을 돌려준다. */
    const val IN_PROGRESS_COURSE_ID = 20L

    /** 완성한 코스. 도장이 붙은 상태다. */
    const val COMPLETED_COURSE_ID = 21L

    const val SESSION_ID = 100L

    /**
     * `courseStepExerciseId` = [COURSE_STEP_EXERCISE_ID_BASE] + `stepOrder` 다.
     *
     * 상태를 저장하지 않으므로 세션 완료 요청에서 **어느 스텝이었는지를 이 규칙으로 역산**한다.
     * 규칙을 상수로 못박아 두 곳(코스 개요·세션)이 같은 값을 쓰게 한다.
     */
    const val COURSE_STEP_EXERCISE_ID_BASE = 50L

    /**
     * 코스별 상태. **모든 코스 API 가 이 한 곳에서 진행도를 가져온다.**
     *
     * 도전 현황과 코스 개요가 각자 값을 들고 있으면 같은 코스의 진행도가 화면마다 달라진다.
     * 여기 없는 식별자는 존재하지 않는 코스이고 404 다.
     */
    val COURSES =
        mapOf(
            IN_PROGRESS_COURSE_ID to CourseState(IN_PROGRESS_COURSE_ID, targetPoseIndex = 0, completedSteps = 1),
            COMPLETED_COURSE_ID to CourseState(COMPLETED_COURSE_ID, targetPoseIndex = 8, completedSteps = 6),
            22L to CourseState(22L, targetPoseIndex = 2, completedSteps = 2),
            23L to CourseState(23L, targetPoseIndex = 3, completedSteps = 4),
        )

    /**
     * 코스 하나의 고정 상태. `completedSteps` 가 전체와 같으면 완성이다.
     */
    internal data class CourseState(
        val courseId: Long,
        val targetPoseIndex: Int,
        val completedSteps: Int,
    ) {
        val completed: Boolean get() = completedSteps >= TOTAL_STEP_COUNT
        val targetPose: TargetPose get() = TARGET_POSES[targetPoseIndex]
    }

    val NOW: Instant = Instant.parse("2026-08-11T12:00:00Z")

    /**
     * 부위 셋. 와이어프레임의 강화 부위 선택 화면과 같다.
     */
    val BODY_PARTS =
        listOf(
            "BACK" to "등",
            "ABDOMEN" to "복부",
            "PELVIS" to "골반",
        )

    /**
     * 핀포즈. 온보딩 그리드가 이것을 그린다.
     *
     * (식별자, 이름, assetKey, 부위, 레벨)
     */
    val TARGET_POSES =
        listOf(
            TargetPose(1L, "낙타자세", "target-pose/camel", "BACK", 1),
            TargetPose(2L, "활자세", "target-pose/bow", "BACK", 2),
            TargetPose(3L, "다운독", "target-pose/downward-dog", "BACK", 3),
            TargetPose(4L, "보트자세", "target-pose/boat", "ABDOMEN", 1),
            TargetPose(5L, "반보트", "target-pose/half-boat", "ABDOMEN", 2),
            TargetPose(6L, "사이드플랭크", "target-pose/side-plank", "ABDOMEN", 3),
            TargetPose(7L, "말라사나", "target-pose/malasana", "PELVIS", 1),
            TargetPose(8L, "파이어로그", "target-pose/fire-log", "PELVIS", 2),
            TargetPose(9L, "캣카우", "target-pose/cat-cow", "PELVIS", 3),
        )

    /**
     * 코스 스텝에 편성되는 운동. 핀포즈도 운동 행을 갖는다 (docs/domains.md §4-3).
     */
    val EXERCISES =
        listOf(
            Exercise(101L, "캣카우", "가동성 웜업", 1, 120, 6),
            Exercise(102L, "보트자세", "가동성 웜업", 3, 120, 6),
            Exercise(103L, "테이블탑 밸런스", "가동성 웜업", 3, 120, 6),
            Exercise(104L, "플랭크 트랜지션", "가동성 웜업", 1, 120, 6),
            Exercise(105L, "브릿지", "가동성 웜업", 2, 120, 6),
            Exercise(106L, "낙타자세", "핀포즈", 3, 120, 39),
        )

    /**
     * 진단 결과. 어떤 자세를 제출해도 같은 원인이 나온다.
     *
     * 와이어프레임의 "등, 골반 근육이 약한 것으로 분석돼요" 화면에 대응한다. 강화 부위
     * 선택 화면에서 고를 수 있는 부위가 이 결과에 들어 있어야 코스 처방이 이어진다.
     */
    val CAUSES =
        listOf(
            Cause("WEAK_BACK", "등 근육 약화", "BACK", "등과 골반 근육이 약한 것으로 분석돼요", 1, 8),
            Cause("WEAK_PELVIS", "골반 불안정", "PELVIS", "골반을 잡아주는 근육이 약해요", 2, 5),
        )

    /** 음성 큐잉 대본. 세션 플레이어가 읽어주는 문장이다. */
    val VOICE_CUES =
        listOf(
            VoiceCue(1, null, null, "무릎을 골반 너비로 벌리고 손은 어깨 아래에 둡니다"),
            VoiceCue(2, 35, 75, "명치를 천장을 향해 높게 끌어올리세요"),
        )

    const val CAUTION_NOTE = "목을 뒤로 완전히 젖히지 마세요. 허리에 날카로운 통증이 오면 즉시 중단하세요."

    internal data class Cause(
        val code: String,
        val name: String,
        val bodyPartCode: String,
        val description: String,
        val rank: Int,
        val score: Int,
    )

    internal data class VoiceCue(
        val displayOrder: Int,
        val startOffsetSeconds: Int?,
        val endOffsetSeconds: Int?,
        val content: String,
    )

    /** 근육. 자세·운동 상세의 근육맵에 쓴다. 앞·뒤 중 한쪽만 보이는 근육이 섞여 있다. */
    val MUSCLES =
        listOf(
            Muscle("ERECTOR_SPINAE", "척추기립근", "BACK", null, "muscle/erector-spinae-back", "STRENGTHEN", 1),
            Muscle("ILIOPSOAS", "장요근", "PELVIS", "muscle/iliopsoas-front", null, "STRETCH", 2),
        )

    internal data class TargetPose(
        val id: Long,
        val name: String,
        val assetKey: String,
        val bodyPartCode: String,
        val level: Int,
    )

    internal data class Exercise(
        val id: Long,
        val name: String,
        val category: String,
        val setCount: Int,
        val durationSeconds: Int,
        val kcal: Int,
    )

    internal data class Muscle(
        val code: String,
        val name: String,
        val bodyPartCode: String,
        val frontAssetKey: String?,
        val backAssetKey: String?,
        val role: String,
        val displayOrder: Int,
    )

    /** 코스 스텝 구성. 모든 코스가 같은 구성을 쓴다 — 목이 코스마다 다를 이유가 없다. */
    val COURSE_STEPS = (1..6).map { order -> order to EXERCISES[order - 1] }

    val TOTAL_STEP_COUNT = COURSE_STEPS.size
    val TOTAL_DURATION_SECONDS = COURSE_STEPS.sumOf { it.second.durationSeconds }
    val TOTAL_SET_COUNT = COURSE_STEPS.sumOf { it.second.setCount }
    val TOTAL_KCAL = COURSE_STEPS.sumOf { it.second.kcal }
}
