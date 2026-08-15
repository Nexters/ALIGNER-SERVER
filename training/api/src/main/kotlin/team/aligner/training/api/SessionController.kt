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
import team.aligner.training.api.dto.AchievementResponse
import team.aligner.training.api.dto.CompleteSessionRequest
import team.aligner.training.api.dto.RecordPerceivedResultRequest
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
                "재시도로 들어온 호출에서는 `stampAcquired` 가 false 다. " +
                "`courseProgress` 에 완료 리포트가 쓰는 값이 다 들어 있다 — 헤더의 자세 이름·부위·난이도와 " +
                "**파이어로그 `acquiredStampCount / requiredStampCount`**(그 자세를 완주한 횟수)까지다. " +
                "자세를 방금 완성했는지는 `targetPoseCompleted && stampAcquired` 로 판단한다.",
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

    @Operation(
        summary = "핀포즈 직후 체감 기록",
        description =
            "\"오늘 파이어로그, 어땠어요?\" 화면의 3 지선다다. **기록만 한다** — `TOO_HARD` 를 보내도 " +
                "서버가 코스를 바꾸거나 자세를 내리지 않는다. 어떤 자세로 옮길지는 아직 정해지지 않았고, " +
                "지금은 화면이 이 값을 보고 부위·난이도 재선택(`POST /courses`)으로 보낸다. " +
                "**다시 답할 수 있다** — 잘못 누른 것을 고치지 못하게 막을 이유가 없다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "기록 성공. perceivedResult 가 채워진 세션이 돌아온다"),
            ApiResponse(
                responseCode = "404",
                description = "`SESSION_NOT_FOUND` — 없는 세션이거나 남의 세션이다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @PostMapping("/{sessionId}/perceived-result")
    fun recordPerceivedResult(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @Parameter(description = "세션 식별자", example = "100")
        @PathVariable sessionId: Long,
        @RequestBody request: RecordPerceivedResultRequest,
    ): SessionResponse =
        SessionResponse.from(
            sessionService.recordPerceivedResult(principal.memberId, sessionId, request.perceivedResult),
        )

    @Operation(
        summary = "연속 달성 현황",
        description =
            "운동 완료 리포트의 \"5일 연속 달성 중 · 이번 주 5 / 7\" 과 요일 체크다. " +
                "**날짜는 `Asia/Seoul` 기준**이고, 하루에 세션을 여러 번 해도 그날은 하루로 센다. " +
                "**오늘 아직 안 했어도 연속이 끊기지 않는다** — 어제까지 이어져 있으면 그 값을 유지한다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공. 완료한 세션이 없으면 0 일 · 전부 false 다")
    @GetMapping("/achievements")
    fun getAchievement(
        @AuthenticationPrincipal principal: AlignerPrincipal,
    ): AchievementResponse = AchievementResponse.from(sessionService.getAchievement(principal.memberId))
}

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 support-web 의 OpenApiConfig 가 등록한다.
 */
private const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
