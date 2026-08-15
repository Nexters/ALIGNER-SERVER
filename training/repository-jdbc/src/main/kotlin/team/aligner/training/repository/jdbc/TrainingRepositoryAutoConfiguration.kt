package team.aligner.training.repository.jdbc

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import team.aligner.training.infrastructure.SessionRepository

/**
 * basePackages 문자열이 아니라 basePackageClasses 를 쓴다. 패키지를 옮겨도 깨지지 않는다
 * (docs/architecture.md §5).
 */
@AutoConfiguration
@EnableJdbcRepositories(basePackageClasses = [SessionJdbcRepository::class])
class TrainingRepositoryAutoConfiguration {
    // internal 함수는 이름이 망글링돼 Bean id 에 Gradle 모듈 경로가 박힌다. 이름을 고정한다.
    @Bean(name = ["sessionRepository"])
    internal fun sessionRepository(sessionJdbcRepository: SessionJdbcRepository): SessionRepository =
        SessionRepositoryImpl(sessionJdbcRepository)

    @Bean(name = ["sessionAchievementQueryRepository"])
    internal fun sessionAchievementQueryRepository(jdbcClient: JdbcClient): SessionAchievementQueryRepository =
        SessionAchievementQueryRepositoryImpl(jdbcClient)
}
