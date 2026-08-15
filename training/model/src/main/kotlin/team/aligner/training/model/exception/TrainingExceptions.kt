package team.aligner.training.model.exception

import team.aligner.support.core.BaseException

/**
 * GlobalExceptionHandler 가 BaseException 을 이미 잡으므로 별도 핸들러가 필요 없다.
 */
class SessionNotFoundException : BaseException(TrainingErrorCode.SESSION_NOT_FOUND)

class CourseStepNotFoundException : BaseException(TrainingErrorCode.COURSE_STEP_NOT_FOUND)

class EmptyCourseStepException : BaseException(TrainingErrorCode.EMPTY_COURSE_STEP)

class UnknownExerciseRecordException : BaseException(TrainingErrorCode.UNKNOWN_EXERCISE_RECORD)

class DuplicateExerciseRecordException : BaseException(TrainingErrorCode.DUPLICATE_EXERCISE_RECORD)
