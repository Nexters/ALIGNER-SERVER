package team.aligner.screening.infrastructure

/**
 * 부위 코드가 실재하는지 확인하는 out-port.
 *
 * 조회처럼 보이지만 Query 리포지토리에 두지 않는다. 이 호출은 **제출 흐름의 검증**이고,
 * Command 가 조회 모델을 끌어 쓰기 시작하면 경계가 흐려진다. `screening_result` 에 FK 가 있어
 * DB 도 막지만, 거기까지 가면 제약 위반이 500 으로 나간다. 회원 입력 문제는 404 여야 한다.
 */
interface BodyPartRepository {
    fun existsByCode(bodyPartCode: String): Boolean
}
