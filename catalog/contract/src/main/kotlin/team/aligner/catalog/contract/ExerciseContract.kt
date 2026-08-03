package team.aligner.catalog.contract

import java.math.BigDecimal

/**
 * course·training 이 catalog 에 요구하는 운동 조회 계약. 통합 전용이라 좁게 만든다
 * (docs/architecture.md §7).
 *
 * 구현체는 internal 로 catalog:service 에 두고 Bean 도 거기서 등록한다.
 */
interface ExerciseContract {
    /**
     * 존재하지 않는 식별자가 섞여 있어도 예외를 던지지 않고 찾은 것만 돌려준다.
     * 도메인 간 FK 가 없어 course seed 가 앞서갈 수 있다 (docs/domains.md §6).
     */
    fun findAllByIds(exerciseIds: List<Long>): List<ExerciseResponse>
}

/**
 * default* 는 course.course_step_exercise 가 비어 있을 때 쓰는 기본값이다
 * (docs/domains.md §4-4).
 *
 * metValue 를 싣고 kcal 은 싣지 않는다. 칼로리는 회원 몸무게의 함수라 catalog 가 계산할 수
 * 없고, 코스 칼로리는 스텝 합으로 course 가 계산한다 (§4-3).
 *
 * 근육·음성 큐·주의사항을 넣지 않는다. 전부 운동 가이드 화면에 그리는 값이고 그 화면은
 * catalog API 가 직접 응답한다. course·training 이 필요로 한다는 근거가 §4-4·4-5 에 없다.
 * 필요해지면 그때 늘린다 (docs/architecture.md §3 "미리 만들지 않는다").
 */
data class ExerciseResponse(
    val exerciseId: Long,
    val name: String,
    val defaultSetCount: Int?,
    val defaultRepCount: Int?,
    val defaultDurationSeconds: Int?,
    val metValue: BigDecimal?,
    val difficulty: String?,
)
