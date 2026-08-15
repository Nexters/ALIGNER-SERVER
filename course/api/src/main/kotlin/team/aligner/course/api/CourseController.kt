package team.aligner.course.api

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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.aligner.course.api.dto.CourseDetailResponse
import team.aligner.course.api.dto.RecommendCourseRequest
import team.aligner.course.api.dto.RecommendCourseResponse
import team.aligner.course.api.dto.TargetPoseProgressResponse
import team.aligner.course.api.dto.TodayCourseResponse
import team.aligner.course.service.CourseCommandService
import team.aligner.course.service.CourseQueryService
import team.aligner.course.service.RecommendCourseCommand
import team.aligner.support.web.AlignerPrincipal

/**
 * SecurityContext 에서 꺼낸 식별자를 service 에 **파라미터로** 넘긴다. service 시그니처에
 * Authentication·Principal 이 등장하면 잘못된 것이다 (docs/architecture.md §9).
 *
 * 이 클래스는 CourseApiAutoConfiguration 이 @Bean 으로 등록한다. ComponentScan 이 없어
 * @RestController 만으로는 등록되지 않는다 — 빠지면 기동은 되고 호출만 404 다 (§5).
 *
 * 401·500 은 문서에 적지 않는다. 모든 엔드포인트가 똑같이 내므로 OpenApiConfig 가 전역으로
 * 붙인다. 여기에는 이 도메인이 정하는 실패만 적는다.
 */
@Tag(name = "코스", description = "코스 추천과 오늘의 코스·진행도")
@RestController
@RequestMapping("/courses")
class CourseController(
    private val courseCommandService: CourseCommandService,
    private val courseQueryService: CourseQueryService,
) {
    @Operation(
        summary = "코스 추천",
        description =
            "강화할 부위와 난이도로 코스를 만든다. **난이도가 곧 목표 자세의 레벨**이고 자세 하나가 코스 하나다. " +
                "자세 식별자와 원인 코드를 요청에 넣지 않는다 — 자세는 서버가 catalog 에서 찾고, " +
                "원인은 서버가 최신 진단에서 찾아 스냅샷으로 남긴다. " +
                "**진단 결과에 없는 부위도 받는다.** 코스는 추천이라 회원이 「자세 도전 현황」에서 아무 자세나 " +
                "골라 시작할 수 있고, 그 경우 원인 스냅샷만 비어 있다. " +
                "**멱등하다.** 같은 자세의 코스가 이미 있으면 새로 만들지 않고 그 코스를 돌려준다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "추천 성공. 이미 있던 코스를 돌려준 경우도 201 이다"),
            ApiResponse(
                responseCode = "409",
                description = "`SCREENING_REQUIRED` — 아직 진단한 적이 없다. **온보딩으로 보낸다**",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
            ApiResponse(
                responseCode = "422",
                description =
                    "`COURSE_TEMPLATE_NOT_FOUND` — 그 부위·난이도의 자세나 템플릿 seed 가 없다 / " +
                        "`EMPTY_COURSE_TEMPLATE` — 템플릿에 스텝이 없다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun recommend(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @RequestBody request: RecommendCourseRequest,
    ): RecommendCourseResponse {
        val identity =
            courseCommandService.recommend(
                memberId = principal.memberId,
                command = RecommendCourseCommand(bodyPartCode = request.bodyPartCode.name, level = request.level),
            )
        return RecommendCourseResponse(courseId = identity.value)
    }

    @Operation(
        summary = "오늘의 코스",
        description =
            "홈 카드다. **\"오늘의 코스\" 는 진행 중인 코스의 다른 이름**이고 일자 개념이 없다. " +
                "진행 중인 코스가 여럿이면 가장 최근에 추천된 것이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "`IN_PROGRESS_COURSE_NOT_FOUND` — 진행 중인 코스가 없다. 화면은 추천으로 보낸다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @GetMapping("/today")
    fun getTodayCourse(
        @AuthenticationPrincipal principal: AlignerPrincipal,
    ): TodayCourseResponse = TodayCourseResponse.from(courseQueryService.getTodayCourse(principal.memberId))

    @Operation(
        summary = "자세 도전 현황",
        description =
            "**서비스가 제공하는 핀포즈 전체**가 나온다. 회원이 시작한 코스만이 아니다 — 코스는 추천이라 " +
                "아직 시작하지 않은 자세도 목록에 있고, 그 경우 `courseId` · `completedStepCount` · " +
                "`totalStepCount` 가 **null** 이다 (`0 / 4` 가 아니다). " +
                "**`completedStepCount / totalStepCount` 가 화면의 `3 / 4`** 이고 코스 안에서 완료한 스텝 개수다. " +
                "루트의 집계 셋은 `completed` 필터와 무관하게 언제나 전체 기준이라 칩 세 개를 한 번에 그릴 수 있다. " +
                "`completed=true` 로 거르면 프로필의 \"완수한 자세 목록\" 이 된다 — 별도 API 를 만들지 않는다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공. 자세 seed 가 없으면 targetPoses 가 빈 배열이고 집계는 0 이다")
    @GetMapping("/progress/target-poses")
    fun getTargetPoseProgress(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @Parameter(description = "true 면 완성한 자세만, false 면 완성하지 않은 자세만. 생략하면 전체다. 집계는 이 값과 무관하다", example = "true")
        @RequestParam(required = false) completed: Boolean?,
    ): TargetPoseProgressResponse =
        TargetPoseProgressResponse.from(
            courseQueryService.getTargetPoseProgress(memberId = principal.memberId, completedOnly = completed),
        )

    @Operation(
        summary = "코스 개요",
        description = "스텝과 그 스텝의 운동을 함께 내린다. 수행 시간·세트는 catalog 기본값까지 반영한 값이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "`COURSE_NOT_FOUND` — 없는 코스이거나 남의 코스다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @GetMapping("/{courseId}")
    fun getCourseDetail(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @Parameter(description = "코스 식별자", example = "20")
        @PathVariable courseId: Long,
    ): CourseDetailResponse =
        CourseDetailResponse.from(
            courseQueryService.getCourseDetail(memberId = principal.memberId, courseId = courseId),
        )
}

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 support-web 의 OpenApiConfig 가 등록한다.
 * 어노테이션 인자는 상수여야 해서 문자열로 둔다.
 */
private const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
