package team.aligner.course.repository.jdbc

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.StampRepository

/**
 * basePackages 문자열이 아니라 basePackageClasses 를 쓴다. 패키지를 옮겨도 깨지지 않는다
 * (docs/architecture.md §5).
 */
@AutoConfiguration
@EnableJdbcRepositories(basePackageClasses = [CourseJdbcRepository::class])
class CourseRepositoryAutoConfiguration {
    // internal 함수는 이름이 망글링돼 Bean id 에 Gradle 모듈 경로가 박힌다. 이름을 고정한다.
    @Bean(name = ["courseRepository"])
    internal fun courseRepository(courseJdbcRepository: CourseJdbcRepository): CourseRepository = CourseRepositoryImpl(courseJdbcRepository)

    @Bean(name = ["stampRepository"])
    internal fun stampRepository(jdbcClient: JdbcClient): StampRepository = StampRepositoryImpl(jdbcClient)

    @Bean(name = ["courseTemplateRepository"])
    internal fun courseTemplateRepository(jdbcClient: JdbcClient): CourseTemplateRepository = CourseTemplateRepositoryImpl(jdbcClient)

    @Bean(name = ["courseQueryRepository"])
    internal fun courseQueryRepository(jdbcClient: JdbcClient): CourseQueryRepository = CourseQueryRepositoryImpl(jdbcClient)
}
