package team.aligner.support.web.bootstrap

import org.slf4j.MDC
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import team.aligner.support.web.AuthMemberPort
import team.aligner.support.web.AuthenticatedMember
import team.aligner.support.web.KakaoLoginCommand

/**
 * support-web 슬라이스 부트스트랩.
 *
 * ComponentScan 을 쓰지 않는 구조라 @SpringBootApplication 을 쓸 수 없다.
 * @EnableAutoConfiguration 이 main 리소스의 AutoConfiguration.imports 를 읽어
 * SecurityConfig · SupportWebAutoConfiguration 을 그대로 로딩한다 (docs/architecture.md §5).
 *
 * **실제 배선을 그대로 태우는 것이 요점이다.** SecurityConfig 를 @Import 로 집어 오면
 * imports 파일에서 빠뜨렸을 때를 못 잡는다.
 *
 * AuthMemberPort 스텁이 필요한 이유는 SupportWebAutoConfiguration 의 주석에 있다 — 이 port 를
 * 요구하는 유일한 Bean(kakaoAuthController)이 없으면 컨텍스트가 뜨지 않는다. 운영에서는
 * member/adapter-auth 가 채운다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class SupportWebTestApplication {
    @Bean
    fun authMemberPort(): AuthMemberPort =
        object : AuthMemberPort {
            override fun findOrRegisterByKakao(command: KakaoLoginCommand) = AuthenticatedMember(memberId = 1L)
        }

    /** 인증이 필요한 경로 하나. 도메인 모듈 없이 필터체인만 확인하기 위한 것이다. */
    @Bean
    fun protectedTestController() = ProtectedTestController()
}

@RestController
class ProtectedTestController {
    @GetMapping(PROTECTED_PATH)
    fun protectedEndpoint(): Map<String, String> = mapOf("ok" to "true")

    @GetMapping(MDC_INSPECT_PATH)
    fun inspectMdc(): Map<String, String> = MDC.getCopyOfContextMap() ?: emptyMap()
}

const val PROTECTED_PATH = "/test/protected"
const val MDC_INSPECT_PATH = "/test/mdc"
