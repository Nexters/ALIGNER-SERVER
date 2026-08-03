package team.aligner.member.api.dto

import team.aligner.member.model.Member
import team.aligner.member.model.view.MemberProfileView

/**
 * nickname·profileImageUrl 은 null 로 내려갈 수 있다. 카카오 프로필 제공에 동의하지 않은
 * 회원이며, 서버가 기본값을 만들어 채우지 않는다.
 */
data class MemberProfileResponse(
    val memberId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(view: MemberProfileView): MemberProfileResponse =
            MemberProfileResponse(
                memberId = view.memberId,
                nickname = view.nickname,
                profileImageUrl = view.profileImageUrl,
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
                profileImageUrl = member.profileImageUrl,
            )
        }
    }
}
