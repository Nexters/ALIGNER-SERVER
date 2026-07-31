package team.aligner.member.service

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.member.contract.MemberAuthContract
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.infrastructure.MemberRepository

/**
 * ComponentScan 을 쓰지 않으므로 Bean 을 여기서 명시한다 (docs/architecture.md §5).
 *
 * AutoConfiguration.imports 에 FQCN 을 등록해야 로딩된다. 빠지면 "Bean 이 없다"로 기동이 실패한다.
 */
@AutoConfiguration
class MemberServiceAutoConfiguration {
    @Bean
    fun memberCommandService(memberRepository: MemberRepository): MemberCommandService = MemberCommandServiceImpl(memberRepository)

    @Bean
    fun memberQueryService(memberQueryRepository: MemberQueryRepository): MemberQueryService = MemberQueryServiceImpl(memberQueryRepository)

    @Bean
    fun memberAuthContract(memberCommandService: MemberCommandService): MemberAuthContract = MemberAuthContractImpl(memberCommandService)
}
