package team.aligner.member.model

import team.aligner.member.model.exception.InvalidHeightException
import team.aligner.member.model.exception.InvalidNicknameException
import team.aligner.member.model.exception.InvalidWeightException
import java.time.Instant

/**
 * 회원 애그리거트 루트.
 *
 * 가입과 수정의 검증이 다르다. 가입은 카카오가 준 값을 그대로 받고, 수정은 회원이 직접 입력한
 * 값이라 규칙을 건다. 카카오 닉네임이 우리 규칙에 안 맞는다고 가입을 막을 이유가 없기 때문이다.
 *
 * Spring Data JDBC 에는 더티체킹이 없다. changeProfile·withdraw 는 새 인스턴스를 반환하고
 * 호출부가 save 를 명시한다 (docs/architecture.md §4).
 */
data class Member(
    val memberIdentity: MemberIdentity?,
    /** 탈퇴하면 null 이 된다. [withdraw] 참고. */
    val kakaoId: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val heightCm: Int?,
    val weightKg: Int?,
    val experienceLevel: ExperienceLevel?,
    val reinforcement: ReinforcementSetting?,
    val withdrawnAt: Instant?,
    val createdAt: Instant?,
) {
    /**
     * 회원이 직접 입력한 값으로 바꾼다. 여기서만 입력 규칙을 검사한다.
     *
     * **null 은 "바꾸지 않는다" 는 뜻이다.** 값을 비우는 수단이 아니다. 온보딩이 경력 화면,
     * 키·몸무게 화면, 강화 설정 화면으로 나뉘어 있어 한 번에 한 조각씩 PATCH 하기 때문이고,
     * 화면 어디에도 입력값을 되비우는 동작이 없기 때문이다.
     *
     * 비우기가 필요해지면 그때 "없음" 을 표현하는 방법을 따로 만든다. 지금 넣으면 요청 DTO 가
     * 삼상태(미전송·null·값)를 표현해야 해서 계약이 한 단계 복잡해진다.
     */
    fun changeProfile(
        nickname: String? = null,
        heightCm: Int? = null,
        weightKg: Int? = null,
        experienceLevel: ExperienceLevel? = null,
        reinforcement: ReinforcementSetting? = null,
    ): Member {
        val changedNickname = nickname?.let(::validateNickname) ?: this.nickname
        heightCm?.let(::validateHeight)
        weightKg?.let(::validateWeight)

        return copy(
            nickname = changedNickname,
            heightCm = heightCm ?: this.heightCm,
            weightKg = weightKg ?: this.weightKg,
            experienceLevel = experienceLevel ?: this.experienceLevel,
            reinforcement = reinforcement ?: this.reinforcement,
        )
    }

    /**
     * 탈퇴. **행을 지우지 않는다.**
     *
     * 운동 기록을 보존하기로 했고 그 기록이 `member_id` 로 붙어 있다. 남는 개인정보가
     * 카카오 식별자뿐이라 그것만 지운다. 닉네임·프로필 이미지도 카카오에서 온 값이지만
     * 회원이 우리 서비스에서 바꿀 수 있는 표시용 값이라 함께 지우지 않는다.
     *
     * `kakaoId` 가 비면 UNIQUE 자리가 풀려 **같은 카카오 계정이 다시 가입할 수 있다.** 그때는
     * 새 `member_id` 를 받으므로 이전 기록이 이어지지 않는다. 기록 보존은 우리 쪽 보관이고
     * 회원에게 되돌려주는 복구가 아니다.
     */
    fun withdraw(at: Instant): Member = copy(kakaoId = null, withdrawnAt = at)

    private fun validateNickname(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > NICKNAME_MAX_LENGTH) {
            throw InvalidNicknameException()
        }
        return trimmed
    }

    private fun validateHeight(value: Int) {
        if (value !in MIN_HEIGHT_CM..MAX_HEIGHT_CM) {
            throw InvalidHeightException()
        }
    }

    private fun validateWeight(value: Int) {
        if (value !in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            throw InvalidWeightException()
        }
    }

    companion object {
        /** member.member.nickname 이 VARCHAR(50) 이다. */
        const val NICKNAME_MAX_LENGTH = 50

        // DDL 의 ck_member_height_cm · ck_member_weight_kg 와 같은 범위여야 한다.
        const val MIN_HEIGHT_CM = 100
        const val MAX_HEIGHT_CM = 250
        const val MIN_WEIGHT_KG = 20
        const val MAX_WEIGHT_KG = 300

        /**
         * 카카오 최초 로그인으로 가입한다.
         *
         * nickname 은 null 을 정상으로 받는다. 카카오 프로필 제공에 동의하지 않으면 오지 않는데,
         * 그때 기본 닉네임을 만들어 넣지 않는다 — 비워두고 회원이 직접 정하게 한다.
         *
         * 길이만 잘라서 담는다. 예외를 던지지 않는 것은 "카카오 값 때문에 가입을 막지 않는다"는
         * 결정 때문이고, 그냥 두지 않는 것은 nickname 컬럼이 VARCHAR(50) 이라 초과하면
         * DB 가 거부해 로그인이 500 으로 죽기 때문이다.
         *
         * 신체 정보와 강화 설정은 비어 있다. 온보딩이 가입 뒤에 화면마다 나눠서 채운다.
         */
        fun register(
            kakaoId: String,
            nickname: String?,
            profileImageUrl: String?,
        ): Member =
            Member(
                memberIdentity = null,
                kakaoId = kakaoId,
                nickname = nickname?.take(NICKNAME_MAX_LENGTH),
                profileImageUrl = profileImageUrl,
                heightCm = null,
                weightKg = null,
                experienceLevel = null,
                reinforcement = null,
                withdrawnAt = null,
                createdAt = null,
            )
    }
}
