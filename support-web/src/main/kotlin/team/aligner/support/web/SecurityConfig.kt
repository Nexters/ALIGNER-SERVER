package team.aligner.support.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * 도메인 횡단 보안 설정.
 *
 * **카카오 OAuth2 는 아직 붙이지 않는다** (이슈 #3 범위 밖). member 도메인이 없어
 * AuthMemberPort 구현체가 없기 때문이다. 지금은 무상태 필터체인만 세워두고,
 * 인증 규칙은 member 착수 시 이 파일에서 채운다.
 *
 * 그 사이 기본값은 `authenticated()` 로 닫아둔다. 이 클래스는 AutoConfiguration.imports 에
 * 등록돼 프로덕션 경로에도 그대로 실리므로, `permitAll()` 을 임시로 두면 도메인 API 가
 * 붙는 순간 무인증으로 열린다. 공개해야 할 경로는 그때 여기에 하나씩 명시한다.
 *
 * Pod 이중화 전제이므로 세션을 쓰지 않는다 (AGENTS.md §4 배포 구성).
 *
 * `before` 가 없으면 Boot 기본 체인이 이긴다. auto-configuration 은 클래스명 알파벳순으로 먼저
 * 정렬되므로 org.springframework... 가 team.aligner... 보다 앞선다. 그 시점에 우리
 * SecurityFilterChain 은 아직 등록 전이라 ServletWebSecurityAutoConfiguration 의
 * `@ConditionalOnDefaultWebSecurity` 가 통과해 기본 체인이 함께 뜨고, 기본 체인의
 * `@Order(BASIC_AUTH_ORDER)` 가 우리 것(순서 미지정)보다 앞서 매칭된다. 결과적으로 아래 설정이
 * 전부 죽는다.
 */
@AutoConfiguration(before = [ServletWebSecurityAutoConfiguration::class])
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .build()
}
