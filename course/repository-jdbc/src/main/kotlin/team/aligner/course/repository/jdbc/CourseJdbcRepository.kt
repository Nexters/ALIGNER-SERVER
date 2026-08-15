package team.aligner.course.repository.jdbc

import org.springframework.data.repository.CrudRepository

/**
 * @EnableJdbcRepositories 의 basePackageClasses 기준점이다.
 */
internal interface CourseJdbcRepository : CrudRepository<CourseEntity, Long> {
    /** 추천 멱등성 분기. 같은 회원이 같은 자세의 코스를 두 개 가질 수 없다. */
    fun findByMemberIdAndTargetPoseId(
        memberId: Long,
        targetPoseId: Long,
    ): CourseEntity?
}

// StampJdbcRepository 를 두지 않는다. 도장 저장은 ON CONFLICT DO NOTHING 한 문장이라
// CrudRepository 로는 표현할 수 없다 (StampRepositoryImpl).
