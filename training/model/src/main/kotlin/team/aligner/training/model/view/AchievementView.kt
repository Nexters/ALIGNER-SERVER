package team.aligner.training.model.view

import java.time.LocalDate

/**
 * 운동 완료 리포트의 "5일 연속 달성 중 · 이번 주 5 / 7" 과 요일 체크.
 *
 * **테이블을 새로 만들지 않는다.** 세션 완료 기록이 이미 날짜를 갖고 있어서 그것을 세면
 * 되고, 별도 집계 테이블을 두면 세션과 어긋난 상태가 생길 자리가 하나 늘어난다.
 *
 * 날짜는 **`Asia/Seoul` 기준**이다. 저장은 `TIMESTAMPTZ` 라 UTC 시각이지만, "며칠 연속" 은
 * 회원이 사는 날짜로 세야 한다 — 밤 10 시 운동이 UTC 로는 다음 날이라 그대로 세면 하루가
 * 둘로 갈린다.
 */
data class AchievementView(
    /**
     * 오늘까지 이어진 연속 달성 일수.
     *
     * **오늘 아직 안 했어도 끊기지 않는다.** 어제까지 이어져 있으면 그 값을 유지한다 —
     * 하루가 지나기도 전에 "0 일" 이 되면 화면이 회원을 잘못 다그친다. 어제도 없으면 0 이다.
     */
    val currentStreakDays: Int,
    /** 이번 주에 달성한 날 수. `days` 의 achieved 개수와 같다. */
    val weeklyAchievedCount: Int,
    /** 이번 주 월요일부터 일요일까지 7 개. 오늘 이후 날짜도 `achieved = false` 로 실린다. */
    val days: List<AchievementDayView>,
)

data class AchievementDayView(
    val date: LocalDate,
    val achieved: Boolean,
)
