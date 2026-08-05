package team.aligner.screening.infrastructure

import team.aligner.screening.model.ScreeningResult

/**
 * 쓰기 out-port. 애그리거트 단위로만 오간다 (docs/architecture.md §4).
 *
 * 자식(`ScreeningAnswer` `ScreeningCause`)만 따로 저장하는 메서드를 만들지 않는다. 응답과
 * 판별 결과가 따로 저장될 수 있으면 "응답은 있는데 원인이 없는 진단" 이 만들어진다.
 */
interface ScreeningResultRepository {
    fun save(screeningResult: ScreeningResult): ScreeningResult
}
