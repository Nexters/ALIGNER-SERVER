package team.aligner.catalog.model

/**
 * 보강 운동 식별자. 원시 Long 이 파라미터 자리에서 섞이는 것을 막는다.
 *
 * course 와 training 은 contract 를 통해 원시 Long 으로 주고받는다 (docs/architecture.md §7).
 * 이 타입은 catalog 안에서만 쓴다.
 */
@JvmInline
value class ExerciseIdentity private constructor(
    val value: Long,
) {
    companion object {
        fun of(value: Long): ExerciseIdentity = ExerciseIdentity(value)
    }
}
