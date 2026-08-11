package team.aligner.course.model

/**
 * 코스 식별자. 원시 Long 이 파라미터 자리에서 섞이는 것을 막는다.
 *
 * `memberId` `targetPoseId` `templateId` 와 나란히 놓이는 자리가 많아 감싸는 값이 특히 필요하다.
 */
@JvmInline
value class CourseIdentity private constructor(
    val value: Long,
) {
    companion object {
        fun of(value: Long): CourseIdentity = CourseIdentity(value)
    }
}
