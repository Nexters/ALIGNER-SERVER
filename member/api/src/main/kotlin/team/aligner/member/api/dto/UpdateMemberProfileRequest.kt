package team.aligner.member.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import team.aligner.member.model.ExperienceLevel
import team.aligner.member.model.ReinforcementSetting
import team.aligner.member.model.exception.InvalidReinforcementSettingException
import team.aligner.member.service.UpdateMemberProfileCommand

/**
 * 입력 검증은 Member.changeProfile 과 ReinforcementSetting 이 한다.
 *
 * @Valid 를 쓰지 않는 것은 의도다. boot-mvc 에 starter-validation 이 없고, 입력 규칙은
 * 이미 도메인 불변식이라 두 곳에 두면 갈린다. 위반 시 400 으로 매핑된다.
 *
 * **모든 필드가 선택이다.** 온보딩이 화면마다 조각을 나눠 보내고, 프로필 편집은 바꾼 것만
 * 보낸다. 보내지 않은 필드는 그대로 유지된다 — null 을 보내도 지워지지 않는다.
 */
@Schema(description = "프로필 수정 요청. 보낸 필드만 바뀐다")
data class UpdateMemberProfileRequest(
    @field:Schema(
        description = "새 닉네임. 1자 이상 50자 이하이며 위반하면 400 INVALID_NICKNAME 이다",
        example = "강혁",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val nickname: String? = null,
    @field:Schema(description = "키(cm). 100 이상 250 이하", example = "170", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    val heightCm: Int? = null,
    @field:Schema(description = "몸무게(kg). 20 이상 300 이하", example = "60", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    val weightKg: Int? = null,
    @field:Schema(description = "운동 경력", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    val experienceLevel: ExperienceLevel? = null,
    @field:Schema(
        description = "강화 부위 코드. reinforcementLevel 과 **함께** 보내야 한다",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val reinforcementBodyPartCode: BodyPartCode? = null,
    @field:Schema(
        description = "강화 난이도 1(하)·2(중)·3(상). reinforcementBodyPartCode 와 **함께** 보내야 한다",
        example = "1",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val reinforcementLevel: Int? = null,
) {
    /**
     * 강화 설정은 한 화면에서 부위와 난이도를 같이 고르므로 둘을 한 값으로 묶는다.
     * 한쪽만 보내면 짝이 맞지 않는 요청이라 400 이다 — DB 의 ck_member_reinforcement_pair 와
     * 같은 규칙을 요청 경계에서 먼저 건다.
     */
    fun toCommand(): UpdateMemberProfileCommand =
        UpdateMemberProfileCommand(
            nickname = nickname,
            heightCm = heightCm,
            weightKg = weightKg,
            experienceLevel = experienceLevel,
            reinforcement = toReinforcement(),
        )

    private fun toReinforcement(): ReinforcementSetting? =
        when {
            reinforcementBodyPartCode == null && reinforcementLevel == null -> null
            reinforcementBodyPartCode != null && reinforcementLevel != null ->
                ReinforcementSetting(bodyPartCode = reinforcementBodyPartCode.name, level = reinforcementLevel)

            else -> throw InvalidReinforcementSettingException()
        }
}
