package team.aligner.course.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

/**
 * kcal = MET × 3.5 × 체중 ÷ 200 × 분.
 *
 * 저장하지 않고 조회할 때마다 계산하는 값이라 공식이 여기서 고정돼야 한다
 * (docs/domains.md §4-3).
 */
class CalorieCalculatorTest :
    DescribeSpec({
        describe("calculate") {
            it("공식대로 계산한다") {
                // 3.0 × 3.5 × 60 ÷ 200 × 2분 = 6.3 → 6
                CalorieCalculator.calculate(BigDecimal("3.00"), 60, 120) shouldBe 6
            }

            /**
             * 0 kcal 은 "운동량 없음" 이라 "계산할 수 없음" 과 다르다. 화면이 둘을 구분해야
             * 하므로 뭉개지 않는다 — 온보딩에서 몸무게를 아직 받지 않은 회원이 흔하다.
             */
            it("몸무게가 없으면 0 이 아니라 null 이다") {
                CalorieCalculator.calculate(BigDecimal("3.00"), null, 120).shouldBeNull()
            }

            it("MET 이 없으면 null 이다") {
                CalorieCalculator.calculate(null, 60, 120).shouldBeNull()
            }

            it("수행 시간이 없거나 0 이하면 null 이다") {
                CalorieCalculator.calculate(BigDecimal("3.00"), 60, null).shouldBeNull()
                CalorieCalculator.calculate(BigDecimal("3.00"), 60, 0).shouldBeNull()
            }
        }

        describe("sum") {
            it("스텝 합이 코스 칼로리다") {
                CalorieCalculator.sum(listOf(6, 6, 7)) shouldBe 19
            }

            /**
             * 일부만 더한 값을 내리면 화면이 그것을 코스 전체 칼로리로 읽는다.
             */
            it("하나라도 계산할 수 없으면 합계도 null 이다") {
                CalorieCalculator.sum(listOf(6, null, 7)).shouldBeNull()
            }

            it("빈 코스는 null 이다") {
                CalorieCalculator.sum(emptyList()).shouldBeNull()
            }
        }
    })
