package com.mugtaba.pixl.exceptions;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception class for unauthorized access errors in the Pixl application.
 * This exception represents client-side errors, typically associated with HTTP status code 401 (Unauthorized).
 * It extends the base PixlException class to provide specific handling for unauthorized access issues.
 */
public class UnauthorizedException extends PixlException {
    /**
     * Constructor for UnauthorizedException.
     * This exception is initialized with a user-friendly message and defaults to HTTP status code 401
     * @param userMessage a message intended for end-users
     */
    public UnauthorizedException(String userMessage) {
        super(HttpServletResponse.SC_UNAUTHORIZED, userMessage, "Unauthorized access: " + userMessage);
    }
}
