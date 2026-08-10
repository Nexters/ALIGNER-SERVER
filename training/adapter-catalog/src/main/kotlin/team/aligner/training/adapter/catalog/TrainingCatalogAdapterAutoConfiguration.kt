package team.aligner.training.adapter.catalog

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.catalog.contract.ExerciseContract
import team.aligner.training.infrastructure.ExerciseDetailPort

@AutoConfiguration
class TrainingCatalogAdapterAutoConfiguration {
    @Bean(name = ["exerciseDetailPort"])
    internal fun exerciseDetailPort(exerciseContract: ExerciseContract): ExerciseDetailPort = ExerciseDetailAdapter(exerciseContract)
}
