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
 *
 * category 는 코스 스텝에 붙는 분류다("가동성 웜업"·"핀포즈"). 값 집합이 감수 대상이라
 * enum 이 아니라 문자열이다 — difficulty 와 같은 이유다.
 *
 * thumbnailUrl 은 영상 포스터 프레임이고 imageAssetKey 와 자리가 다르다 — 그림은 자세
 * 일러스트(프론트 정적 자산)이고 이쪽은 실제 영상의 한 프레임(YMove 자산)이다. videoUrl 과
 * 달리 서명도 만료도 없어 DB 에 저장하므로 **YMove 장애와 무관하게 값이 있다**.
 *
 * imageAssetKey 는 키이고 videoUrl 은 URL 이다. 그림 파일은 프론트가 갖지만 영상 소스는
 * YMove 라 우리가 갖지 않는다 (docs/domains.md §4-3-1). videoUrl 은 adapter-ymove 연동
 * 전까지 항상 null 이다.
 */
data class ExerciseDetailView(
    val exerciseId: Long,
    val name: String,
    val imageAssetKey: String?,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val defaultSetCount: Int?,
    val defaultRepCount: Int?,
    val defaultDurationSeconds: Int?,
    val metValue: BigDecimal?,
    val difficulty: String?,
    val category: String?,
    val cautionNote: String?,
    val muscles: List<MuscleView>,
    val voiceCues: List<ExerciseVoiceCueView>,
    /**
     * 부위 탭마다 붙는 핵심 동작 문구. 탭 집합은 [muscles] 의 부위 집합과 같다 — 근육은
     * 있는데 문구가 없는 탭이 생기지 않도록 seed 를 그렇게 맞춰 두었다.
     */
    val bodyPartGuides: List<ExerciseBodyPartGuideView>,
)
