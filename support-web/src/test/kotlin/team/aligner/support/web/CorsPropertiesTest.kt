package team.aligner.support.web

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

/**
 * 설정 값 자체의 방어. 필터체인 동작은 CorsConfigurationTest 가 본다.
 */
class CorsPropertiesTest :
    DescribeSpec({
        /** 환경변수가 실제로 거치는 경로. 문자열 하나가 List 로 풀리는 것도 바인더의 동작이다. */
        fun bind(allowedOrigins: String): CorsProperties =
            Binder(
                MapConfigurationPropertySource(
                    mapOf(
                        "aligner.web.cors.allowed-origins" to allowedOrigins,
                        "aligner.web.cors.max-age-seconds" to "3600",
                    ),
                ),
            ).bind("aligner.web.cors", CorsProperties::class.java).get()

        describe("환경변수 바인딩") {
            it("쉼표로 여러 오리진을 준다") {
                // docs/frontend-integration.md 가 프론트에 안내하는 형식이다.
                bind("http://localhost:5173,https://aligner.example.com").allowedOrigins shouldBe
                    listOf("http://localhost:5173", "https://aligner.example.com")
            }

            it("하나만 주면 원소 하나짜리 목록이다") {
                bind("http://localhost:5173").allowedOrigins shouldBe listOf("http://localhost:5173")
            }

            it("빈 문자열이면 교차 출처가 전부 막힌다") {
                // 배포에서 값을 빠뜨렸을 때 열리는 쪽이 아니라 닫히는 쪽으로 실패해야 한다.
                bind("").allowedOrigins shouldBe emptyList()
            }

            it("바인딩 단계에서도 * 를 막는다") {
                // init 검사가 바인더를 거쳐도 살아 있어야 의미가 있다.
                shouldThrow<Exception> { bind("*") }
            }
        }

        describe("allowedOrigins") {
            it("* 를 넣으면 기동 시점에 막는다") {
                // allowCredentials 가 false 라 * 는 문법상 통과한다. 막지 않으면 설정 실수로
                // 전체 공개가 된 것을 아무도 모른 채 배포된다.
                shouldThrow<IllegalArgumentException> {
                    CorsProperties(allowedOrigins = listOf("*"), maxAgeSeconds = 3600)
                }
            }

            it("허용 목록에 * 가 섞여 있어도 막는다") {
                shouldThrow<IllegalArgumentException> {
                    CorsProperties(allowedOrigins = listOf("https://aligner.app", "*"), maxAgeSeconds = 3600)
                }
            }

            it("비어 있는 것은 허용한다 — 교차 출처를 전부 막는 상태다") {
                // 설정을 빠뜨렸을 때 열리는 쪽이 아니라 닫히는 쪽으로 실패해야 한다.
                shouldNotThrowAny { CorsProperties(allowedOrigins = emptyList(), maxAgeSeconds = 3600) }
            }
        }
    })
