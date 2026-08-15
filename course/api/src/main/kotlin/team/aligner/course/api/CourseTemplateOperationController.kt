package team.aligner.course.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.aligner.course.api.dto.CourseTemplateResponse
import team.aligner.course.service.CourseQueryService

/**
 * 운영용 코스 템플릿 목록.
 *
 * **회원별 코스(`/courses`)가 아니라 마스터다.** 그래서 `@AuthenticationPrincipal` 이 없다 —
 * 회원과 무관한 값이고, 남의 코스를 보는 경로가 되지 않는다. 인증 자체는 SecurityConfig
 * 기본값(anyRequest().authenticated())이 여전히 요구한다.
 *
 * 실행 모듈을 새로 만들지 않고 application-api 에 함께 싣는다. 근거는
 * ExerciseOperationController 주석과 같다 (docs/domains.md §7-16).
 *
 * 이 클래스는 CourseApiAutoConfiguration 이 @Bean 으로 등록한다 (docs/architecture.md §5).
 */
@Tag(name = "운영 — 코스", description = "감수 콘텐츠 점검용 코스 템플릿 목록")
@RestController
@RequestMapping("/operation/course-templates")
class CourseTemplateOperationController(
    private val courseQueryService: CourseQueryService,
) {
    @Operation(
        summary = "코스 템플릿 전체 조회",
        description =
            "적재된 코스 템플릿을 식별자 순으로 전부 내린다. 스텝과 스텝에 편성된 운동을 함께 싣는다. " +
                "**페이징이 없다** — 템플릿은 핀포즈 하나에 하나라 상한이 자세 개수(현재 9)다. " +
                "자세 이름·운동 이름은 catalog 에서 붙이고, 찾지 못하면 예외가 아니라 빈 문자열이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공. 템플릿이 없으면 빈 배열이다"),
        ],
    )
    @GetMapping
    fun getCourseTemplates(): List<CourseTemplateResponse> =
        courseQueryService
            .getAllCourseTemplates()
            .map(CourseTemplateResponse::from)
}
