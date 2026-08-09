package team.aligner.course.model.exception

import team.aligner.support.core.BaseException

/**
 * GlobalExceptionHandler 가 BaseException 을 이미 잡으므로 별도 핸들러가 필요 없다.
 */
class CourseNotFoundException : BaseException(CourseErrorCode.COURSE_NOT_FOUND)

class CourseStepNotFoundException : BaseException(CourseErrorCode.COURSE_STEP_NOT_FOUND)

class InProgressCourseNotFoundException : BaseException(CourseErrorCode.IN_PROGRESS_COURSE_NOT_FOUND)

class CourseTemplateNotFoundException : BaseException(CourseErrorCode.COURSE_TEMPLATE_NOT_FOUND)

class ScreeningRequiredException : BaseException(CourseErrorCode.SCREENING_REQUIRED)

class BodyPartNotInScreeningException : BaseException(CourseErrorCode.BODY_PART_NOT_IN_SCREENING)

class EmptyCourseTemplateException : BaseException(CourseErrorCode.EMPTY_COURSE_TEMPLATE)
