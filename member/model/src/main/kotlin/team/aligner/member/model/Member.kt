package team.aligner.member.model

import team.aligner.member.model.exception.InvalidNicknameException
import java.time.Instant

/**
 * 회원 애그리거트 루트.
 *
 * 가입과 수정의 검증이 다르다. 가입은 카카오가 준 값을 그대로 받고, 수정은 회원이 직접 입력한
 * 값이라 규칙을 건다. 카카오 닉네임이 우리 규칙에 안 맞는다고 가입을 막을 이유가 없기 때문이다.
 *
 * Spring Data JDBC 에는 더티체킹이 없다. changeProfile 은 새 인스턴스를 반환하고
 * 호출부가 save 를 명시한다 (docs/architecture.md §4).
 */
data class Member(
    val memberIdentity: MemberIdentity?,
    val kakaoId: String,
    val nickname: String?,
    val profileImageUrl: String?,
    val createdAt: Instant?,
) {
    /**
     * 회원이 직접 입력한 닉네임으로 바꾼다. 여기서만 닉네임 규칙을 검사한다.
     */
    fun changeProfile(nickname: String): Member {
        val trimmed = nickname.trim()
        if (trimmed.isEmpty() || trimmed.length > NICKNAME_MAX_LENGTH) {
            throw InvalidNicknameException()
        }
        return copy(nickname = trimmed)
    }

    companion object {
        /** member.member.nickname 이 VARCHAR(50) 이다. */
        const val NICKNAME_MAX_LENGTH = 50

        /**
         * 카카오 최초 로그인으로 가입한다.
         *
         * nickname 은 null 을 정상으로 받는다. 카카오 프로필 제공에 동의하지 않으면 오지 않는데,
         * 그때 기본 닉네임을 만들어 넣지 않는다 — 비워두고 회원이 직접 정하게 한다.
         *
         * 길이만 잘라서 담는다. 예외를 던지지 않는 것은 "카카오 값 때문에 가입을 막지 않는다"는
         * 결정 때문이고, 그냥 두지 않는 것은 nickname 컬럼이 VARCHAR(50) 이라 초과하면
         * DB 가 거부해 로그인이 500 으로 죽기 때문이다.
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
                createdAt = null,
            )
    }
}
