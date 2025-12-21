package com.mugtaba.pixl.util;

import com.mugtaba.pixl.models.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Centralized utility for sending standardized HTTP responses.
 */
public class ResponseUtil {

    /**
     * Sends a JSON error response with specified status code and message.
     * @param response the HttpServletResponse object
     * @param statusCode the HTTP status code
     * @param message the error message
     * @throws IOException if an error occurs during response writing
     */
    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Sends a JSON success response with data.
     * @param response the HttpServletResponse object
     * @param message the success message
     * @param data the response data
     * @throws IOException if an error occurs during response writing
     */
    public static void sendSuccess(HttpServletResponse response, String message, Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.success(message, data);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Sends a JSON success response without data.
     * @param response the HttpServletResponse object
     * @param message the success message
     * @throws IOException if an error occurs during response writing
     */
    public static void sendSuccess(HttpServletResponse response, String message) throws IOException {
        sendSuccess(response, message, null);
    }

    /**
     * Sends a 201 Created response with data.
     * @param response the HttpServletResponse object
     * @param message the success message
     * @param data the created resource data
     * @throws IOException if an error occurs during response writing
     */
    public static void sendCreated(HttpServletResponse response, String message, Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.success(message, data);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Sends a 401 Unauthorized response.
     * @param response the HttpServletResponse object
     * @param message the error message
     * @throws IOException if an error occurs during response writing
     */
    public static void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    /**
     * Sends a 403 Forbidden response.
     * @param response the HttpServletResponse object
     * @param message the error message
     * @throws IOException if an error occurs during response writing
     */
    public static void sendForbidden(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_FORBIDDEN, message);
    }

    /**
     * Sends a 404 Not Found response.
     * @param response the HttpServletResponse object
     * @param message the error message
     * @throws IOException if an error occurs during response writing
     */
    public static void sendNotFound(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_NOT_FOUND, message);
    }

    /**
     * Sends a 429 Too Many Requests response with Retry-After header.
     * @param response the HttpServletResponse object
     * @param message the error message
     * @param retryAfterSeconds seconds to wait before retrying
     * @throws IOException if an error occurs during response writing
     */
    public static void sendRateLimitExceeded(HttpServletResponse response, String message, int retryAfterSeconds) throws IOException {
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    }

    /**
     * Sends a 500 Internal Server Error response.
     * @param response the HttpServletResponse object
     * @param message the error message
     * @throws IOException if an error occurs during response writing
     */
    public static void sendInternalError(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

}
