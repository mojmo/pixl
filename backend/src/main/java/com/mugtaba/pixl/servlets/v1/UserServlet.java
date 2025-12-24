package com.mugtaba.pixl.servlets.v1;

import com.mugtaba.pixl.exceptions.*;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.services.UserService;
import com.mugtaba.pixl.servlets.BaseServlet;
import com.mugtaba.pixl.util.*;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * User profile servlet for API version 1.
 * Handles user profile management operations.
 * 
 * Endpoints:
 * - GET /api/v1/users/me - Get current user profile
 * - PUT /api/v1/users/me - Update current user profile
 * 
 * @version 1.0
 * @since 1.0
 */
@WebServlet("/api/v1/users/*")
public class UserServlet extends BaseServlet {

    private static final String COMPONENT_NAME = "UserServlet[v1]";
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
        LogUtil.logInfo(COMPONENT_NAME, "init", "UserServlet v1 initialized successfully");
    }

    /**
     * Handles HTTP GET requests for user profile endpoints.
     * 
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null) {
                throw new ValidationException("User endpoint is required");
            }

            if (pathInfo.equals("/me")) {
                handleGetProfile(request, response);
            } else {
                throw new ValidationException("Invalid user endpoint: " + pathInfo);
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doGet", e.getLogMessage(), e);
            ResponseUtil.sendError(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doGet", "Unexpected error in GET request", e);
            ResponseUtil.sendInternalError(response, "Unable to retrieve user profile. Please try again later.");
        }
    }

    /**
     * Handles HTTP PUT requests for user profile updates.
     * 
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null) {
                throw new ValidationException("User endpoint is required");
            }

            if (pathInfo.equals("/me")) {
                handleUpdateProfile(request, response);
            }else {
                throw new ValidationException("Invalid user endpoint: " + pathInfo);
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doPut", e.getLogMessage(), e);
            ResponseUtil.sendError(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doPut", "Unexpected error in PUT request", e);
            ResponseUtil.sendInternalError(response, "Unable to update user profile. Please try again later.");
        }
    }

    /**
     * Handles retrieving the current user's profile.
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     * @throws SQLException if a database error occurs
     * @throws PixlException if there is a validation error
     */
    private void handleGetProfile(HttpServletRequest request, HttpServletResponse response)
            throws PixlException, SQLException, IOException {

        Long userId = requireAuthentication(request);

        Optional<User> userOpt = userService.getUserById(userId);

        if (userOpt.isEmpty()) {
            throw new ResourceNotFoundException("User profile");
        }

        LogUtil.logInfo(
            COMPONENT_NAME, "handleGetProfile",
            String.format("User %d retrieved profile successfully", userId)
        );

        ResponseUtil.sendSuccess(response, "User profile retrieved successfully", userOpt.get());
    }

    /**
     * Handles updating the current user's profile.
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     * @throws SQLException if a database error occurs
     * @throws PixlException if there is a validation error
     */
    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response)
            throws PixlException, SQLException, IOException {

        Long userId = requireAuthentication(request);

        Map<String, String> requestData = extractRequestData(request);

        String newUsername = requestData.get("username");
        String newEmail = requestData.get("email");

        // Validate inputs
        ValidationUtil.validateStringNotEmpty(newUsername, "Username");
        ValidationUtil.validateStringNotEmpty(newEmail, "Email");
        ValidationUtil.validateStringLength(newUsername, "Username", 3, 50);
        ValidationUtil.validateStringLength(newEmail, "Email", 5, 100);

        newUsername = sanitize(newUsername);
        newEmail = sanitize(newEmail);

        // Validate username format
        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            throw new ValidationException("Username can only contain letters, numbers, and underscores");
        }

        // Validate email format
        if (!ValidationUtil.isValidEmailFormat(newEmail)) {
            throw new ValidationException("Please provide a valid email address");
        }

        validateSqlSafe(newUsername, "username");
        validateSqlSafe(newEmail, "email");

        // Update profile
        boolean updated = userService.updateProfile(userId, newUsername, newEmail);

        if (!updated) {
            throw new DatabaseException("profile update", new Exception("Update operation failed"));
        }

        // Update session
        HttpSession session = request.getSession();
        session.setAttribute("username", newUsername);
        session.setAttribute("userEmail", newEmail);

        LogUtil.logInfo(
            COMPONENT_NAME, "handleUpdateProfile",
            String.format("User %d updated profile successfully", userId)
        );

        ResponseUtil.sendSuccess(response, "Profile updated successfully");
    }
}
