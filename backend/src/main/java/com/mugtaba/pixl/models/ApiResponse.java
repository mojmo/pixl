package com.mugtaba.pixl.models;

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
public class ApiResponse<T> {
    private final boolean success;
    private String message;
    private T data;
    private String error;

    /**
     * Constructs a successful API response with data and an optional message.
     * Use this constructor for successful operations that return data.
     *
     * @param success  true indicating a successful operation
     * @param message  an informational message about the operation (maybe null)
     * @param data     the payload data to be returned (maybe null)
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
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
        this.error = error;
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
}
