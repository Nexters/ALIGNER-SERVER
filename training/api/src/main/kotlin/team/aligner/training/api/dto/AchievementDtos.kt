package team.aligner.training.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.training.model.PerceivedResult
import team.aligner.training.model.view.AchievementDayView
import team.aligner.training.model.view.AchievementView
import java.time.LocalDate

/**
 * 핀포즈 직후 체감 기록 요청.
 *
 * **서버가 이 값으로 코스를 바꾸지 않는다.** `TOO_HARD` 는 화면이 "다음 자세로 바꿔드려요" 를
 * 안내할 근거일 뿐이고, 어떤 자세로 옮길지는 아직 정해지지 않았다. 교체는 화면이 기존
 * `POST /courses` 로 새 코스를 받아 진행한다.
 */
@Schema(description = "핀포즈 직후 체감 기록 요청")
data class RecordPerceivedResultRequest(
    @field:Schema(
        description = "SUCCEEDED(잘됐어요) · STILL_HARD(아직 어려워요) · TOO_HARD(안될 거 같아요)",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val perceivedResult: PerceivedResult,
)

/**
 * 완료 리포트 아래쪽의 연속 달성 카드.
 *
 * **날짜는 `Asia/Seoul` 기준**이다. 저장은 UTC 시각이지만 "며칠 연속" 은 회원이 사는 날짜로
 * 세야 한다 — 밤 10 시 운동이 UTC 로는 다음 날이라 그대로 세면 하루가 둘로 갈린다.
 */
@Schema(description = "연속 달성 현황")
data class AchievementResponse(
    @field:Schema(
        description =
            "오늘까지 이어진 연속 달성 일수. **오늘 아직 안 했어도 끊기지 않는다** — 어제까지 " +
                "이어져 있으면 그 값을 유지한다. 어제도 없으면 0 이다",
        example = "5",
    )
    val currentStreakDays: Int,
    @field:Schema(description = "이번 주에 달성한 날 수. days 의 achieved 개수와 같다", example = "5")
    val weeklyAchievedCount: Int,
    @field:Schema(description = "이번 주 월요일부터 일요일까지 7 개. 오늘 이후 날짜도 achieved=false 로 실린다")
    val days: List<AchievementDayResponse>,
) {
    companion object {
        fun from(view: AchievementView): AchievementResponse =
            AchievementResponse(
                currentStreakDays = view.currentStreakDays,
                weeklyAchievedCount = view.weeklyAchievedCount,
                days = view.days.map(AchievementDayResponse::from),
            )
    }
}

@Schema(description = "이번 주 하루")
data class AchievementDayResponse(
    @field:Schema(description = "날짜(Asia/Seoul)", example = "2026-08-10")
    val date: LocalDate,
    @field:Schema(description = "그날 세션을 하나라도 완료했는지", example = "true")
    val achieved: Boolean,
) {
    companion object {
        fun from(view: AchievementDayView): AchievementDayResponse = AchievementDayResponse(date = view.date, achieved = view.achieved)
    }
}
