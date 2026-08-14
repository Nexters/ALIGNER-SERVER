package team.aligner.course.infrastructure

import java.math.BigDecimal

/**
 * 코스 스텝에 그릴 운동 정보를 읽는 out-port. `course/adapter-catalog` 가 구현한다.
 *
 * **식별자 목록을 한 번에 받는다.** 코스 개요가 스텝을 개수만큼 그리므로 스텝마다 부르면
 * 조회가 스텝 수만큼 늘어난다 (docs/domains.md §4-3-1).
 */
interface ExerciseCatalogPort {
    fun findAllByIds(exerciseIds: List<Long>): List<ExerciseCatalogEntry>
}

/**
 * `defaultDurationSeconds` `defaultSetCount` 는 코스에 override 가 없을 때 쓰는 값이다.
 *
 * `category` 는 코스 스텝 행에 이름 아래로 그리는 분류다("가동성 웜업"·"핀포즈").
 *
 * `metValue` 는 칼로리 계산 입력이다. kcal 자체는 회원 몸무게의 함수라 catalog 가 계산할 수
 * 없다 (docs/domains.md §4-3).
 */
data class ExerciseCatalogEntry(
    val exerciseId: Long,
    val name: String,
    /** 코스 순서 카드가 스텝마다 그리는 그림. URL 이 아니라 키다. */
    val imageAssetKey: String?,
    val category: String?,
    val defaultSetCount: Int?,
    val defaultDurationSeconds: Int?,
    val metValue: BigDecimal?,
)
