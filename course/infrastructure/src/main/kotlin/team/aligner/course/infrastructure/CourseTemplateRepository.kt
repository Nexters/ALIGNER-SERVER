package team.aligner.course.infrastructure

import team.aligner.course.model.CourseTemplate

/**
 * 템플릿 마스터를 읽는 out-port. seed 라 쓰기가 없다.
 *
 * 조회처럼 보이지만 Query 리포지토리에 두지 않는다. 이 호출은 **추천 흐름의 입력**이고
 * Command 가 조회 모델을 끌어 쓰기 시작하면 경계가 흐려진다 (screening 의 BodyPartRepository
 * 와 같은 판단).
 */
interface CourseTemplateRepository {
    fun findByTargetPoseId(targetPoseId: Long): CourseTemplate?

    /**
     * 운영 목록 화면이 쓰는 전체 조회.
     *
     * 추천 흐름의 입력이 아니라 순수한 조회지만 여기 둔다. 템플릿을 읽는 SQL 이 이미 이 구현체에
     * 있고, 같은 테이블을 Query 리포지토리에서 한 번 더 읽으면 컬럼이 늘 때 고칠 자리가 둘이 된다.
     * 템플릿은 seed 라 쓰기가 없어 Command/Query 를 가를 실익도 없다 (docs/architecture.md §4).
     *
     * 페이징을 두지 않는다. 템플릿은 핀포즈 하나에 하나라 상한이 자세 개수(현재 9)다.
     */
    fun findAll(): List<CourseTemplate>
}
