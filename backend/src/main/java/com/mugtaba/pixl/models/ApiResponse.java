package com.mugtaba.pixl.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * A generic response wrapper for API responses that standardizes the structure
 * of success and error responses.
 *
 * <p>The response structure includes:
 * <ul>
 *   <li><b>success</b>: Boolean indicating if the request was successful</li>
 *   <li><b>message</b>: Optional informational message for successful responses</li>
 *   <li><b>data</b>: The actual payload/data for successful responses</li>
 *   <li><b>error</b>: Error message or description for failed responses</li>
 * </ul>
 *
 * @param <T> the type of the data payload included in successful responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @JsonProperty("success")
    private final boolean success;
    @JsonProperty("message")
    private final String message;
    @JsonProperty("data")
    private final T data;
    @JsonProperty("error")
    private String error;
    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;
    @JsonProperty("path")
    private String path;
    @JsonProperty("status")
    private Integer status;

    /**
     * Constructs a successful API response with data and an optional message.
     * Use this constructor for successful operations that return data.
     *
     * @param success  true indicating a successful operation
     * @param message  an informational message about the operation (maybe null)
     * @param data     the payload data to be returned (maybe null)
     */
    public ApiResponse(boolean success, String message, T data) {
        if (!success && data != null) {
            throw new IllegalArgumentException("Error responses should not contain data");
        }
        if (!success && message != null) {
            throw new IllegalArgumentException("Error responses should use error field, not message");
        }
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = null;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructs an API response for failed operations with an error message.
     * Use this constructor for operations that failed and need to communicate an error.
     *
     * @param success false indicating a failed operation
     * @param error   a description of the error that occurred
     * @throws IllegalArgumentException if success is true but an error is provided
     */
    public ApiResponse(boolean success, String error) {
        if (success && error != null) {
            throw new IllegalArgumentException("Successful responses should not contain error messages");
        }
        this.success = success;
        this.message = null;
        this.data = null;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructs an API response with error message and additional context such as path and status.
     * 
     * @param success false indicating a failed operation
     * @param error   a description of the error that occurred
     * @param path    the request path that was accessed
     * @param status  the HTTP status code for the response
     */
    public ApiResponse(boolean success, String error, String path, Integer status) {
        if (success && error != null) {
            throw new IllegalArgumentException("Successful responses should not contain error messages");
        }
        this.success = success;
        this.message = null;
        this.data = null;
        this.error = error;
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.status = status;
    }

    /**
     * Constructs an API response with additional context such as path and status.
     *
     * @param success  true indicating a successful operation
     * @param message  an informational message about the operation (maybe null)
     * @param data     the payload data to be returned (maybe null)
     * @param path     the request path that was accessed
     * @param status   the HTTP status code for the response
     */
    public ApiResponse(boolean success, String message, T data, String path, Integer status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = null;
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.status = status;
    }

    // Static factory methods for convenience

    /**
     * Creates a successful ApiResponse with the given message and data.
     * @param <T> the type of the data payload
     * @param message an informational message about the operation
     * @param data the payload data to be returned
     * @return an ApiResponse indicating success with the provided message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Creates a successful ApiResponse with the given message and no data.
     * @param <T> the type of the data payload
     * @param message an informational message about the operation
     * @return an ApiResponse indicating success with the provided message and no data
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /**
     * Creates an error ApiResponse with the given error message.
     * @param <T> the type of the data payload
     * @param errorMessage a description of the error that occurred
     * @return an ApiResponse indicating failure with the provided error message
     */
    public static <T> ApiResponse<T> error(String errorMessage) {
        return new ApiResponse<>(false, errorMessage);
    }

    /**
     * Creates an error ApiResponse with the given error message, path, and status.
     * @param <T> the type of the data payload
     * @param errorMessage a description of the error that occurred
     * @param path the request path that was accessed
     * @param status the HTTP status code for the response
     * @return an ApiResponse indicating failure with the provided error message, path, and status
     */
    public static <T> ApiResponse<T> error(String errorMessage, String path, Integer status) {
        return new ApiResponse<>(false, errorMessage, path, status);
    }

    /**
     * Returns whether the API operation was successful.
     *
     * @return true if the operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the informational message associated with a successful operation.
     * This method typically returns null for failed responses.
     *
     * @return the informational message or null if not available
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the data payload of a successful operation.
     * This method typically returns null for failed responses.
     *
     * @return the data payload or null if not available
     */
    public T getData() {
        return data;
    }

    /**
     * Returns the error message for a failed operation.
     * This method typically returns null for successful responses.
     *
     * @return the error message or null if not available
     */
    public String getError() {
        return error;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPath() { return path; }
    public Integer getStatus() { return status; }

    // Setters for additional context
    public void setPath(String path) { this.path = path; }
    public void setStatus(Integer status) { this.status = status; }

    /**
     * Creates a successful ApiResponse with pagination details included in the data.
     *
     * @param message      an informational message about the operation
     * @param data         the payload data to be returned
     * @param currentPage  the current page number
     * @param totalPages   the total number of pages available
     * @param totalItems   the total number of items across all pages
     * @param <T>          the type of the data payload
     * @return an ApiResponse containing the data and pagination details
     */
    @SuppressWarnings("unchecked")
    public static <T> ApiResponse<T> successWithPagination(String message, T data, int currentPage, int totalPages, long totalItems) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("items", data);
        responseData.put("pagination", Map.of(
            "currentPage", currentPage,
            "totalPages", totalPages,
            "totalItems", totalItems,
            "hasNext", currentPage < totalPages,
            "hasPrevious", currentPage > 1
        ));

        return success(message, (T) responseData);
    }
}
