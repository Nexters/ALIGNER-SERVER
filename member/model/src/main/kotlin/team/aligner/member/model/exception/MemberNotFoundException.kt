package team.aligner.member.model.exception

import team.aligner.support.core.BaseException

class MemberNotFoundException : BaseException(MemberErrorCode.MEMBER_NOT_FOUND)
