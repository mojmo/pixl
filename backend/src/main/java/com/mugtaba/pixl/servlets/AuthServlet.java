package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.exceptions.*;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.services.UserService;
import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.PasswordUtil;
import com.mugtaba.pixl.util.ValidationUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servlet handling user authentication operations including registration and login, logout, and profile updates.
 * Maps to "/auth/*" URL pattern and processes POST, GET, and PUT requests for authentication endpoints.
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {

    private static final String COMPONENT_NAME = "AuthServlet";
    private UserService userService;

    /** Initializes the servlet */
    @Override
    public void init() {
        userService = new UserService();
        LogUtil.logInfo(COMPONENT_NAME, "init", "AuthServlet initialized successfully");
    }

    /**
     * Handles HTTP POST requests for authentication endpoints.
     * Processes user registration and login requests.
     * 
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        try {
            switch (pathInfo) {
                case "/register" -> handleRegister(request, response);
                case "/login" -> handleLogin(request, response);
                default -> throw new ResourceNotFoundException("Authentication endpoint");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doPost", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doPost", "Unexpected error in POST request", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to complete authentication. Please try again later."
            );
        }
    }

    /**
     * Handles HTTP GET requests for authentication endpoints.
     * Processes user logout, profile, and session check requests.
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
            switch (pathInfo) {
                case "/logout" -> handleLogout(request, response);
                case "/profile" -> handleGetProfile(request, response);
                case "/session" -> handleCheckSession(request, response);
                default -> throw new ResourceNotFoundException("Authentication endpoint");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doGet", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(
                COMPONENT_NAME, "doGet",
                "Unexpected error in GET request", e
            );
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to complete request. Please try again later."
            );
        }
    }

    /**
     * Handles HTTP PUT requests for authentication endpoints.
     * Processes user profile update and password change requests.
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
            if (pathInfo.equals("/profile")) {
                handleUpdateProfile(request, response);
            } else {
                throw new ResourceNotFoundException("Authentication endpoint");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doPut", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(COMPONENT_NAME, "doPut", "Unexpected error in PUT requests", e);
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to update information. Please try again later"
            );
        }
    }

    /**
     * Handles user registration requests.
     * Registers a new user with the provided username, email, and password.
     * 
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, SQLException, IOException {

        Map<String, String> credentials = extractRequestData(request);

        String username = credentials.get("username");
        String email = credentials.get("email");
        String password = credentials.get("password");

        // Validate required fields
        ValidationUtil.validateStringNotEmpty(username, "Username");
        ValidationUtil.validateStringNotEmpty(email, "Email");
        ValidationUtil.validateStringNotEmpty(password, "Password");

        // Validate field lengths and formats
        ValidationUtil.validateStringLength(username.trim(), "Username", 3, 50);
        ValidationUtil.validateStringLength(email.trim(), "Email", 5, 100);
        ValidationUtil.validateStringLength(password, "Password", 8, 128);

        // Validate username format
        if (!username.trim().matches("^[a-zA-Z0-9_]+$")) {
            throw new ValidationException("Username can only contain letters, numbers, and underscores");
        }

        // Validate email format
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ValidationException("Please provide a valid email address");
        }

        // Validate password strength
        if (PasswordUtil.isWeakPassword(password)) {
            throw new ValidationException("Password must contain at least 3 of: uppercase letter, lowercase letter, number, special character");
        }

        User newUser = userService.registerUser(username.trim(), email.trim(), password);
        User publicUser = newUser.toPublicUser();

        LogUtil.logInfo(
            COMPONENT_NAME, "handleRegister",
            String.format("User registered successfully: %s (ID: %d)", username.trim(), newUser.getId())
        );

        sendCreatedResponse(response, "Account created successfully", publicUser);
    }

    /**
     * Handles user login requests.
     * Authenticates a user with the provided username or email and password.
     * 
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, SQLException, IOException {

        Map<String, String> credentials = extractRequestData(request);

        String usernameOrEmail = credentials.get("username");
        String password = credentials.get("password");

        ValidationUtil.validateStringNotEmpty(usernameOrEmail, "Username or email");
        ValidationUtil.validateStringNotEmpty(password, "Password");

        Optional<User> userOpt = userService.authenticateUser(usernameOrEmail.trim(), password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Create a session
            HttpSession session = request.getSession();
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("userEmail", user.getEmail());
            session.setMaxInactiveInterval(2 * 60 * 60); // 2 hours

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", user.toPublicUser());
            responseData.put("sessionId", session.getId());

            LogUtil.logInfo(
                COMPONENT_NAME, "handleLogin",
                String.format("User logged in successfully: %s (ID: %d)", user.getUsername(), user.getId())
            );
            sendSuccessResponse(response, "Logged in successfully", responseData);
        } else {
            LogUtil.logWarning(
                COMPONENT_NAME, "handleLogin",
                String.format("Failed login attempt for: %s", usernameOrEmail.trim())
            );
            throw new UnauthorizedException("Invalid username/email or password");
        }
    }

    /**
     * Handles user logout requests.
     * Invalidates the current session and clears authentication data.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an error occurs during response writing
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session != null) {
            String username = (String) session.getAttribute("username");
            Long userId = (Long) session.getAttribute("userId");

            session.invalidate();

            LogUtil.logInfo(
                COMPONENT_NAME, "handleLogout",
                String.format("User logged out: %s (ID: %d)", username, userId)
            );
        }

        sendSuccessResponse(response, "Logged out successfully", null);
    }

    /**
     * Handles user profile requests.
     * Retrieves the authenticated user's profile data.
     * 
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    private void handleGetProfile(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, SQLException, IOException {
        
        Long userId = requireAuthentication(request);

        Optional<User> userOpt = userService.getUserById(userId);

        if (userOpt.isPresent()) {
            sendSuccessResponse(response, "User profile retrieved successfully", userOpt.get().toPublicUser());
        } else {
            throw new ResourceNotFoundException("User profile");
        }
    }

    /**
     * Handles session check requests.
     * Checks if the user is authenticated and returns session information.
     *
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an error occurs during response writing
     */
    private void handleCheckSession(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, IOException {

        requireAuthentication(request);

        Map<String, Object> sessionInfo = getUserSessionInfo(request);

        sessionInfo.put("sessionId", request.getSession().getId());
        sessionInfo.put("maxInactiveInterval", request.getSession().getMaxInactiveInterval());

        sendSuccessResponse(response, "Session is active", sessionInfo);
    }

    /**
     * Handles user profile update requests.
     * Updates the authenticated user's profile data.
     * 
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an error occurs during response writing
     */
    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, SQLException, IOException {

        Long userId = requireAuthentication(request);

        Map<String, String> updates = extractRequestData(request);

        String newUsername = updates.get("username").trim();
        String newEmail = updates.get("email").trim();

        ValidationUtil.validateStringNotEmpty(newUsername, "Username");
        ValidationUtil.validateStringNotEmpty(newEmail, "Email");
        ValidationUtil.validateStringLength(newUsername, "Username", 3, 50);
        ValidationUtil.validateStringLength(newEmail, "Email", 5, 100);

        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            throw new ValidationException("Username can only contain letters, numbers, and underscores");
        }

        if (!ValidationUtil.isValidEmail(newEmail)) {
            throw new ValidationException("Please provide a valid email address");
        }

        boolean updated = userService.updateProfile(userId, newUsername, newEmail);

        if (updated) {
            // Update session
            HttpSession session = request.getSession();
            session.setAttribute("username", newUsername);
            session.setAttribute("userEmail", newEmail);

            LogUtil.logInfo(
                COMPONENT_NAME, "handleUpdateProfile",
                String.format("User %d updated profile successfully", userId)
            );

            sendSuccessResponse(response, "Profile updated successfully", null);
        } else {
            throw new DatabaseException("profile update", new Exception("Update operation failed"));
        }
    }

}
