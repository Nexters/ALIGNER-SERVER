package team.aligner.training.infrastructure

/**
 * 세션 중에 보여줄 운동 정보를 읽는 out-port. `training/adapter-catalog` 가 구현한다
 * (docs/domains.md §3).
 *
 * **식별자 목록을 한 번에 받는다.** 세션 응답이 스텝의 운동을 모두 실으므로 운동마다 부르면
 * 조회가 그만큼 늘어난다 (§4-3-1).
 *
 * 이름과 기본값만 받는다. 음성 큐·근육·주의사항은 세션 플레이어가 `GET /catalog/exercises/{id}`
 * 로 직접 읽는다 — 그 화면이 catalog API 를 이미 쓰고 있다.
 */
interface ExerciseDetailPort {
    fun findAllByIds(exerciseIds: List<Long>): List<ExerciseDetailLookup>
}

data class ExerciseDetailLookup(
    val exerciseId: Long,
    val name: String,
    val category: String?,
    val defaultSetCount: Int?,
    val defaultDurationSeconds: Int?,
)
