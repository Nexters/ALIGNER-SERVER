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
}
