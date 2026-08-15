package team.aligner.course.api.dto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * enum 정의가 catalog·course·member·screening 의 api 모듈에 네 벌 있다. 값이 갈리면
 * OpenAPI 스키마가 둘로 쪼개져 프론트 생성 타입이 어긋난다. 모듈 간 참조가 금지라
 * 네 벌을 한 테스트로 묶을 수 없으므로, 각 모듈이 자기 값 집합을 스스로 고정한다.
 */
class BodyPartCodeTest :
    DescribeSpec({
        describe("BodyPartCode") {
            it("부위는 등 · 복부 · 골반 셋이다") {
                BodyPartCode.entries.map { it.name } shouldBe listOf("BACK", "ABDOMEN", "PELVIS")
            }

            it("저장된 문자열을 응답 타입으로 되돌린다") {
                BodyPartCode.from("BACK") shouldBe BodyPartCode.BACK
            }

            it("세 값 밖의 코드는 조용히 넘기지 않고 드러낸다") {
                // 응답 경로에서만 나는 예외다. 요청은 Spring 의 enum 변환이 먼저 400 으로 막는다.
                shouldThrow<IllegalStateException> { BodyPartCode.from("NECK_SHOULDER") }
            }

            it("소문자는 다른 값이다") {
                shouldThrow<IllegalStateException> { BodyPartCode.from("back") }
            }
        }
    })
