package team.aligner.member.service

/**
 * 프로필 수정 입력.
 *
 * 회원이 직접 입력한 값이라 Member.changeProfile 이 닉네임 규칙을 검사한다.
 * 프로필 이미지는 카카오가 주는 값이라 MVP 에서 수정 대상이 아니다.
 */
data class UpdateMemberProfileCommand(
    val nickname: String,
)
