package com.kusm.exceptions;

public class MaxOtpAttemptsExceededException extends RuntimeException {
    public MaxOtpAttemptsExceededException(String message) {
        super(message);
    }
}
