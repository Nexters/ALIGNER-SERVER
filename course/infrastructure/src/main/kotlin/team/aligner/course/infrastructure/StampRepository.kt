package team.aligner.course.infrastructure

import team.aligner.course.model.Stamp

/**
 * 도장 쓰기 out-port.
 *
 * **한 번의 DB 연산으로 끝낸다.** "있는지 보고 없으면 넣는다" 로 짜면 두 요청이 확인을 함께
 * 통과해 둘 다 INSERT 하고, `(member_id, target_pose_id, attempt_no)` 유니크 제약에 걸린다.
 * 세션 완료 push 는 재시도되는 경로라 실제로 겹칠 수 있고, 그때 정상 재시도가 500 이 되면
 * 멱등성 계약이 깨진다 (docs/domains.md §7-8).
 *
 * **새로 붙었는지를 저장 결과가 알려준다.** 서비스가 "코스가 방금 완료됐나" 로 짐작하면
 * 경쟁 상황에서 두 요청이 모두 획득으로 판단할 수 있다.
 */
interface StampRepository {
    /** 새로 저장했으면 true, 이미 있었으면 false 다. */
    fun saveIfAbsent(stamp: Stamp): Boolean

    /**
     * 이 자세에 지금까지 붙은 도장 수. 완료 리포트의 **"파이어로그 N / 4회"** 이자 재도전을
     * 더 열어줄지의 판단 근거다.
     *
     * 세어서 돌려주는 것이지 애그리거트를 돌려주지 않지만 쓰기 port 에 함께 둔다. 완주 판정과
     * 같은 트랜잭션 안에서 쓰는 값이라 조회 모델로 분리하면 두 경로가 서로 다른 시점을 본다.
     */
    fun countAcquired(
        memberId: Long,
        targetPoseId: Long,
    ): Int
}
