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
 */
internal object MockFixtures {
    const val MEMBER_ID = 1L

    /** 진행 중인 코스. GET /courses/today 가 이것을 돌려준다. */
    const val IN_PROGRESS_COURSE_ID = 20L

    /** 완성한 코스. 도장이 붙은 상태다. */
    const val COMPLETED_COURSE_ID = 21L

    const val SESSION_ID = 100L

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

    /** 코스 20 의 스텝 구성. 운동 6 개, 그중 1 개 완료 — 화면의 진행 중 상태다. */
    val COURSE_STEPS = (1..6).map { order -> order to EXERCISES[order - 1] }

    const val COMPLETED_STEP_COUNT = 1
    val TOTAL_STEP_COUNT = COURSE_STEPS.size
    val TOTAL_DURATION_SECONDS = COURSE_STEPS.sumOf { it.second.durationSeconds }
    val TOTAL_SET_COUNT = COURSE_STEPS.sumOf { it.second.setCount }
    val TOTAL_KCAL = COURSE_STEPS.sumOf { it.second.kcal }
}
