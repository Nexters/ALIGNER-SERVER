package team.aligner.training.model

/**
 * 세션 식별자. 원시 Long 이 파라미터 자리에서 섞이는 것을 막는다.
 */
@JvmInline
value class SessionIdentity private constructor(
    val value: Long,
) {
    companion object {
        fun of(value: Long): SessionIdentity = SessionIdentity(value)
    }
}
