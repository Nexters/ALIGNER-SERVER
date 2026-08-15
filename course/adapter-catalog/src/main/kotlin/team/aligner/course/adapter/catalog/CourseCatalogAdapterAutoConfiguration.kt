package team.aligner.course.adapter.catalog

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.catalog.contract.ExerciseContract
import team.aligner.catalog.contract.TargetPoseContract
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.TargetPoseCatalogPort

@AutoConfiguration
class CourseCatalogAdapterAutoConfiguration {
    @Bean(name = ["targetPoseCatalogPort"])
    internal fun targetPoseCatalogPort(targetPoseContract: TargetPoseContract): TargetPoseCatalogPort =
        TargetPoseCatalogAdapter(targetPoseContract)

    @Bean(name = ["exerciseCatalogPort"])
    internal fun exerciseCatalogPort(exerciseContract: ExerciseContract): ExerciseCatalogPort = ExerciseCatalogAdapter(exerciseContract)
}
