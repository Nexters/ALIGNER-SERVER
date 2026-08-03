package team.aligner.catalog.model.view

import java.math.BigDecimal

/**
 * 운동 가이드 화면 하나를 위한 읽기 모델.
 *
 * 칼로리 필드를 두지 않는다. kcal 은 MET × 3.5 × 체중 ÷ 200 × 분 이라 회원 몸무게의 함수인데
 * 몸무게는 member 소유이고 catalog 는 member 를 의존할 수 없다 (docs/domains.md §1, §4-3).
 * metValue 만 실어 보내고 계산은 조회하는 쪽이 한다.
 *
 * ymoveSlug 를 싣지 않는다. 외부 시스템 식별자가 화면 계층까지 새어 나갈 이유가 없다.
 */
data class ExerciseDetailView(
    val exerciseId: Long,
    val name: String,
    val defaultSetCount: Int?,
    val defaultRepCount: Int?,
    val defaultDurationSeconds: Int?,
    val metValue: BigDecimal?,
    val difficulty: String?,
    val contraindications: String?,
    val muscles: List<MuscleView>,
    val voiceCues: List<ExerciseVoiceCueView>,
)
