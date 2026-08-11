package team.aligner.mock

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.runApplication

/**
 * 프론트 연동용 더미 목 서버. **임시 개발 도구이고 배포 대상이 아니다.**
 *
 * seed 가 없어 실제 서버로는 로그인 다음 화면부터 아무것도 그릴 수 없다. 그 사이에 프론트가
 * API 형태와 화면 전환을 실제 HTTP 로 확인할 수 있게 한다 (이슈 #29).
 *
 * **@SpringBootApplication 을 쓰지 않는다.** 조립을 클래스패스 우연이 아니라 명시 선언이
 * 결정하게 하는 것은 여기서도 같다 (docs/architecture.md §5).
 *
 * 인증은 실제 그대로다. support-web 의 JWT 필터·CORS·에러 포맷을 쓰고 AuthMemberPort 만
 * 고정 회원으로 갈아끼운다 — 토큰 발급과 검증은 진짜다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class AlignerMockApplication

fun main(args: Array<String>) {
    runApplication<AlignerMockApplication>(*args)
}
