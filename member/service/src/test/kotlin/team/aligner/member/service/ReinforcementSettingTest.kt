package team.aligner.member.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import team.aligner.member.model.ReinforcementSetting
import team.aligner.member.model.exception.InvalidReinforcementSettingException

/**
 * 값 객체의 불변식을 고정한다.
 *
 * 모델 테스트를 service 모듈 test 소스셋에 두는 것은 screening 의 ScreeningResultTest 와
 * 같은 배치다.
 */
class ReinforcementSettingTest :
    DescribeSpec({
        describe("생성") {
            it("부위와 난이도가 정상이면 만들어진다") {
                shouldNotThrowAny { ReinforcementSetting("BACK", 1) }
                shouldNotThrowAny { ReinforcementSetting("BACK", 3) }
            }

            it("부위 코드가 비면 막는다") {
                shouldThrow<InvalidReinforcementSettingException> { ReinforcementSetting("", 1) }
                shouldThrow<InvalidReinforcementSettingException> { ReinforcementSetting("   ", 1) }
            }

            /**
             * reinforcement_body_part_code 가 VARCHAR(40) 이다. 여기서 막지 않으면 DB 가 거부해
             * 회원 입력 문제가 400 이 아니라 500 으로 나간다.
             */
            it("부위 코드가 40자를 넘으면 막는다") {
                shouldNotThrowAny {
                    ReinforcementSetting("A".repeat(ReinforcementSetting.BODY_PART_CODE_MAX_LENGTH), 1)
                }
                shouldThrow<InvalidReinforcementSettingException> {
                    ReinforcementSetting("A".repeat(ReinforcementSetting.BODY_PART_CODE_MAX_LENGTH + 1), 1)
                }
            }

            it("난이도가 1~3 밖이면 막는다") {
                shouldThrow<InvalidReinforcementSettingException> { ReinforcementSetting("BACK", 0) }
                shouldThrow<InvalidReinforcementSettingException> { ReinforcementSetting("BACK", 4) }
            }
        }
    })
