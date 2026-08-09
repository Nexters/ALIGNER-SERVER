package team.aligner.screening.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.aligner.screening.api.dto.BodyPartResponse
import team.aligner.screening.api.dto.ScreeningResultResponse
import team.aligner.screening.api.dto.SubmitScreeningRequest
import team.aligner.screening.service.ScreeningCommandService
import team.aligner.screening.service.ScreeningQueryService
import team.aligner.screening.service.SubmitScreeningCommand
import team.aligner.support.web.AlignerPrincipal

/**
 * SecurityContext 에서 꺼낸 식별자를 service 에 **파라미터로** 넘긴다. service 시그니처에
 * Authentication·Principal 이 등장하면 잘못된 것이다 (docs/architecture.md §9).
 *
 * 이 클래스는 ScreeningApiAutoConfiguration 이 @Bean 으로 등록한다. ComponentScan 이 없어
 * @RestController 만으로는 등록되지 않는다 — 빠지면 기동은 되고 호출만 404 다 (§5).
 *
 * 401·500 은 문서에 적지 않는다. 모든 엔드포인트가 똑같이 내므로 OpenApiConfig 가 전역으로
 * 붙인다. 여기에는 이 도메인이 정하는 실패만 적는다.
 */
@Tag(name = "자가 스크리닝", description = "자세 체감으로 원인을 판별한다")
@RestController
@RequestMapping("/screening")
class ScreeningController(
    private val screeningCommandService: ScreeningCommandService,
    private val screeningQueryService: ScreeningQueryService,
) {
    @Operation(
        summary = "부위 목록 조회",
        description =
            "진단 결과를 본 뒤 **강화할 부위를 고르는 화면**의 선택지다. 판별된 원인의 부위만이 아니라 " +
                "전체 부위를 내린다 — 회원은 분석 결과에 없는 부위도 고를 수 있다. " +
                "진단 제출(`POST /screening/results`)에는 부위를 넣지 않는다.",
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "조회 성공. 화면 노출 순서로 정렬된다")])
    @GetMapping("/body-parts")
    fun getBodyParts(): List<BodyPartResponse> = screeningQueryService.getBodyParts().map(BodyPartResponse::from)

    @Operation(
        summary = "자세 체감 제출과 원인 판별",
        description =
            "고른 자세와 체감을 넘기면 서버가 분기 규칙으로 원인을 판별해 저장하고 결과를 그대로 돌려준다. " +
                "**제출과 판별이 한 요청에서 끝난다.** 쉬웠던 자세와 어려웠던 자세를 각각 최대 4 개까지 담을 수 있고, " +
                "같은 자세를 두 번 넣을 수 없다. " +
                "**부위를 넣지 않는다** — 강화할 부위는 이 응답의 원인을 보고 다음 화면에서 고른다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "판별 성공. 원인이 rank 오름차순으로 실린다"),
            ApiResponse(
                responseCode = "400",
                description =
                    "`EMPTY_SCREENING_ANSWER` — 자세를 하나도 고르지 않았다 / " +
                        "`TOO_MANY_SCREENING_ANSWERS` — 한 체감에 4 개를 넘겼다 / " +
                        "`DUPLICATE_SCREENING_ANSWER` — 같은 자세를 두 번 넣었다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
            ApiResponse(
                responseCode = "422",
                description = "`CAUSE_NOT_DETERMINED` — 고른 자세 조합이 어떤 분기 규칙에도 걸리지 않았다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @PostMapping("/results")
    fun submit(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @RequestBody request: SubmitScreeningRequest,
    ): ScreeningResultResponse {
        val identity =
            screeningCommandService.submit(
                memberId = principal.memberId,
                command = SubmitScreeningCommand(answers = request.toAnswers()),
            )
        // Command 는 식별자만 돌려준다. 화면이 필요로 하는 원인 이름·설명은 마스터 seed 와의
        // 조인이라 Query 쪽 모델이다 (docs/architecture.md §4).
        return ScreeningResultResponse.from(
            screeningQueryService.getResult(memberId = principal.memberId, resultId = identity.value),
        )
    }

    @Operation(
        summary = "최신 진단 결과 조회",
        description = "회원이 가장 최근에 받은 진단이다. 진단한 적이 없으면 404 다 — 화면은 이때 온보딩으로 보낸다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "`SCREENING_RESULT_NOT_FOUND` — 아직 진단한 적이 없다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @GetMapping("/results/latest")
    fun getLatestResult(
        @AuthenticationPrincipal principal: AlignerPrincipal,
    ): ScreeningResultResponse = ScreeningResultResponse.from(screeningQueryService.getLatestResult(principal.memberId))
}

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 OpenApiConfig 가 등록한다.
 * 어노테이션 인자는 상수여야 해서 문자열로 둔다.
 */
private const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
