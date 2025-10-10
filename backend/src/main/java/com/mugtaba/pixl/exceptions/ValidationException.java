package com.mugtaba.pixl.exceptions;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception class for validation errors in the Pixl application.
 * This exception represents client-side errors, typically associated with HTTP status code 400 (Bad Request).
 * It extends the base PixlException class to provide specific handling for validation issues.
 */
public class ValidationException extends PixlException {

    /**
     * Constructor for ValidationException.
     * This exception is initialized with a user-friendly message and defaults to HTTP status code 400.
     * @param userMessage a message intended for end-users
     */
    public ValidationException(String userMessage) {
        super(HttpServletResponse.SC_BAD_REQUEST, userMessage, "Validation failed: " + userMessage);
    }

    /**
     * Constructor for ValidationException with detailed log information.
     * This exception is initialized with a user-friendly message and additional log details,
     * defaulting to HTTP status code 400.
     * @param userMessage a message intended for end-users
     * @param logDetails additional details for logging purposes
     */
    public ValidationException(String userMessage, String logDetails) {
        super(HttpServletResponse.SC_BAD_REQUEST, userMessage, "Validation failed: " + logDetails);
    }
}
