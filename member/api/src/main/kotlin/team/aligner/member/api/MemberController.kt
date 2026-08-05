package team.aligner.member.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.aligner.member.api.dto.MemberProfileResponse
import team.aligner.member.api.dto.UpdateMemberProfileRequest
import team.aligner.member.model.MemberIdentity
import team.aligner.member.service.MemberCommandService
import team.aligner.member.service.MemberQueryService
import team.aligner.member.service.UpdateMemberProfileCommand
import team.aligner.support.web.AlignerPrincipal

/**
 * SecurityContext 에서 꺼낸 식별자를 MemberIdentity 로 바꿔 service 에 파라미터로 넘긴다.
 * service 시그니처에 Authentication·Principal 이 등장하면 잘못된 것이다
 * (docs/architecture.md §9).
 *
 * 이 클래스는 MemberApiAutoConfiguration 이 @Bean 으로 등록한다. ComponentScan 이 없어
 * @RestController 만으로는 등록되지 않는다 — 빠지면 기동은 되고 호출만 404 다 (§5).
 *
 * 401·500 은 문서에 적지 않는다. 모든 엔드포인트가 똑같이 내므로 OpenApiConfig 가 전역으로
 * 붙인다. 여기에는 이 도메인이 정하는 실패만 적는다.
 */
@Tag(name = "회원", description = "내 프로필 조회와 수정")
@RestController
@RequestMapping("/members")
class MemberController(
    private val memberCommandService: MemberCommandService,
    private val memberQueryService: MemberQueryService,
) {
    @Operation(
        summary = "내 프로필 조회",
        description = "토큰의 회원 식별자로 조회한다. 다른 회원을 조회하는 경로는 없다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "`MEMBER_NOT_FOUND` — 토큰은 유효하나 그 회원이 이미 삭제된 경우",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal principal: AlignerPrincipal,
    ): MemberProfileResponse =
        MemberProfileResponse.from(
            memberQueryService.getProfile(MemberIdentity.of(principal.memberId)),
        )

    @Operation(
        summary = "내 프로필 수정",
        description = "닉네임만 바꾼다. 프로필 이미지는 카카오가 소유하므로 서버가 수정하지 않는다. 수정된 프로필 전체를 돌려준다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공. 수정 직후 값을 그대로 돌려준다"),
            ApiResponse(
                responseCode = "400",
                description = "`INVALID_NICKNAME` — 닉네임은 1자 이상 50자 이하여야 합니다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "`MEMBER_NOT_FOUND` — 토큰은 유효하나 그 회원이 이미 삭제된 경우",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @PatchMapping("/me")
    fun updateMyProfile(
        @AuthenticationPrincipal principal: AlignerPrincipal,
        @RequestBody request: UpdateMemberProfileRequest,
    ): MemberProfileResponse =
        MemberProfileResponse.from(
            memberCommandService.updateProfile(
                MemberIdentity.of(principal.memberId),
                UpdateMemberProfileCommand(nickname = request.nickname),
            ),
        )
}

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 support-web 의 OpenApiConfig 가 등록한다.
 * 어노테이션 인자는 상수여야 해서 문자열로 둔다.
 */
private const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
