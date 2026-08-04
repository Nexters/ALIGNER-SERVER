package team.aligner.member.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 닉네임 검증은 Member.changeProfile 이 한다.
 *
 * @Valid 를 쓰지 않는 것은 의도다. boot-mvc 에 starter-validation 이 없고, 닉네임 규칙은
 * 이미 도메인 불변식이라 두 곳에 두면 갈린다. 위반 시 InvalidNicknameException 이 400 으로
 * 매핑된다.
 */
@Schema(description = "프로필 수정 요청")
data class UpdateMemberProfileRequest(
    @field:Schema(
        description = "새 닉네임. 1자 이상 50자 이하이며 위반하면 400 INVALID_NICKNAME 이다",
        example = "강혁",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val nickname: String,
)
