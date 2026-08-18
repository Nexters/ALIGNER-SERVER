package team.aligner.catalog.model.view

import team.aligner.catalog.model.MuscleRole

/**
 * 자세·운동에 딸린 근육 하나. 운동 가이드의 부위 탭과 근육맵에 쓴다.
 *
 * 하이라이트 키가 **앞·뒤 두 개**다. 세션 플레이어의 근육맵이 인체 앞면과 뒷면을 토글로
 * 보여주고 각각 근육을 칠하므로, 어느 쪽 그림에 얹을 키인지가 구분돼야 한다.
 * 척추기립근처럼 뒤에만 보이는 근육은 front 가 null 이고 그 반대도 마찬가지다.
 *
 * 실제 파일은 정적 asset 이고 DB 에는 키만 둔다 (docs/domains.md §4-3).
 *
 * bodyPartCode 는 screening 소유 어휘를 값으로 받은 것이다. catalog 에 타입을 만들지 않는다.
 */
data class MuscleView(
    val muscleCode: String,
    val name: String,
    val bodyPartCode: String,
    val frontHighlightAssetKey: String?,
    val backHighlightAssetKey: String?,
    val role: MuscleRole,
    val displayOrder: Int,
    /**
     * 운동 가이드의 「핵심 동작」 한 문장. 이 운동에서 그 근육을 어떻게 쓰는지다.
     *
     * **자세(`pose_muscle`)에는 없어 항상 null 이다.** 자세 상세를 그리는 화면이 아직 없어
     * 문구를 적재하지 않았다. 운동에서도 감수 전이면 null 일 수 있고, 그때 화면은 카드를
     * 그리지 않는다.
     */
    val description: String?,
)
