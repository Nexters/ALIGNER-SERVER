package team.aligner.training.service

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.ExerciseDetailPort
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import team.aligner.training.infrastructure.SessionRepository

/**
 * ComponentScan 을 쓰지 않으므로 Bean 을 여기서 명시한다 (docs/architecture.md §5).
 *
 * port 3 개의 구현체는 adapter 모듈에 있다. 그 모듈을 application-api 가 조립하지 않으면
 * **기동이 실패해야 정상이다**.
 */
@AutoConfiguration
class TrainingServiceAutoConfiguration {
    @Bean
    fun sessionService(
        sessionRepository: SessionRepository,
        sessionAchievementQueryRepository: SessionAchievementQueryRepository,
        courseStepPort: CourseStepPort,
        courseProgressPort: CourseProgressPort,
        exerciseDetailPort: ExerciseDetailPort,
    ): SessionService =
        SessionServiceImpl(
            sessionRepository,
            sessionAchievementQueryRepository,
            courseStepPort,
            courseProgressPort,
            exerciseDetailPort,
        )
}
