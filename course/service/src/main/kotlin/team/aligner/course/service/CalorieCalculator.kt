package team.aligner.course.service

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * `kcal = MET × 3.5 × 체중(kg) ÷ 200 × 분`.
 *
 * **저장하지 않고 조회할 때마다 계산한다.** 회원 몸무게의 함수라 저장값이 될 수 없다
 * (docs/domains.md §4-3). 몸무게가 바뀌면 지난 코스의 칼로리도 같이 바뀌는 것이 맞다.
 *
 * `catalog` 가 계산하지 못하는 이유이기도 하다 — 몸무게는 `member` 소유다.
 */
internal object CalorieCalculator {
    private val MET_FACTOR = BigDecimal("3.5")
    private val DIVISOR = BigDecimal("200")
    private val SECONDS_PER_MINUTE = BigDecimal("60")

    /**
     * 입력이 하나라도 비면 **0 이 아니라 null** 이다.
     *
     * 0 kcal 은 "운동량이 없다" 는 뜻이라 "계산할 수 없다" 와 다르다. 화면이 둘을 구분해야
     * 하므로 여기서 뭉개지 않는다 — 온보딩에서 몸무게를 아직 받지 않은 회원이 흔하다.
     */
    fun calculate(
        metValue: BigDecimal?,
        weightKg: Int?,
        durationSeconds: Int?,
    ): Int? {
        if (metValue == null || weightKg == null || durationSeconds == null || durationSeconds <= 0) {
            return null
        }
        val minutes = BigDecimal(durationSeconds).divide(SECONDS_PER_MINUTE, 4, RoundingMode.HALF_UP)
        return metValue
            .multiply(MET_FACTOR)
            .multiply(BigDecimal(weightKg))
            .divide(DIVISOR, 4, RoundingMode.HALF_UP)
            .multiply(minutes)
            // 화면이 정수 kcal 만 그린다. 반올림은 마지막에 한 번만 한다.
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }

    /**
     * 코스 칼로리는 **스텝 합**이다. 레벨별 고정값을 쓰지 않는 이유는 구성이 회원마다
     * 달라질 수 있어서다 (docs/domains.md §4-3).
     *
     * 하나라도 계산할 수 없으면 합계도 null 이다. 일부만 더한 값을 내리면 화면이 그것을
     * 코스 전체 칼로리로 읽는다.
     */
    fun sum(values: List<Int?>): Int? = if (values.isEmpty() || values.any { it == null }) null else values.filterNotNull().sum()
}
