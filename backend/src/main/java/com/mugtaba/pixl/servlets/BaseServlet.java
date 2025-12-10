package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.exceptions.UnauthorizedException;
import com.mugtaba.pixl.exceptions.ValidationException;
import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.util.JsonUtil;
import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.ValidationUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base servlet class providing common functionality for all API servlets.
 * Handles JSON communication, standardized responses, and common utilities.
 */
public abstract class BaseServlet extends HttpServlet {

    /**
     * Extracts request data from JSON body only.
     * This method enforces JSON-only communication across all endpoints.
     * 
     * @param request the HttpServletRequest object
     * @return a map of request data
     * @throws ValidationException if JSON parsing fails or content type is not JSON
     */
    protected Map<String, String> extractRequestData(HttpServletRequest request) throws ValidationException {
        String contentType = request.getContentType();

        if (contentType == null || !contentType.contains("application/json")) {
            throw new ValidationException("Content-Type must be 'application/json'");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = JsonUtil.fromRequest(request, Map.class);

            Map<String, String> requestData = new HashMap<>();
            if (json != null) {
                json.forEach((key, value) -> {
                    if (value != null) {
                        requestData.put(key, value.toString().trim());
                    }
                });
            }

            return requestData;
        } catch (Exception e) {
            String componentName = this.getClass().getSimpleName();
            LogUtil.logError(componentName, "extractRequestData", "Failed to parse JSON", e);
            throw new ValidationException("Invalid JSON format in request body");
        }
    }

    /**
     * Extracts typed object from JSON request body.
     * 
     * @param request the HttpServletRequest object
     * @param clazz the target class type
     * @param <T> the type parameter
     * @return the parsed object
     * @throws ValidationException if parsing fails
     */
    protected <T> T extractRequestObject(HttpServletRequest request, Class<T> clazz) throws ValidationException {
        String contentType = request.getContentType();

        if (contentType == null || !contentType.contains("application/json")) {
            throw new ValidationException("Content-Type must be 'application/json'");
        }

        try {
            T object = JsonUtil.fromRequest(request, clazz);
            if (object == null) {
                throw new ValidationException("Invalid or missing " + clazz.getSimpleName().toLowerCase() + " data");
            }

            return object;
        } catch (Exception e) {
            String componentName = this.getClass().getSimpleName();
            LogUtil.logError(componentName, "extractRequestObject", "Failed to parse " + clazz.getSimpleName(), e);
            throw new ValidationException("Invlaid JSON format in request body");
        }
    }

    /**
     * Retrieves the user ID from the current session.
     * 
     * @param request the HttpServletRequest object
     * @return the user ID if present, otherwise null
     */
    protected Long getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            Object userId = session.getAttribute("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            }
        }

        return null;
    }

    /**
     * Gets user info from session for logging and validation.
     * 
     * @param request the HttpServletRequest object
     * @return map containing user session info
     */
    protected Map<String, Object> getUserSessionInfo(HttpServletRequest request) {
        Map<String, Object> sessionInfo = new HashMap<>();
        HttpSession session = request.getSession(false);

        if (session != null) {
            sessionInfo.put("userId", session.getAttribute("userId"));
            sessionInfo.put("username", session.getAttribute("username"));
            sessionInfo.put("email", session.getAttribute("userEmail"));
        }

        return sessionInfo;
    }

    /**
     * Validates user authentication and returns user ID.
     * 
     * @param request the HttpServletRequest object
     * @return the authenticated user ID
     * @throws UnauthorizedException if user is not authenticated
     */
    protected Long requireAuthentication(HttpServletRequest request) throws UnauthorizedException {
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            throw new UnauthorizedException(
                "Authentication required. Please log in to access this resource."
            );
        }
        return userId;
    }

    /**
     * Sends a success response with the specified message and data.
     * 
     * @param response the HttpServletResponse object
     * @param message an informational message about the operation
     * @param data the payload data to be returned
     * @throws IOException if an error occurs during response writing
     */
    protected void sendSuccessResponse(HttpServletResponse response, String message, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.success(message, data);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Sends a success response with message only (no data).
     * 
     * @param response the HttpServletResponse object
     * @param message an informational message about the operation
     * @throws IOException if an error occurs during response writing
     */
    protected void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        sendSuccessResponse(response, message, null);
    }

    /**
     * Sends an error response with the specified status code and message.
     * 
     * @param response the HttpServletResponse object
     * @param statusCode the HTTP status code
     * @param message an error message to be included in the response
     * @throws IOException if an error occurs during response writing
     */
    protected void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Sends a created response (HTTP 201) with message and data.
     * 
     * @param response the HttpServletResponse object
     * @param message success message
     * @param data the created resource data
     * @throws IOException if an error occurs during response writing
     */
    protected void sendCreatedResponse(HttpServletResponse response, String message, Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        sendSuccessResponse(response, message, data);
    }


    /**
     * Creates pagination information for responses.
     * 
     * @param currentPage the current page number
     * @param limit the number of items per page
     * @param totalCount the total number of items
     * @return a map containing pagination details
     */
    protected Map<String, Object> createPaginationInfo(int currentPage, int limit, int totalCount) {
        Map<String, Object> pagination = new HashMap<>();

        int totalPages = (int) Math.ceil((double) totalCount / limit);

        pagination.put("currentPage", currentPage);
        pagination.put("limit", limit);
        pagination.put("totalCount", totalCount);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", currentPage < totalPages);
        pagination.put("hasPrevious", currentPage > 1);

        return pagination;
    }

    /**
     * Validates pagination parameters and returns validated values.
     * 
     * @param request the HttpServletRequest object
     * @return map with validated page and limit values
     * @throws ValidationException if parameters are invalid
     */
    protected Map<String, Integer> validateAndGetPagination(HttpServletRequest request) throws ValidationException {
        int page = ValidationUtil.parsePageParameter(request.getParameter("page"));
        int limit = ValidationUtil.parseLimitParameter(request.getParameter("limit"));

        ValidationUtil.validatePagination(page, limit);

        Map<String, Integer> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("offset", (page - 1) * limit);

        return pagination;
    }
}
