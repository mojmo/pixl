package com.mugtaba.pixl.exceptions;

/**
 * Base exception class for all custom exceptions in the Pixl application.
 */
public class PixlException extends Exception{
    private final int statusCode;
    private final String userMessage;
    private final String logMessage;

    /**
     * Constructor for PixlException.
     * This exception is designed to carry an HTTP status code, a user-friendly message,
     * and a log message for internal use.
     * @param statusCode HTTP status code associated with the exception
     * @param userMessage a message intended for end-users
     * @param logMessage a message intended for logging purposes
     */
    public PixlException(int statusCode, String userMessage, String logMessage) {
        super(logMessage);
        this.statusCode = statusCode;
        this.userMessage = userMessage;
        this.logMessage = logMessage;
    }

    public int getStatusCode() { return statusCode; }
    public String getUserMessage() { return userMessage; }
    public String getLogMessage() { return logMessage; }
}