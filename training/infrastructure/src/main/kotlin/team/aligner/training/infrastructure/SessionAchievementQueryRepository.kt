package team.aligner.training.infrastructure

import java.time.LocalDate

/**
 * 연속 달성 계산에 쓰는 조회 out-port. `training/repository-jdbc` 가 구현한다.
 *
 * **날짜만 돌려준다.** 세션 애그리거트를 통째로 읽으면 수행 기록까지 딸려 오는데, 연속
 * 달성은 "그날 완료한 세션이 하나라도 있었나" 만 필요하다. 쓰기 out-port(`SessionRepository`)와
 * 나눠 두는 것은 Query 가 조회 모델에 직결한다는 규칙 때문이다 (docs/architecture.md §4).
 */
interface SessionAchievementQueryRepository {
    /**
     * `from` 이후에 세션을 완료한 날짜. **회원이 사는 날짜(`Asia/Seoul`)로 접어서** 돌려준다.
     *
     * 중복은 없다. 하루에 세션을 여러 번 해도 그날은 하루다.
     */
    fun findCompletedDates(
        memberId: Long,
        from: LocalDate,
    ): List<LocalDate>
}
