package team.aligner.catalog.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.aligner.catalog.api.dto.ExerciseSummaryResponse
import team.aligner.catalog.service.ExerciseQueryService

/**
 * 운영용 운동 목록.
 *
 * **회원 화면이 아니라 감수 콘텐츠 점검용이다.** 그래서 회원용 `/catalog/exercises` 와 경로를
 * 나눈다 — 뜻이 다른 조회가 같은 경로 아래 섞이면 나중에 보안 경계를 경로로 가를 수 없다.
 *
 * 실행 모듈을 새로 만들지 않고 application-api 에 함께 싣는다. 조회뿐이라 seed 와 DB 상태가
 * 갈라지지 않고, 실행 모듈을 하나로 둔 결정(docs/architecture.md §2)을 건드릴 이유가 없다
 * (docs/domains.md §7-16).
 *
 * **인증은 아직 회원 JWT 그대로다.** SecurityConfig 기본값이 anyRequest().authenticated() 라
 * 로그인한 회원이면 부를 수 있다. 콘텐츠 자체가 회원에게 공개되는 값이라 지금은 이 경계로
 * 충분하지만, 쓰기나 회원 데이터가 이 경로에 붙는 순간 운영 전용 권한이 먼저 필요하다.
 *
 * 이 클래스는 CatalogApiAutoConfiguration 이 @Bean 으로 등록한다. ComponentScan 이 없어
 * @RestController 만으로는 등록되지 않는다 (docs/architecture.md §5).
 */
@Tag(name = "운영 — 운동", description = "감수 콘텐츠 점검용 운동 목록")
@RestController
@RequestMapping("/operation/exercises")
class ExerciseOperationController(
    private val exerciseQueryService: ExerciseQueryService,
) {
    @Operation(
        summary = "운동 전체 조회",
        description =
            "적재된 운동을 식별자 순으로 전부 내린다. **페이징이 없다** — 운동은 감수 seed 로만 " +
                "늘어나고 지금 29 행이다. " +
                "근육·음성 큐·주의사항은 싣지 않는다. 하나를 자세히 볼 때는 GET /catalog/exercises/{exerciseId} 다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공. 운동이 없으면 빈 배열이다"),
        ],
    )
    @GetMapping
    fun getExercises(): List<ExerciseSummaryResponse> =
        exerciseQueryService
            .getAll()
            .map(ExerciseSummaryResponse::from)
}
