package team.aligner.screening.infrastructure

import team.aligner.screening.model.CauseRule

/**
 * 분기표 조회 out-port. 판별의 입력이다.
 *
 * 회원이 고른 자세로 좁혀서 읽는다. 전량을 읽지 않는 것은 seed 가 자세 32 개 × 체감 2 종 ×
 * 원인 여러 개로 불어나기 때문이고, 응답에 없는 자세의 규칙은 어차피 버려진다.
 */
interface CauseRuleRepository {
    fun findAllByTargetPoseIds(targetPoseIds: Collection<Long>): List<CauseRule>
}
