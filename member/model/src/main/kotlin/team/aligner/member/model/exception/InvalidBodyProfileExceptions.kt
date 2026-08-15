package team.aligner.member.model.exception

import team.aligner.support.core.BaseException

/**
 * 온보딩·프로필 편집 입력 오류. DDL 의 CHECK 도 같은 범위를 막지만 거기까지 가면 500 이다.
 * 회원 입력 문제는 400 이어야 한다.
 */
class InvalidHeightException : BaseException(MemberErrorCode.INVALID_HEIGHT)

class InvalidWeightException : BaseException(MemberErrorCode.INVALID_WEIGHT)

class InvalidReinforcementSettingException : BaseException(MemberErrorCode.INVALID_REINFORCEMENT_SETTING)
