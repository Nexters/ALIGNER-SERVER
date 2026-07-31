package team.aligner.member.repository.jdbc

import team.aligner.member.infrastructure.MemberRepository
import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity
import java.time.Instant

/**
 * Entity ↔ Model 변환이 일어나는 유일한 자리다. 도메인은 Entity 를 모른다.
 *
 * 시각은 @EnableJdbcAuditing 대신 여기서 명시적으로 채운다. auditing 은 전역 설정이라
 * 도메인 모듈의 AutoConfiguration 에서 켜면 다른 도메인 4 개에 조용히 영향이 간다.
 */
internal class MemberRepositoryImpl(
    private val memberJdbcRepository: MemberJdbcRepository,
) : MemberRepository {
    override fun save(member: Member): Member {
        val now = Instant.now()
        val saved =
            memberJdbcRepository.save(
                MemberEntity(
                    memberId = member.memberIdentity?.value,
                    kakaoId = member.kakaoId,
                    nickname = member.nickname,
                    profileImageUrl = member.profileImageUrl,
                    // 신규는 지금, 기존은 원래 가입 시각을 그대로 둔다.
                    createdAt = member.createdAt ?: now,
                    updatedAt = now,
                ),
            )
        return saved.toModel()
    }

    override fun findByKakaoId(kakaoId: String): Member? = memberJdbcRepository.findByKakaoId(kakaoId)?.toModel()

    override fun findByMemberIdentity(memberIdentity: MemberIdentity): Member? =
        memberJdbcRepository.findById(memberIdentity.value).orElse(null)?.toModel()
}

private fun MemberEntity.toModel(): Member =
    Member(
        memberIdentity = memberId?.let { MemberIdentity.of(it) },
        kakaoId = kakaoId,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
        createdAt = createdAt,
    )
