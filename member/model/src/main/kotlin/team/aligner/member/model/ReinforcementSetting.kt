package team.aligner.member.model

import team.aligner.member.model.exception.InvalidReinforcementSettingException

/**
 * 회원이 고른 **강화 부위와 난이도**.
 *
 * 진단 결과를 본 뒤 고르고, 마이페이지의 "난이도 조정하기" 로 언제든 바뀐다. 코스 처방
 * 시점의 일회성 입력이 아니라 지속되는 설정이라 `member` 가 갖는다 (docs/domains.md §4-1).
 *
 * **둘을 한 타입으로 묶는다.** 한 화면에서 같이 고르므로 부위만 있고 난이도가 없는 상태가
 * 있을 수 없다. DDL 의 `ck_member_reinforcement_pair` 가 같은 것을 DB 에서 막는다.
 *
 * `bodyPartCode` 는 `screening` 소유 어휘를 값으로 받는다. 값 집합 검증은 하지 않는다 —
 * 하려면 `member → screening` 의존이 생기는데 그 방향은 docs/domains.md §3 에 없다.
 */
data class ReinforcementSetting(
    val bodyPartCode: String,
    val level: Int,
) {
    init {
        if (bodyPartCode.isBlank() || level !in MIN_LEVEL..MAX_LEVEL) {
            throw InvalidReinforcementSettingException()
        }
    }

    companion object {
        /** 하 */
        const val MIN_LEVEL = 1

        /** 상 */
        const val MAX_LEVEL = 3
    }
}
