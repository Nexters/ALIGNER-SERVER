package team.aligner.member.contract

/**
 * `course` 가 member 에 요구하는 신체 정보 조회 계약. 통합 전용이라 좁게 만든다
 * (docs/architecture.md §7).
 *
 * **몸무게만 돌려준다.** 쓰임이 칼로리 계산 하나뿐이기 때문이다 —
 * `kcal = MET × 3.5 × 체중(kg) ÷ 200 × 분` (docs/domains.md §4-3).
 * 키·운동 경력은 코스가 쓸 자리가 없으므로 넣지 않는다. 필요해지면 그때 늘린다.
 *
 * `MemberAuthContract` 에 얹지 않고 인터페이스를 따로 둔다. 그쪽은 support-web 이 쓰는
 * 인증 계약이고 이쪽은 course 가 쓰는 조회 계약이라 소비자도 수명도 다르다.
 *
 * 구현체는 internal 로 member:service 에 두고 Bean 도 거기서 등록한다.
 */
interface MemberBodyContract {
    /**
     * 없는 회원이거나 탈퇴한 회원이면 null 이다. 예외를 던지지 않는다 —
     * 호출부가 흐름 제어에 예외를 쓰게 된다.
     */
    fun findBody(memberId: Long): MemberBodyResponse?
}

/**
 * `weightKg` 는 null 일 수 있다. 온보딩에서 몸무게를 아직 받지 않은 회원이다.
 *
 * **그때 칼로리를 0 으로 만들지 않는다.** 0 kcal 은 "운동량이 없다" 는 뜻이라 "모른다" 와
 * 다르고, 화면이 둘을 구분해야 한다. 계산하는 쪽이 null 을 그대로 다룬다.
 */
data class MemberBodyResponse(
    val memberId: Long,
    val weightKg: Int?,
)
