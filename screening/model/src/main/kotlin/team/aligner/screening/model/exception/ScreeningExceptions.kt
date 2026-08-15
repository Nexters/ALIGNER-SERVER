package team.aligner.screening.model.exception

import team.aligner.support.core.BaseException

/**
 * GlobalExceptionHandler 가 BaseException 을 이미 잡으므로 별도 핸들러가 필요 없다.
 */
class EmptyScreeningAnswerException : BaseException(ScreeningErrorCode.EMPTY_SCREENING_ANSWER)

class TooManyScreeningAnswersException : BaseException(ScreeningErrorCode.TOO_MANY_SCREENING_ANSWERS)

class DuplicateScreeningAnswerException : BaseException(ScreeningErrorCode.DUPLICATE_SCREENING_ANSWER)

class BodyPartNotFoundException : BaseException(ScreeningErrorCode.BODY_PART_NOT_FOUND)

class ScreeningResultNotFoundException : BaseException(ScreeningErrorCode.SCREENING_RESULT_NOT_FOUND)

class CauseNotDeterminedException : BaseException(ScreeningErrorCode.CAUSE_NOT_DETERMINED)
