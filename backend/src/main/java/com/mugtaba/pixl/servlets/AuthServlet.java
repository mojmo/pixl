package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.services.UserService;
import com.mugtaba.pixl.util.JsonUtil;
import com.mugtaba.pixl.util.PasswordUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
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
public class AuthServlet extends HttpServlet {

    /** Service class for user-related operations */
    private UserService userService;

    /** Initializes the servlet */
    @Override
    public void init() {
        userService = new UserService();
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
            if ("/register".equals(pathInfo)) {
                handleRegister(request, response);
            } else if ("/login".equals(pathInfo)) {
                handleLogin(request, response);
            } else {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "Endpoint not found"
                );
            }
        } catch (Exception e) {
            System.err.println("Error in AuthServlet: " + e.getMessage());
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "An internal server error occurred"
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
                case null, default -> sendErrorResponse(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Endpoint not found"
                );
            }
        } catch (Exception e) {
            System.err.println("Error in AuthServlet: " + e.getMessage());
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "An internal server error occurred"
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

        if ("/profile".equals(pathInfo)) {
            handleUpdateProfile(request, response);
        } else if ("/password".equals(pathInfo)) {
            handleChangePassword(request, response);
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
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
    private void handleRegister(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            // Try to parse JSON first, fall back to form parameters
            Map<String, String> credentials = extractCredentials(request);

            String username = credentials.get("username");
            String email = credentials.get("email");
            String password = credentials.get("password");

            // Validate required fields
            if (username == null || email == null || password == null ||
                username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
                    sendErrorResponse(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Username, email, and password are required"
                    );
                    return;
            }

            // Validate password strength
            if (PasswordUtil.isWeakPassword(password)) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character"
                );
                return;
            }

            // Register user
            User newUser = userService.registerUser(username.trim(), email.trim(), password);

            // Return user data without sensitive information
            User publicUser = newUser.toPublicUser();
            sendSuccessResponse(response, "User registered successfully", publicUser);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Registration failed due to server error"
            );
        } catch (Exception e) {
            System.err.println("Unexpected error during registration: " + e.getMessage());
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Registration failed"
            );
        }
    }

    /**
     * Handles user login requests.
     * Authenticates a user with the provided username or email and password.
     * 
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            // Try to parse JSON first, fall back to form parameters
            Map<String, String> credentials = extractCredentials(request);

            String usernameOrEmail = credentials.get("username");
            String password = credentials.get("password");

            // Validate required fields
            if (usernameOrEmail == null || password == null ||
                usernameOrEmail.trim().isEmpty() || password.trim().isEmpty()) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Username (or email) and password are required"
                );
                return;
            }

            // Authenticate user
            Optional<User> userOpt = userService.authenticateUser(usernameOrEmail.trim(), password.trim());

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                System.out.println("\nUser: ");
                System.out.println(user);

                // Create a session
                HttpSession session = request.getSession();
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("userEmail", user.getEmail());
                session.setMaxInactiveInterval(30 * 60); // 30 minutes

                // Prepare response data
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("user", user.toPublicUser());
                responseData.put("sessionId", session.getId());


                System.out.println("\nPublic User: ");
                System.out.println(user.toPublicUser());

                System.out.println("\nresponse: ");
                System.out.println(responseData);

                sendSuccessResponse(response, "Logged in successfully", responseData);
            } else {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid credentials"
                );
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Login failed due to server error"
            );
        } catch (Exception e) {
            System.err.println("Unexpected error during login: " + e.getMessage());
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Login failed"
            );
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
            session.invalidate();
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
    private void handleGetProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            sendErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "User not authenticated");
            return;
        }

        try {
            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isPresent()) {
                sendSuccessResponse(
                    response,
                    "User profile retrieved successfully",
                    userOpt.get().toPublicUser()
                );
            } else {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "User not found"
                );
            }
        } catch (SQLException e) {
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error retrieving profile"
            );
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
    private void handleCheckSession(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            sendErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "No active session"
            );
            return;
        }

        HttpSession session = request.getSession(false);
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("userId", userId);
        sessionInfo.put("username", session.getAttribute("username"));
        sessionInfo.put("email", session.getAttribute("userEmail"));
        sessionInfo.put("sessionId", session.getId());
        sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());

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
    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            sendErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "User not authenticated"
            );
            return;
        }

        try {
            Map<String, String> updates = extractCredentials(request);
            String newUsername = updates.get("username");
            String newEmail = updates.get("email");

            if (newUsername == null || newEmail == null || 
                newUsername.trim().isEmpty() || newEmail.trim().isEmpty()) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST, 
                    "Username and email are required"
                );
                return;
            }

            boolean updated = userService.updateProfile(userId, newUsername.trim(), newEmail.trim());

            if (updated) {
                // Update session
                HttpSession session = request.getSession();
                session.setAttribute("username", newUsername.trim());
                session.setAttribute("userEmail", newEmail.trim());

                sendSuccessResponse(response, "Profile updated successfully", null);
            } else {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Failed to update profile"
                );
            }
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error updating profile: " + e.getMessage());
            sendErrorResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error updating profile"
            );
        }
    }

    /**
     * Handles password change requests.
     * Changes the authenticated user's password.
     *
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an error occurs during response writing
     */
    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            sendErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "User not authenticated"
            );
            return;
        }

        try {
            Map<String, String> passwords = extractCredentials(request);
            String currentPassword = passwords.get("currentPassword");
            String newPassword = passwords.get("newPassword");

            if (currentPassword == null || newPassword == null || 
                currentPassword.trim().isEmpty() || newPassword.trim().isEmpty()) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST, 
                    "Current password and new password are required"
                );
                return;
            }

            if (PasswordUtil.isWeakPassword(newPassword)) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New Password must be at least 8 characters long and include uppercase, lowercase, number, and special character"
                );
                return;
            }

            boolean updated = userService.updatePassword(userId, currentPassword, newPassword);
            if (updated) {
                sendSuccessResponse(response, "Password updated successfully", null);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update password");
            }
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error updating password");
        }
    }

    /**
     * Extracts credentials from the request, supporting both JSON and form data.
     *
     * @param request the HttpServletRequest containing the credentials
     * @return a map of credential keys and values
     */
    private Map<String, String> extractCredentials(HttpServletRequest request) {
        Map<String, String> credentials = new HashMap<>();

        String contentType = request.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            // Parse JSON
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = JsonUtil.fromRequest(request, Map.class);

                if (json != null) {
                    json.forEach((key, value) -> credentials.put(key, value.toString()));
                }
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + e.getMessage());
            }
        } else {
            // Fallback to form parameters
            request.getParameterMap().forEach((key, value) -> {
                if (value.length > 0) {
                    credentials.put(key, value[0]);
                }
            });
        }

        return credentials;
    }

    /**
     * Retrieves the user ID from the current session.
     *
     * @param request the HttpServletRequest object
     * @return the user ID if present, otherwise null
     */
    private Long getUserIdFromSession(HttpServletRequest request) {
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
     * Sends a success response with the specified message and data.
     *
     * @param response the HttpServletResponse object
     * @param message an informational message about the operation
     * @param data the payload data to be returned
     * @throws IOException if an error occurs during response writing
     */
    private void sendSuccessResponse(HttpServletResponse response, String message, Object data)
        throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.success(message, data);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Sends an error response with the specified status code and message.
     *
     * @param response the HttpServletResponse object
     * @param statusCode the HTTP status code
     * @param message an error message to be included in the response
     * @throws IOException if an error occurs during response writing
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message)
        throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

}
