package team.aligner.screening.repository.jdbc.bootstrap

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

/**
 * 통합 테스트 부트스트랩.
 *
 * ComponentScan 을 쓰지 않는 구조라 @SpringBootApplication 을 쓸 수 없다.
 * @EnableAutoConfiguration 이 이 모듈 main 리소스의 AutoConfiguration.imports 를 읽어
 * ScreeningRepositoryAutoConfiguration 을 로딩한다 (docs/architecture.md §5).
 *
 * 하위 패키지에 두는 것은 member 의 선례를 따른 것이다. @EnableJdbcRepositories 와 부트스트랩
 * 스캔이 같은 패키지에서 겹치면 BeanDefinitionOverrideException 이 난다 — screening 도
 * CrudRepository 를 쓰므로 member 와 같은 조건이다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class ScreeningRepositoryTestApplication
