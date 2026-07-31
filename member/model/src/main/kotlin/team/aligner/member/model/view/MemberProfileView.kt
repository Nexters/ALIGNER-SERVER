package team.aligner.member.model.view

/**
 * 프로필 화면 하나를 위한 읽기 모델.
 *
 * infrastructure 가 아니라 model/view 에 둔다. 여기 있어야 api 가 port 모듈을 의존하지 않고
 * 이 타입을 쓸 수 있다 (docs/architecture.md §4).
 *
 * nickname·profileImageUrl 은 null 일 수 있다. 카카오 프로필 제공에 동의하지 않은 회원이다.
 */
data class MemberProfileView(
    val memberId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
)
