package team.aligner.catalog.api

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
 * 목록 엔드포인트를 만들지 않는다. 운동 전체를 그리는 화면이 없다 (docs/architecture.md §4).
 *
 * 이 클래스는 CatalogApiAutoConfiguration 이 @Bean 으로 등록한다. ComponentScan 이 없어
 * @RestController 만으로는 등록되지 않는다 — 빠지면 기동은 되고 호출만 404 다 (§5).
 */
@RestController
@RequestMapping("/catalog/exercises")
class ExerciseController(
    private val exerciseQueryService: ExerciseQueryService,
) {
    @GetMapping("/{exerciseId}")
    fun getExercise(
        @PathVariable exerciseId: Long,
    ): ExerciseDetailResponse =
        ExerciseDetailResponse.from(
            exerciseQueryService.getDetail(ExerciseIdentity.of(exerciseId)),
        )
}
