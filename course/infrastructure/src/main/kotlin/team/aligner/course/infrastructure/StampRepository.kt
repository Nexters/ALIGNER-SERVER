package team.aligner.course.infrastructure

import team.aligner.course.model.Stamp

/**
 * 도장 쓰기 out-port.
 *
 * save 가 **이미 있으면 아무것도 하지 않는다.** 세션 완료 push 가 재시도돼도 도장이 두 번
 * 붙지 않아야 한다 (docs/domains.md §7-8). 유니크 제약이 DB 에도 있지만 거기까지 가면
 * 제약 위반이 500 으로 나가므로 port 가 먼저 흡수한다.
 */
interface StampRepository {
    fun saveIfAbsent(stamp: Stamp)
}
