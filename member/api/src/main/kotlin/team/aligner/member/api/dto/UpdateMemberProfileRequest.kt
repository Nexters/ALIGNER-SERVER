package team.aligner.member.api.dto

/**
 * 닉네임 검증은 Member.changeProfile 이 한다.
 *
 * @Valid 를 쓰지 않는 것은 의도다. boot-mvc 에 starter-validation 이 없고, 닉네임 규칙은
 * 이미 도메인 불변식이라 두 곳에 두면 갈린다. 위반 시 InvalidNicknameException 이 400 으로
 * 매핑된다.
 */
data class UpdateMemberProfileRequest(
    val nickname: String,
)
