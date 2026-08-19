package team.aligner.api.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * 카카오 `redirect_uri` 는 환경마다 다르고, `authorize()` 에 넘긴 값·카카오 콘솔 등록값과
 * 완전히 일치해야만 토큰 교환이 통과한다. 그래서 **어느 프로필에서 무엇으로 결정되는지**가
 * 코드가 아니라 설정 파일 하나에 달려 있다.
 *
 * 프로필 문서는 공통 문서를 덮으므로, 프로필 쪽에 리터럴을 박으면 `KAKAO_REDIRECT_URI` 가
 * 조용히 무시된다. 배포 env 파일에 값을 넣어도 반영되지 않고, 로그인만 400 으로 죽는다.
 * 이 테스트는 그 회귀를 잡는다.
 */
class KakaoRedirectUriTest {
    private val runner =
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())

    @Test
    fun `dev 프로필의 기본값은 프론트 로컬 개발 서버다`() {
        runner
            .withPropertyValues("spring.profiles.active=dev")
            .run { context ->
                context.environment.getProperty(REDIRECT_URI_KEY) shouldBe "http://localhost:5173/oauth/kakao"
            }
    }

    @Test
    fun `prod 프로필의 기본값은 운영 도메인이다`() {
        runner
            .withPropertyValues("spring.profiles.active=prod")
            .run { context ->
                context.environment.getProperty(REDIRECT_URI_KEY) shouldBe "https://www.aligneryoga.com/oauth/kakao"
            }
    }

    @Test
    fun `KAKAO_REDIRECT_URI 를 주면 프로필 기본값을 덮는다`() {
        listOf("dev", "prod", "test").forEach { profile ->
            runner
                .withPropertyValues(
                    "spring.profiles.active=$profile",
                    "KAKAO_REDIRECT_URI=https://override.example.com/oauth/kakao",
                ).run { context ->
                    context.environment.getProperty(REDIRECT_URI_KEY) shouldBe "https://override.example.com/oauth/kakao"
                }
        }
    }

    private companion object {
        const val REDIRECT_URI_KEY = "aligner.auth.kakao.redirect-uri"
    }
}
