package team.aligner.member.model.view

import team.aligner.member.model.ExperienceLevel

/**
 * 프로필 화면 하나를 위한 읽기 모델.
 *
 * infrastructure 가 아니라 model/view 에 둔다. 여기 있어야 api 가 port 모듈을 의존하지 않고
 * 이 타입을 쓸 수 있다 (docs/architecture.md §4).
 *
 * nickname·profileImageUrl 은 null 일 수 있다. 카카오 프로필 제공에 동의하지 않은 회원이다.
 * 신체 정보와 강화 설정도 null 일 수 있다 — 온보딩을 끝내지 않은 회원이다. 프론트는 이
 * null 들을 "온보딩 미완료" 신호로 읽는다.
 */
data class MemberProfileView(
    val memberId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
    val heightCm: Int?,
    val weightKg: Int?,
    val experienceLevel: ExperienceLevel?,
    val reinforcementBodyPartCode: String?,
    val reinforcementLevel: Int?,
)
