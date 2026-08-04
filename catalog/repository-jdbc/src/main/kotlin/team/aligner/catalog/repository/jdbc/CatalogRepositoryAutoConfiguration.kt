package team.aligner.catalog.repository.jdbc

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.infrastructure.TargetPoseQueryRepository

/**
 * ComponentScan 을 쓰지 않으므로 Bean 을 여기서 명시한다 (docs/architecture.md §5).
 *
 * `@EnableJdbcRepositories` 를 붙이지 않는다. catalog 에는 CrudRepository 를 상속한 인터페이스가
 * 하나도 없어 스캔할 대상이 없다. 쓰기가 없는 도메인이라 엔티티도 없다 (docs/domains.md §4-3).
 */
@AutoConfiguration
class CatalogRepositoryAutoConfiguration {
    @Bean
    fun exerciseQueryRepository(jdbcClient: JdbcClient): ExerciseQueryRepository = ExerciseQueryRepositoryImpl(jdbcClient)

    @Bean
    fun targetPoseQueryRepository(jdbcClient: JdbcClient): TargetPoseQueryRepository = TargetPoseQueryRepositoryImpl(jdbcClient)
}
