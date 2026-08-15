package team.aligner.catalog.service

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.catalog.contract.ExerciseContract
import team.aligner.catalog.contract.TargetPoseContract
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.infrastructure.PoseVideoPort
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
    /**
     * PoseVideoPort 를 요구하는 유일한 Bean 이다. catalog:adapter-ymove 가 조립에서 빠지면
     * 여기서 기동이 실패해야 정상이다 (docs/architecture.md §9 의 adapter-auth 와 같다).
     */
    @Bean
    fun exerciseQueryService(
        exerciseQueryRepository: ExerciseQueryRepository,
        poseVideoPort: PoseVideoPort,
    ): ExerciseQueryService = ExerciseQueryServiceImpl(exerciseQueryRepository, poseVideoPort)

    @Bean
    fun targetPoseQueryService(targetPoseQueryRepository: TargetPoseQueryRepository): TargetPoseQueryService =
        TargetPoseQueryServiceImpl(targetPoseQueryRepository)

    @Bean
    fun exerciseContract(exerciseQueryService: ExerciseQueryService): ExerciseContract = ExerciseContractImpl(exerciseQueryService)

    @Bean
    fun targetPoseContract(targetPoseQueryService: TargetPoseQueryService): TargetPoseContract =
        TargetPoseContractImpl(targetPoseQueryService)
}
