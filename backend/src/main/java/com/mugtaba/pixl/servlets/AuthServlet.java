package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.services.UserService;
import com.mugtaba.pixl.util.JsonUtil;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

/**
 * Servlet handling user authentication operations including registration and login.
 * Maps to "/auth/*" URL pattern and processes POST requests for authentication endpoints.
 */
@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {

    /** Service class for user-related operations */
    private final UserService userService = new UserService();

    /**
     * Handles HTTP POST requests for authentication endpoints.
     * Routes requests to appropriate handlers based on the path info.
     *
     * @param request  the HttpServletRequest object containing client request data
     * @param response the HttpServletResponse object for sending responses
     * @throws IOException if an input or output error occurs during request handling
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
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Endpoint not found"));
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handles user registration requests.
     * Validates input parameters and attempts to register a new user.
     *
     * @param request  the HttpServletRequest containing registration data
     * @param response the HttpServletResponse for sending registration result
     * @throws IOException if an error occurs during response writing
     */
    private void handleRegister(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Extract parameters from request
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (username == null || email == null || password == null ||
            username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Username, email, and password are required"));
            return;
        }

        // validate password strength
        if (password.length() < 8) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Password must be at least 8 characters long"));
            return;
        }

        try {
            if (userService.registerUser(username, email, password)) {
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(true, "User registered successfully", null));
            } else {
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Registration failed"));
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, e.getMessage()));
        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Handles user login requests.
     * Authenticates user credentials and creates a session upon successful login.
     *
     * @param request  the HttpServletRequest containing login credentials
     * @param response the HttpServletResponse for sending login result
     * @throws IOException if an error occurs during response writing
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Extract parameters from request
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Validate required parameters
        if (username == null || password == null ||
                username.trim().isEmpty() || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(response,
                    new ApiResponse<>(false, "Username and password are required"));
            return;
        }

        try {
            User user = userService.loginUser(username, password);

            if (user != null) {
                HttpSession session = request.getSession();
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("userEmail", user.getEmail());

                // Set session timeout (30 minutes)
                session.setMaxInactiveInterval(30 * 60);

                response.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(true, "Logged in successfully", user));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "'Invalid credentials"));
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Handles exceptions that occur during request processing.
     * Logs the exception and returns an appropriate error response.
     *
     * @param response the HttpServletResponse for sending error response
     * @param e        the exception that occurred
     * @throws IOException if an error occurs during response writing
     */
    private void handleException(HttpServletResponse response, Exception e) throws IOException {
        System.err.println("Error: " + e.getMessage());

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "An internal server error occurred"));
    }

    /**
     * Handles HTTP GET requests for logout functionality.
     * Invalidates the current user session.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        if ("/logout".equals(pathInfo)) {
            handleLogout(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonUtil.writeJsonResponse(response, new ApiResponse<>(false, "Endpoint not found"));
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

        JsonUtil.writeJsonResponse(response, new ApiResponse<>(true,"Logged out successfully", null));
    }
}
