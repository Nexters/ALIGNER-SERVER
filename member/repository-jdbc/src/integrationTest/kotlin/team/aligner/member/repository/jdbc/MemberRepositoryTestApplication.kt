package team.aligner.member.repository.jdbc

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

/**
 * 통합 테스트 부트스트랩.
 *
 * ComponentScan 을 쓰지 않는 구조라 @SpringBootApplication 을 쓸 수 없다.
 * @EnableAutoConfiguration 이 이 모듈 main 리소스의 AutoConfiguration.imports 를 읽어
 * MemberRepositoryAutoConfiguration 을 로딩한다 (docs/architecture.md §5).
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class MemberRepositoryTestApplication
