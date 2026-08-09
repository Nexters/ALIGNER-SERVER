package team.aligner.screening.service

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.screening.contract.ScreeningResultContract
import team.aligner.screening.infrastructure.CauseRuleRepository
import team.aligner.screening.infrastructure.ScreeningQueryRepository
import team.aligner.screening.infrastructure.ScreeningResultRepository

/**
 * ComponentScan 을 쓰지 않으므로 Bean 을 여기서 명시한다 (docs/architecture.md §5).
 *
 * AutoConfiguration.imports 에 FQCN 을 등록해야 로딩된다. 빠지면 "Bean 이 없다" 로 기동이 실패한다.
 */
@AutoConfiguration
class ScreeningServiceAutoConfiguration {
    @Bean
    fun screeningCommandService(
        screeningResultRepository: ScreeningResultRepository,
        causeRuleRepository: CauseRuleRepository,
    ): ScreeningCommandService = ScreeningCommandServiceImpl(screeningResultRepository, causeRuleRepository)

    @Bean
    fun screeningQueryService(screeningQueryRepository: ScreeningQueryRepository): ScreeningQueryService =
        ScreeningQueryServiceImpl(screeningQueryRepository)

    @Bean
    fun screeningResultContract(screeningQueryRepository: ScreeningQueryRepository): ScreeningResultContract =
        ScreeningResultContractImpl(screeningQueryRepository)
}
