package team.aligner.catalog.model

/**
 * 목표 자세 식별자. 원시 Long 이 파라미터 자리에서 섞이는 것을 막는다.
 *
 * screening 이 cause_rule 에 이 값을 값 컬럼으로 저장하지만 타입을 공유하지는 않는다
 * (docs/domains.md §4-2). 도메인 간에는 원시 Long 으로만 오간다.
 */
@JvmInline
value class TargetPoseIdentity private constructor(
    val value: Long,
) {
    companion object {
        fun of(value: Long): TargetPoseIdentity = TargetPoseIdentity(value)
    }
}
