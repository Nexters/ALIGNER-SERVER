package team.aligner.training.adapter.course

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.course.contract.CourseProgressContract
import team.aligner.course.contract.CourseStepContract
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepPort

/**
 * 이 모듈이 조립되지 않으면 두 port 의 Bean 이 없어 기동이 실패해야 정상이다.
 */
@AutoConfiguration
class TrainingCourseAdapterAutoConfiguration {
    @Bean(name = ["courseStepPort"])
    internal fun courseStepPort(courseStepContract: CourseStepContract): CourseStepPort = CourseStepAdapter(courseStepContract)

    @Bean(name = ["courseProgressPort"])
    internal fun courseProgressPort(courseProgressContract: CourseProgressContract): CourseProgressPort =
        CourseProgressAdapter(courseProgressContract)
}
