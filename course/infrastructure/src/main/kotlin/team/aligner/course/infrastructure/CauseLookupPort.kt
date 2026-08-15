package team.aligner.course.infrastructure

/**
 * 회원의 최신 진단 원인을 읽는 out-port. `course/adapter-screening` 이 구현한다
 * (docs/domains.md §3).
 *
 * **클라이언트가 원인을 들고 오지 않는다.** 추천 요청은 course 가 받고 그 회원의 최신 원인을
 * 여기서 읽어 검증한다. 원인을 요청 본문으로 받으면 위조가 가능하다 (§2).
 */
interface CauseLookupPort {
    /** rank 오름차순. 진단한 적이 없으면 빈 목록이다. */
    fun findLatestCauses(memberId: Long): List<CauseLookup>
}

data class CauseLookup(
    val causeCode: String,
    val bodyPartCode: String,
    val rank: Int,
)
