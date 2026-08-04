package team.aligner.catalog.repository.jdbc.bootstrap

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

/**
 * 통합 테스트 부트스트랩.
 *
 * ComponentScan 을 쓰지 않는 구조라 @SpringBootApplication 을 쓸 수 없다.
 * @EnableAutoConfiguration 이 이 모듈 main 리소스의 AutoConfiguration.imports 를 읽어
 * CatalogRepositoryAutoConfiguration 을 로딩한다 (docs/architecture.md §5).
 *
 * member 는 @EnableJdbcRepositories 와 부트스트랩 스캔이 겹쳐 BeanDefinitionOverrideException
 * 이 나는 문제가 있어 하위 패키지로 내려야 했다. catalog 는 CrudRepository 가 없어 그 충돌이
 * 원천적으로 없지만, 같은 자리에 두는 관례를 지킨다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class CatalogRepositoryTestApplication
