package team.aligner.training.repository.jdbc.bootstrap

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

/**
 * 통합 테스트 부트스트랩.
 *
 * ComponentScan 을 쓰지 않는 구조라 @SpringBootApplication 을 쓸 수 없다.
 *
 * 이 클래스를 리포지토리와 같은 패키지에 두면 안 된다. Boot 는 @SpringBootConfiguration
 * 클래스의 패키지를 auto-configuration 패키지로 잡고 그 아래에서 JDBC 리포지토리를 한 번
 * 등록하는데, TrainingRepositoryAutoConfiguration 의 @EnableJdbcRepositories 가 같은 것을
 * 또 등록해 BeanDefinitionOverrideException 이 난다 (member 에 같은 주석이 있다).
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class TrainingRepositoryTestApplication
