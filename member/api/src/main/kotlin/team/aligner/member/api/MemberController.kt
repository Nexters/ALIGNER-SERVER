package team.aligner.member.api

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
 */
@RestController
@RequestMapping("/members")
class MemberController(
    private val memberCommandService: MemberCommandService,
    private val memberQueryService: MemberQueryService,
) {
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal principal: AlignerPrincipal,
    ): MemberProfileResponse =
        MemberProfileResponse.from(
            memberQueryService.getProfile(MemberIdentity.of(principal.memberId)),
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
