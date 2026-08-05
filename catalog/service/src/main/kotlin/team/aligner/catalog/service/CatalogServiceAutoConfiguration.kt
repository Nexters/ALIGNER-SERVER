package team.aligner.catalog.service

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.catalog.contract.ExerciseContract
import team.aligner.catalog.contract.TargetPoseContract
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.infrastructure.TargetPoseQueryRepository

/**
 * ComponentScan 을 쓰지 않으므로 Bean 을 여기서 명시한다 (docs/architecture.md §5).
 *
 * AutoConfiguration.imports 에 FQCN 을 등록해야 로딩된다. 빠지면 "Bean 이 없다"로 기동이 실패한다.
 *
 * CommandService Bean 이 없다. catalog 는 쓰기가 없는 도메인이다 (docs/domains.md §4-3).
 */
@AutoConfiguration
class CatalogServiceAutoConfiguration {
    @Bean
    fun exerciseQueryService(exerciseQueryRepository: ExerciseQueryRepository): ExerciseQueryService =
        ExerciseQueryServiceImpl(exerciseQueryRepository)

    @Bean
    fun targetPoseQueryService(targetPoseQueryRepository: TargetPoseQueryRepository): TargetPoseQueryService =
        TargetPoseQueryServiceImpl(targetPoseQueryRepository)

    @Bean
    fun exerciseContract(exerciseQueryService: ExerciseQueryService): ExerciseContract = ExerciseContractImpl(exerciseQueryService)

    @Bean
    fun targetPoseContract(targetPoseQueryService: TargetPoseQueryService): TargetPoseContract =
        TargetPoseContractImpl(targetPoseQueryService)
}
