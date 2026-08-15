package team.aligner.member.service

import team.aligner.member.model.ExperienceLevel
import team.aligner.member.model.ReinforcementSetting

/**
 * 프로필 수정 입력. 온보딩과 프로필 편집이 **같은 명령을 쓴다.**
 *
 * 온보딩이 경력 화면, 키·몸무게 화면, 강화 설정 화면으로 나뉘어 있어 화면마다 자기가 받은
 * 조각만 채워 보낸다. 별도의 온보딩 전용 명령을 만들지 않는 이유다.
 *
 * **null 은 "바꾸지 않는다" 는 뜻이다.** 값을 비우는 수단이 아니다 (Member.changeProfile).
 *
 * 회원이 직접 입력한 값이라 Member 가 규칙을 검사한다.
 * 프로필 이미지는 카카오가 주는 값이라 MVP 에서 수정 대상이 아니다.
 */
data class UpdateMemberProfileCommand(
    val nickname: String? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val experienceLevel: ExperienceLevel? = null,
    val reinforcement: ReinforcementSetting? = null,
)
