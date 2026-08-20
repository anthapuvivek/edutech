package com.learntrix.edtech.common.exception;

import org.springframework.http.HttpStatus;

public class CourseAccessDeniedException extends BusinessException {
    public CourseAccessDeniedException(String message) {
        super(ErrorCode.COURSE_ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
