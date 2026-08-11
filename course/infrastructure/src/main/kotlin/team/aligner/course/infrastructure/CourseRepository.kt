package team.aligner.course.infrastructure

import team.aligner.course.model.Course
import team.aligner.course.model.CourseIdentity

/**
 * 쓰기 out-port. 애그리거트 단위로만 오간다 (docs/architecture.md §4).
 *
 * findByMemberIdAndTargetPoseId 가 조회처럼 보이지만 여기 있다. 처방 멱등성 분기에서
 * 애그리거트가 필요하고 반환 타입이 Course 이기 때문이다 — Command/Query 의 기준은
 * "무엇을 반환하는가" 다 (member 의 findByKakaoId 와 같다).
 */
interface CourseRepository {
    fun save(course: Course): Course

    fun findByIdentity(courseIdentity: CourseIdentity): Course?

    /** 처방 멱등성. 같은 회원이 같은 자세의 코스를 두 개 가질 수 없다. */
    fun findByMemberIdAndTargetPoseId(
        memberId: Long,
        targetPoseId: Long,
    ): Course?
}
