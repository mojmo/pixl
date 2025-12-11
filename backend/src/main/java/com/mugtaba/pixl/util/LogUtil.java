package com.mugtaba.pixl.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for logging various events and errors in the Pixl application.
 * This class provides static methods to log messages with different severity levels,
 * including error, warning, info, validation errors, and unauthorized access attempts.
 * Logs include timestamps and contextual information to aid in debugging and monitoring.
 */
public class LogUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs an error message with optional exception details.
     * @param component the component or module where the error occurred
     * @param operation the specific operation or function where the error occurred
     * @param message a descriptive error message
     * @param throwable the exception that was thrown (can be null)
     */
    public static void logError(String component, String operation, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        System.err.printf("[%s] ERROR [%s:%s] %s%n", timestamp, component, operation, message);
        if (throwable != null) {
            System.err.printf(
                "[%s] ERROR [%s:%s] Exception: %s%n",
                timestamp, component, operation, throwable.getMessage()
            );
        }
    }

    /**
     * Logs a warning message.
     * @param component the component or module where the warning occurred
     * @param operation the specific operation or function where the warning occurred
     * @param message a descriptive warning message
     */
    public static void logWarning(String component, String operation, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        System.out.printf("[%s] WARNING [%s:%s] %s%n", timestamp, component, operation, message);
    }

    /**
     * Logs an informational message.
     * @param component the component or module where the info is logged
     * @param operation the specific operation or function related to the info
     * @param message a descriptive informational message
     */
    public static void logInfo(String component, String operation, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        System.out.printf("[%s] INFO [%s:%s] %s%n", timestamp, component, operation, message);
    }

    /**
     * Logs a security-related message.
     * @param component the component or module where the security event occurred
     * @param message a descriptive security message
     */
    public static void logSecurity(String component, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        System.out.printf("[%s] SECURITY [%s] %s%n", timestamp, component, message);
    }

    /**
     * Logs a validation error with details about the invalid field and reason.
     * @param component the component or module where the validation error occurred
     * @param field the specific field that failed validation
     * @param value the value that failed validation
     * @param reason the reason why the validation failed
     */
    public static void logValidationError(String component, String field, String value, String reason) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        System.out.printf("[%s] VALIDATION [%s] Field '%s' with value '%s': %s%n",
            timestamp, component, field, value != null ? value : "null", reason
        );
    }

    /**
     * Logs an unauthorized access attempt with details about the component, operation, and reason.
     * @param component the component or module where the unauthorized access was attempted
     * @param operation the specific operation or function that was attempted
     * @param details additional details about the unauthorized access attempt
     */
    public static void logUnauthorizedAccess(String component, String operation, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        System.out.printf("[%s] SECURITY [%s:%s] Unauthorized access: %s%n",
            timestamp, component, operation, details
        );
    }
}
