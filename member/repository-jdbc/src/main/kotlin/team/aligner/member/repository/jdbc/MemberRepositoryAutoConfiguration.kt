package team.aligner.member.repository.jdbc

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.simple.JdbcClient
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.infrastructure.MemberRepository

/**
 * basePackages 문자열이 아니라 basePackageClasses 를 쓴다. 패키지를 옮겨도 깨지지 않는다
 * (docs/architecture.md §5).
 */
@AutoConfiguration
@EnableJdbcRepositories(basePackageClasses = [MemberJdbcRepository::class])
class MemberRepositoryAutoConfiguration {
    // internal 함수는 이름이 망글링돼 Bean id 에 Gradle 모듈 경로가 박힌다. 이름을 고정한다.
    @Bean(name = ["memberRepository"])
    internal fun memberRepository(memberJdbcRepository: MemberJdbcRepository): MemberRepository = MemberRepositoryImpl(memberJdbcRepository)

    @Bean
    fun memberQueryRepository(jdbcClient: JdbcClient): MemberQueryRepository = MemberQueryRepositoryImpl(jdbcClient)
}
