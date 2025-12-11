package com.mugtaba.pixl.exceptions;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception thrown when a user attempts to access admin-only resources without proper permissions.
 */
public class ForbiddenException extends PixlException {

    public ForbiddenException(String userMessage) {
        super(
            HttpServletResponse.SC_FORBIDDEN,
            userMessage,
            "Access forbidden: " + userMessage
        );
    }

    public ForbiddenException(String userMessage, String logMessage) {
        super(HttpServletResponse.SC_FORBIDDEN, userMessage, logMessage);
    }
}
