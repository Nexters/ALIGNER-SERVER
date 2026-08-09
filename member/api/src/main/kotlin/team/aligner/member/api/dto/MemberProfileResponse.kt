package team.aligner.member.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.member.model.ExperienceLevel
import team.aligner.member.model.Member
import team.aligner.member.model.view.MemberProfileView

/**
 * nickname 은 null 로 내려갈 수 있다. 카카오 프로필 제공에 동의하지 않은 회원이며,
 * 서버가 기본값을 만들어 채우지 않는다.
 *
 * 신체 정보·경력·강화 설정도 null 로 내려갈 수 있다. **온보딩을 끝내지 않은 회원**이며,
 * 프론트는 이 null 들을 보고 온보딩으로 보낼지 판단한다.
 *
 * **프로필 이미지는 내려보내지 않는다.** 카카오에서 받아 `member.profile_image_url` 에
 * 저장은 계속하지만 응답에서는 뺐다. 화면이 쓰지 않는 값을 내보내면 프론트가 그 URL 에
 * 의존하기 시작하고, 카카오 CDN 링크는 우리가 수명을 보장할 수 없다.
 * 다시 필요해지면 View 와 컬럼이 그대로 있으므로 이 클래스에 필드만 되살리면 된다.
 */
@Schema(description = "회원 프로필")
data class MemberProfileResponse(
    @field:Schema(description = "회원 식별자", example = "1")
    val memberId: Long,
    @field:Schema(description = "닉네임. 카카오 프로필 제공에 동의하지 않으면 null 이다", example = "강혁", nullable = true)
    val nickname: String?,
    @field:Schema(description = "키(cm). 온보딩 전이면 null 이다", example = "170", nullable = true)
    val heightCm: Int?,
    @field:Schema(description = "몸무게(kg). 온보딩 전이면 null 이다", example = "60", nullable = true)
    val weightKg: Int?,
    @field:Schema(description = "운동 경력. 온보딩 전이면 null 이다", nullable = true)
    val experienceLevel: ExperienceLevel?,
    @field:Schema(
        description = "회원이 고른 강화 부위. 진단 결과 뒤에 고른다. 고르기 전이면 null 이다",
        example = "BACK",
        nullable = true,
    )
    val reinforcementBodyPartCode: String?,
    @field:Schema(description = "강화 난이도. 1(하)·2(중)·3(상). 고르기 전이면 null 이다", example = "1", nullable = true)
    val reinforcementLevel: Int?,
) {
    companion object {
        fun from(view: MemberProfileView): MemberProfileResponse =
            MemberProfileResponse(
                memberId = view.memberId,
                nickname = view.nickname,
                heightCm = view.heightCm,
                weightKg = view.weightKg,
                experienceLevel = view.experienceLevel,
                reinforcementBodyPartCode = view.reinforcementBodyPartCode,
                reinforcementLevel = view.reinforcementLevel,
            )

        /**
         * 수정 직후에는 방금 저장한 애그리거트를 그대로 응답한다. 다시 조회하지 않는다.
         */
        fun from(member: Member): MemberProfileResponse {
            val memberIdentity =
                requireNotNull(member.memberIdentity) {
                    "저장된 회원에 식별자가 없다"
                }
            return MemberProfileResponse(
                memberId = memberIdentity.value,
                nickname = member.nickname,
                heightCm = member.heightCm,
                weightKg = member.weightKg,
                experienceLevel = member.experienceLevel,
                reinforcementBodyPartCode = member.reinforcement?.bodyPartCode,
                reinforcementLevel = member.reinforcement?.level,
            )
        }
    }
}
