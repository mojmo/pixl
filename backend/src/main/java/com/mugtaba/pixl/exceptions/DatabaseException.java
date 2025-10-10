package com.mugtaba.pixl.exceptions;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception class for database errors in the Pixl application.
 * This exception represents server-side errors, typically associated with HTTP status code 500 (Internal Server Error).
 * It extends the base PixlException class to provide specific handling for database issues.
 */
public class DatabaseException extends PixlException {

    /**
     * Constructor for DatabaseException.
     * This exception is initialized with a user-friendly message and defaults to HTTP status code 500.
     * 
     * @param operation the operation that caused the database error
     * @param cause the underlying cause of the database error
     */
    public DatabaseException(String operation, Throwable cause) {
        super(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Unable to complete the requested operation. Please try again later.",
            "Database error during " + operation + ": " + cause.getMessage()
        );
    }
}
