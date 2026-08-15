package team.aligner.screening.repository.jdbc

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.screening.infrastructure.BodyPartRepository
import team.aligner.screening.infrastructure.CauseRuleRepository
import team.aligner.screening.infrastructure.ScreeningQueryRepository
import team.aligner.screening.infrastructure.ScreeningResultRepository

/**
 * basePackages 문자열이 아니라 basePackageClasses 를 쓴다. 패키지를 옮겨도 깨지지 않는다
 * (docs/architecture.md §5).
 */
@AutoConfiguration
@EnableJdbcRepositories(basePackageClasses = [ScreeningResultJdbcRepository::class])
class ScreeningRepositoryAutoConfiguration {
    // internal 함수는 이름이 망글링돼 Bean id 에 Gradle 모듈 경로가 박힌다. 이름을 고정한다.
    @Bean(name = ["screeningResultRepository"])
    internal fun screeningResultRepository(screeningResultJdbcRepository: ScreeningResultJdbcRepository): ScreeningResultRepository =
        ScreeningResultRepositoryImpl(screeningResultJdbcRepository)

    @Bean(name = ["bodyPartRepository"])
    internal fun bodyPartRepository(jdbcClient: JdbcClient): BodyPartRepository = BodyPartRepositoryImpl(jdbcClient)

    @Bean(name = ["causeRuleRepository"])
    internal fun causeRuleRepository(jdbcClient: JdbcClient): CauseRuleRepository = CauseRuleRepositoryImpl(jdbcClient)

    @Bean(name = ["screeningQueryRepository"])
    internal fun screeningQueryRepository(jdbcClient: JdbcClient): ScreeningQueryRepository = ScreeningQueryRepositoryImpl(jdbcClient)
}
