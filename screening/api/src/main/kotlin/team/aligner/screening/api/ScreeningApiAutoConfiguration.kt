package team.aligner.screening.api

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.screening.service.ScreeningCommandService
import team.aligner.screening.service.ScreeningQueryService

/**
 * 컨트롤러를 @Bean 으로 등록하는 자리다. 빠지면 기동은 성공하고 호출만 404 가 되므로
 * 사람 눈으로만 잡힌다 (docs/architecture.md §5).
 */
@AutoConfiguration
class ScreeningApiAutoConfiguration {
    @Bean
    fun screeningController(
        screeningCommandService: ScreeningCommandService,
        screeningQueryService: ScreeningQueryService,
    ): ScreeningController = ScreeningController(screeningCommandService, screeningQueryService)
}
