package team.aligner.training.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.aligner.support.web.AlignerPrincipal
import team.aligner.training.api.dto.CompleteSessionRequest
import team.aligner.training.api.dto.SessionResponse
import team.aligner.training.api.dto.StartSessionRequest
import team.aligner.training.service.SessionService

/**
 * SecurityContext 에서 꺼낸 식별자를 service 에 **파라미터로** 넘긴다 (docs/architecture.md §9).
 *
 * 이 클래스는 TrainingApiAutoConfiguration 이 @Bean 으로 등록한다. ComponentScan 이 없어
 * @RestController 만으로는 등록되지 않는다 — 빠지면 기동은 되고 호출만 404 다 (§5).
 *
 * **휴식 타이머·±10초·이전/다음·음성 재생 전환에 API 를 두지 않는다.** 전부 클라이언트
 * 동작이고, 휴식 타이머는 와이어프레임에서 deprecated 처리됐다.
 */
@Tag(name = "세션", description = "코스 스텝 수행과 완료 기록")
@RestController
@RequestMapping("/sessions")
class SessionController(
    private val sessionService: SessionService,
) {
    @Operation(
        summary = "세션 시작",
        description =
            "코스 스텝 하나를 수행할 세션을 연다. 스텝 구성을 복사해 수행 기록의 뼈대를 만들어 두고, " +
                "완료 요청이 그 값을 채운다. **이미 완료한 스텝으로도 다시 시작할 수 있다.**",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "시작 성공. 수행 기록이 전부 completed=false 로 실린다"),
            ApiResponse(
                responseCode = "404",
                description = "`COURSE_STEP_NOT_FOUND` — 없는 코스이거나 없는 스텝이다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
            ApiResponse(
                responseCode = "422",
                description = "`EMPTY_COURSE_STEP` — 스텝에 운동이 편성돼 있지 않다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun start(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @RequestBody request: StartSessionRequest,
    ): SessionResponse = SessionResponse.from(sessionService.start(principal.memberId, request.toCommand()))

    @Operation(
        summary = "세션 조회",
        description =
            "세션 복구에 쓴다. 앱이 죽었다 돌아오면 이 API 로 현재 상태를 다시 그린다. " +
                "응답 형태가 시작·완료와 같다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "`SESSION_NOT_FOUND` — 없는 세션이거나 남의 세션이다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @GetMapping("/{sessionId}")
    fun getSession(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @Parameter(description = "세션 식별자", example = "100")
        @PathVariable sessionId: Long,
    ): SessionResponse = SessionResponse.from(sessionService.getSession(principal.memberId, sessionId))

    @Operation(
        summary = "세션 완료",
        description =
            "수행 결과를 저장하고 **코스 진행도에 반영한다.** 응답의 `courseProgress` 가 반영 결과다. " +
                "요청에 없는 운동은 수행하지 않은 것으로 남는다 — 부분 완료가 정상이다. " +
                "**멱등하다.** 같은 요청이 재시도돼도 진행도가 두 번 오르지 않고 도장도 한 번만 붙는다. " +
                "재시도로 들어온 호출에서는 `stampAcquired` 가 false 다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "완료 성공. courseProgress 가 함께 실린다"),
            ApiResponse(
                responseCode = "400",
                description = "`UNKNOWN_EXERCISE_RECORD` — 이 세션에 없는 courseStepExerciseId 가 섞였다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "`SESSION_NOT_FOUND` — 없는 세션이거나 남의 세션이다 / `COURSE_NOT_FOUND` — 코스가 사라졌다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @PostMapping("/{sessionId}/complete")
    fun complete(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @Parameter(description = "세션 식별자", example = "100")
        @PathVariable sessionId: Long,
        @RequestBody request: CompleteSessionRequest,
    ): SessionResponse = SessionResponse.from(sessionService.complete(principal.memberId, sessionId, request.toCommand()))
}

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 support-web 의 OpenApiConfig 가 등록한다.
 */
private const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
