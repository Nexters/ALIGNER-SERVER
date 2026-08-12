package team.aligner.course.service

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.course.contract.CourseProgressContract
import team.aligner.course.contract.CourseStepContract
import team.aligner.course.infrastructure.CauseLookupPort
import team.aligner.course.infrastructure.CourseQueryRepository
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.infrastructure.TargetPoseCatalogPort

/**
 * ComponentScan 을 쓰지 않으므로 Bean 을 여기서 명시한다 (docs/architecture.md §5).
 *
 * AutoConfiguration.imports 에 FQCN 을 등록해야 로딩된다. 빠지면 "Bean 이 없다" 로 기동이 실패한다.
 *
 * port 3 개(CauseLookupPort·TargetPoseCatalogPort·ExerciseCatalogPort·MemberBodyPort)의
 * 구현체는 adapter 모듈에 있다. 그 모듈을 application-api 가 조립하지 않으면 **기동이
 * 실패해야 정상이다** (docs/domains.md §4-1 의 adapter-auth 와 같다).
 */
@AutoConfiguration
class CourseServiceAutoConfiguration {
    @Bean
    fun courseCommandService(
        courseRepository: CourseRepository,
        courseTemplateRepository: CourseTemplateRepository,
        stampRepository: StampRepository,
        exerciseCatalogPort: ExerciseCatalogPort,
        memberBodyPort: MemberBodyPort,
        causeLookupPort: CauseLookupPort,
        targetPoseCatalogPort: TargetPoseCatalogPort,
    ): CourseCommandService =
        CourseCommandServiceImpl(
            courseRepository,
            courseTemplateRepository,
            stampRepository,
            exerciseCatalogPort,
            memberBodyPort,
            causeLookupPort,
            targetPoseCatalogPort,
        )

    @Bean
    fun courseQueryService(
        courseQueryRepository: CourseQueryRepository,
        targetPoseCatalogPort: TargetPoseCatalogPort,
        exerciseCatalogPort: ExerciseCatalogPort,
        memberBodyPort: MemberBodyPort,
    ): CourseQueryService =
        CourseQueryServiceImpl(
            courseQueryRepository,
            targetPoseCatalogPort,
            exerciseCatalogPort,
            memberBodyPort,
        )

    @Bean
    fun courseProgressContract(courseCommandService: CourseCommandService): CourseProgressContract =
        CourseProgressContractImpl(courseCommandService)

    @Bean
    fun courseStepContract(courseRepository: CourseRepository): CourseStepContract = CourseStepContractImpl(courseRepository)
}
