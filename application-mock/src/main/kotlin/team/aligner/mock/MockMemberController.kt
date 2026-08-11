package team.aligner.mock

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.aligner.member.api.dto.MemberProfileResponse
import team.aligner.member.api.dto.UpdateMemberProfileRequest
import team.aligner.member.model.ExperienceLevel

/**
 * 온보딩을 마친 회원을 흉내낸다.
 *
 * **PATCH 가 저장하지 않는다.** 보낸 값을 그대로 되돌려주므로 화면에서는 수정된 것처럼
 * 보이지만 다음 GET 에는 반영되지 않는다. 목의 한계이고 연동 가이드에 적어둔다.
 */
@RestController
@RequestMapping("/members")
internal class MockMemberController {
    @GetMapping("/me")
    fun getMyProfile(): MemberProfileResponse = profile()

    @PatchMapping("/me")
    fun updateMyProfile(
        @RequestBody request: UpdateMemberProfileRequest,
    ): MemberProfileResponse =
        profile().copy(
            nickname = request.nickname ?: profile().nickname,
            heightCm = request.heightCm ?: profile().heightCm,
            weightKg = request.weightKg ?: profile().weightKg,
            experienceLevel = request.experienceLevel ?: profile().experienceLevel,
            reinforcementBodyPartCode = request.reinforcementBodyPartCode ?: profile().reinforcementBodyPartCode,
            reinforcementLevel = request.reinforcementLevel ?: profile().reinforcementLevel,
        )

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/me")
    fun withdraw() = Unit

    private fun profile() =
        MemberProfileResponse(
            memberId = MockFixtures.MEMBER_ID,
            nickname = "요가하는 사람",
            heightCm = 170,
            weightKg = 60,
            experienceLevel = ExperienceLevel.ONE_TO_THREE_YEARS,
            reinforcementBodyPartCode = "BACK",
            reinforcementLevel = 1,
        )
}
