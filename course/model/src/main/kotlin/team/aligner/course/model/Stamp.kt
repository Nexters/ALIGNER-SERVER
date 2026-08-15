package team.aligner.course.model

import java.time.Instant

/**
 * 코스를 한 번 완주할 때마다 붙는 도장. 완료 리포트의 **"파이어로그 1 / 4회"** 가 이 개수다.
 *
 * **자세당 하나가 아니다.** 같은 자세의 코스를 다시 완주하면 회차가 하나 올라간 도장이 또
 * 붙고, [REQUIRED_COUNT] 개를 채우면 그 자세를 완성한 것이다.
 *
 * `training` 이 아니라 `course` 가 소유한다. 기록은 training, **판단은 course** 다
 * (docs/domains.md §2).
 */
data class Stamp(
    val identity: Long?,
    val memberId: Long,
    val targetPoseId: Long,
    val courseId: Long,
    /** 몇 번째 완주인가. 같은 회차의 완료 push 가 재시도돼도 도장은 하나다. */
    val attemptNo: Int,
    val acquiredAt: Instant?,
) {
    init {
        // 회차가 0 이하면 유니크 키가 뜻을 잃고 도장 집계도 어긋난다. 회원 입력이 아니라
        // 코스의 회차를 그대로 받는 값이라, 여기 걸리면 호출부 버그다.
        require(attemptNo > 0) { "도장의 회차는 1 이상이어야 한다: $attemptNo" }
    }

    companion object {
        /**
         * 자세 하나를 완성하는 데 필요한 도장 수. 완료 리포트의 세그먼트 개수이자 도전 현황
         * `3 / 4` 의 분모다.
         *
         * **감수 데이터가 아니라 화면 규칙이라 코드에 둔다.** 스크리닝 문항·원인 매핑처럼
         * 요가 지도자 감수를 거치는 값이 아니고, 바뀌면 changeset 이 아니라 이 상수만 고치는
         * 편이 맞다 (docs/domains.md §4-2 의 "난이도별 최대 4 개" 와 같은 판단).
         */
        const val REQUIRED_COUNT = 4

        fun acquire(
            memberId: Long,
            targetPoseId: Long,
            courseId: Long,
            attemptNo: Int,
            at: Instant,
        ): Stamp =
            Stamp(
                identity = null,
                memberId = memberId,
                targetPoseId = targetPoseId,
                courseId = courseId,
                attemptNo = attemptNo,
                acquiredAt = at,
            )
    }
}
