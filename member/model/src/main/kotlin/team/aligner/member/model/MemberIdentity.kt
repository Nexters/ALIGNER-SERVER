package team.aligner.member.model

/**
 * 회원 식별자. 원시 Long 이 파라미터 자리에서 섞이는 것을 막는다.
 *
 * api 가 SecurityContext 에서 꺼낸 값을 이 타입으로 감싸 service 에 넘긴다
 * (docs/architecture.md §9).
 */
@JvmInline
value class MemberIdentity private constructor(
    val value: Long,
) {
    companion object {
        fun of(value: Long): MemberIdentity = MemberIdentity(value)
    }
}
