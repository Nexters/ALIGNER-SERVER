package team.aligner.catalog.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.aligner.catalog.api.dto.ExerciseDetailResponse
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.service.ExerciseQueryService

/**
 * 운동 가이드 화면. 근육맵 탭과 주의사항이 여기서 나간다.
 *
 * `@AuthenticationPrincipal` 이 없다. 운동 카탈로그는 회원별로 달라지지 않는다. 다만
 * SecurityConfig 기본값이 anyRequest().authenticated() 라 인증 자체는 여전히 필요하다
 * — permitAll 을 추가하지 않는다.
 *
 * **회원용 목록 엔드포인트를 만들지 않는다.** 운동 전체를 그리는 회원 화면이 없다
 * (docs/architecture.md §4). 감수 콘텐츠 점검용 전체 목록은 ExerciseOperationController 가
 * `/operation/exercises` 로 따로 낸다.
 *
 * 이 클래스는 CatalogApiAutoConfiguration 이 @Bean 으로 등록한다. ComponentScan 이 없어
 * @RestController 만으로는 등록되지 않는다 — 빠지면 기동은 되고 호출만 404 다 (§5).
 */
@Tag(name = "카탈로그 — 운동", description = "보강 운동 상세. 회원별로 달라지지 않는 마스터 데이터다")
@RestController
@RequestMapping("/catalog/exercises")
class ExerciseController(
    private val exerciseQueryService: ExerciseQueryService,
) {
    @Operation(
        summary = "운동 상세 조회",
        description =
            "근육맵과 음성 큐를 포함한 운동 가이드 화면 전체를 한 번에 내린다. " +
                "칼로리는 회원 몸무게의 함수라 catalog 가 계산하지 않고 metValue 만 내린다. " +
                "재생 URL 과 썸네일은 아직 없다 — YMove 연동은 후속 작업이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "`EXERCISE_NOT_FOUND` — 운동을 찾을 수 없습니다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @GetMapping("/{exerciseId}")
    fun getExercise(
        @Parameter(description = "운동 식별자", example = "1")
        @PathVariable exerciseId: Long,
    ): ExerciseDetailResponse =
        ExerciseDetailResponse.from(
            exerciseQueryService.getDetail(ExerciseIdentity.of(exerciseId)),
        )
}
