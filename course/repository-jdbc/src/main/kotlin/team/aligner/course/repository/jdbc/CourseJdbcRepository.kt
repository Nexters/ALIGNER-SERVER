package team.aligner.course.repository.jdbc

import org.springframework.data.repository.CrudRepository

/**
 * @EnableJdbcRepositories 의 basePackageClasses 기준점이다.
 */
internal interface CourseJdbcRepository : CrudRepository<CourseEntity, Long> {
    /** 처방 멱등성 분기. 같은 회원이 같은 자세의 코스를 두 개 가질 수 없다. */
    fun findByMemberIdAndTargetPoseId(
        memberId: Long,
        targetPoseId: Long,
    ): CourseEntity?
}

internal interface StampJdbcRepository : CrudRepository<StampEntity, Long> {
    fun existsByMemberIdAndTargetPoseId(
        memberId: Long,
        targetPoseId: Long,
    ): Boolean
}
