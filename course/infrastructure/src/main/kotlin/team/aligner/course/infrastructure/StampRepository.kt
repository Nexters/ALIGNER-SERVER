package team.aligner.course.infrastructure

import team.aligner.course.model.Stamp

/**
 * 도장 쓰기 out-port.
 *
 * **한 번의 DB 연산으로 끝낸다.** "있는지 보고 없으면 넣는다" 로 짜면 두 요청이 확인을 함께
 * 통과해 둘 다 INSERT 하고, `(member_id, target_pose_id)` 유니크 제약에 걸린다. 세션 완료
 * push 는 재시도되는 경로라 실제로 겹칠 수 있고, 그때 정상 재시도가 500 이 되면 멱등성
 * 계약이 깨진다 (docs/domains.md §7-8).
 *
 * **새로 붙었는지를 저장 결과가 알려준다.** 서비스가 "코스가 방금 완료됐나" 로 짐작하면
 * 경쟁 상황에서 두 요청이 모두 획득으로 판단할 수 있다.
 */
interface StampRepository {
    /** 새로 저장했으면 true, 이미 있었으면 false 다. */
    fun saveIfAbsent(stamp: Stamp): Boolean
}
