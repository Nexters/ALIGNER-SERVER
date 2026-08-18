package team.aligner.catalog.model.view

/**
 * 운동 가이드 부위 탭 하나에 붙는 「핵심 동작」 문구.
 *
 * **근육 단위가 아니라 부위 단위다.** 탭 하나에 근육이 여럿 칠해지므로 [MuscleView] 마다
 * 문구를 두면 같은 문구가 중복되고 서로 어긋날 수 있다.
 *
 * [ExerciseVoiceCueView] 와 자리가 다르다. 저쪽은 재생 중 순서대로 읽어주는 대본이라
 * 타임코드를 갖고, 이쪽은 재생 전 정적 설명이다.
 *
 * bodyPartCode 는 screening 소유 어휘를 값으로 받은 것이다. catalog 에 타입을 만들지 않는다.
 */
data class ExerciseBodyPartGuideView(
    val bodyPartCode: String,
    val content: String,
    val displayOrder: Int,
)
