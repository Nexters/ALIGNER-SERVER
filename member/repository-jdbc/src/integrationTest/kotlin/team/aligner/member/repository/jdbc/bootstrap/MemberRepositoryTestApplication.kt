package team.aligner.member.repository.jdbc.bootstrap

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

/**
 * 통합 테스트 부트스트랩.
 *
 * ComponentScan 을 쓰지 않는 구조라 @SpringBootApplication 을 쓸 수 없다.
 * @EnableAutoConfiguration 이 이 모듈 main 리소스의 AutoConfiguration.imports 를 읽어
 * MemberRepositoryAutoConfiguration 을 로딩한다 (docs/architecture.md §5).
 *
 * 이 클래스를 리포지토리와 같은 패키지에 두면 안 된다. Boot 는 @SpringBootConfiguration
 * 클래스의 패키지를 auto-configuration 패키지로 잡고 그 아래에서 JDBC 리포지토리를 한 번
 * 등록하는데, MemberRepositoryAutoConfiguration 의 @EnableJdbcRepositories 가 같은 것을
 * 또 등록해 BeanDefinitionOverrideException 이 난다.
 *
 * 운영에서는 AlignerApplication 이 team.aligner.api 에 있어 스캔 범위가 겹치지 않는다.
 * 부트스트랩을 하위 패키지로 내려 그 조건을 그대로 재현한다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class MemberRepositoryTestApplication
