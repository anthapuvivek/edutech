package com.learntrix.edtech.common.exception;

public final class ErrorCode {
    private ErrorCode() {}

    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED";
    public static final String AUTH_EMAIL_NOT_VERIFIED = "AUTH_EMAIL_NOT_VERIFIED";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
    public static final String AUTH_TOKEN_INVALID = "AUTH_TOKEN_INVALID";

    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_EMAIL_ALREADY_EXISTS = "USER_EMAIL_ALREADY_EXISTS";
    public static final String USER_ACCOUNT_SUSPENDED = "USER_ACCOUNT_SUSPENDED";

    public static final String STUDENT_NOT_FOUND = "STUDENT_NOT_FOUND";
    public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";

    public static final String COURSE_NOT_FOUND = "COURSE_NOT_FOUND";
    public static final String COURSE_ACCESS_DENIED = "COURSE_ACCESS_DENIED";
    public static final String COURSE_NOT_PUBLISHED = "COURSE_NOT_PUBLISHED";

    public static final String ENROLLMENT_NOT_FOUND = "ENROLLMENT_NOT_FOUND";
    public static final String ENROLLMENT_NOT_ACTIVE = "ENROLLMENT_NOT_ACTIVE";
    public static final String ENROLLMENT_EXPIRED = "ENROLLMENT_EXPIRED";
    public static final String ENROLLMENT_ALREADY_EXISTS = "ENROLLMENT_ALREADY_EXISTS";

    public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
    public static final String PAYMENT_ALREADY_VERIFIED = "PAYMENT_ALREADY_VERIFIED";
    public static final String PAYMENT_REJECTED = "PAYMENT_REJECTED";

    public static final String COUPON_INVALID = "COUPON_INVALID";
    public static final String COUPON_EXPIRED = "COUPON_EXPIRED";
    public static final String COUPON_USAGE_LIMIT = "COUPON_USAGE_LIMIT";
    public static final String COUPON_NOT_APPLICABLE = "COUPON_NOT_APPLICABLE";
    public static final String COUPON_MINIMUM_PURCHASE = "COUPON_MINIMUM_PURCHASE";

    public static final String BATCH_NOT_FOUND = "BATCH_NOT_FOUND";
    public static final String BATCH_FULL = "BATCH_FULL";

    public static final String CLASS_NOT_FOUND = "CLASS_NOT_FOUND";
    public static final String CLASS_ACCESS_DENIED = "CLASS_ACCESS_DENIED";

    public static final String QUIZ_NOT_FOUND = "QUIZ_NOT_FOUND";
    public static final String QUIZ_ALREADY_SUBMITTED = "QUIZ_ALREADY_SUBMITTED";
    public static final String QUIZ_TIME_EXPIRED = "QUIZ_TIME_EXPIRED";

    public static final String ASSIGNMENT_NOT_FOUND = "ASSIGNMENT_NOT_FOUND";
    public static final String ASSIGNMENT_DEADLINE_PASSED = "ASSIGNMENT_DEADLINE_PASSED";

    public static final String JOB_NOT_FOUND = "JOB_NOT_FOUND";
    public static final String JOB_NOT_ELIGIBLE = "JOB_NOT_ELIGIBLE";

    public static final String TICKET_NOT_FOUND = "TICKET_NOT_FOUND";
    public static final String LEAD_NOT_FOUND = "LEAD_NOT_FOUND";

    public static final String FILE_UPLOAD_FAILED = "FILE_UPLOAD_FAILED";
    public static final String FILE_TYPE_NOT_ALLOWED = "FILE_TYPE_NOT_ALLOWED";
    public static final String FILE_SIZE_EXCEEDED = "FILE_SIZE_EXCEEDED";

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
}
