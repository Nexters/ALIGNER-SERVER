package team.aligner.screening.contract

/**
 * `course` 가 코스를 처방할 때 쓰는 계약. 통합 전용으로 좁게 만든다.
 *
 * 식별자를 원시 타입으로 받고 자기 발행 DTO 로 반환한다 (docs/architecture.md §7).
 * `screening:model` 을 의존하지 않는 이유가 이것이다 — 계약이 도메인 모델을 노출하면
 * 좁게 유지되지 않는다.
 *
 * 구현체는 `internal` 로 `screening:service` 에 두고 Bean 도 거기서 등록한다.
 */
interface ScreeningResultContract {
    /**
     * 회원의 최신 진단에서 판별된 원인을 **rank 순으로** 돌려준다.
     *
     * 복수를 돌려주는 것은 진단 결과 화면이 원인을 순위로 나열하기 때문이다. 단수로는 만들 수 없다.
     *
     * `bodyPartCode` 파라미터가 없다. 진단이 부위를 결정하는 쪽이라 호출부가 미리 알고
     * 들어오지 않는다 (docs/domains.md §3).
     *
     * 진단한 적이 없으면 빈 목록이다. 예외를 던지지 않는 것은 "아직 진단 전" 이 `course` 입장에서
     * 정상 상태이기 때문이다.
     */
    fun findLatestCauses(memberId: Long): List<LatestCauseResponse>
}

/**
 * 계약이 발행하는 DTO. `screening` 의 내부 모델이 바뀌어도 이 모양은 `course` 와의 합의로만 바뀐다.
 */
data class LatestCauseResponse(
    val causeCode: String,
    val bodyPartCode: String,
    val rank: Int,
)
