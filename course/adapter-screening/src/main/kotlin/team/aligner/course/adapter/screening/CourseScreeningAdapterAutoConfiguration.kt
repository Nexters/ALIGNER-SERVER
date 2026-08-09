package team.aligner.course.adapter.screening

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.course.infrastructure.CauseLookupPort
import team.aligner.screening.contract.ScreeningResultContract

/**
 * 이 모듈이 조립되지 않으면 CauseLookupPort Bean 이 없어 기동이 실패해야 정상이다
 * (docs/domains.md §4-1 의 adapter-auth 와 같다).
 */
@AutoConfiguration
class CourseScreeningAdapterAutoConfiguration {
    @Bean(name = ["causeLookupPort"])
    internal fun causeLookupPort(screeningResultContract: ScreeningResultContract): CauseLookupPort =
        CauseLookupAdapter(screeningResultContract)
}
