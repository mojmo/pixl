package com.mugtaba.pixl.filters;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.util.JsonUtil;
import com.mugtaba.pixl.util.LogUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter that ensures only authenticated admin users can access admin endpoints.
 * Applied to all /api/admin/* routes.
 */
@WebFilter("/api/admin/*")
public class AdminAuthFilter implements Filter {

    private static final String COMPONENT_NAME = "AdminAuthFilter";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);

        // Get session
        if (session == null) {
            LogUtil.logWarning(
                COMPONENT_NAME, "doFilter",
                "Unauthorized admin access attempt - no session"
            );
            sendForbiddenResponse(httpResponse, "Authentication required. Please log in.");
            return;
        }

         // Check if user is authenticated
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            LogUtil.logWarning(COMPONENT_NAME, "doFilter", "Unauthorized admin access attempt - no userId in session");
            sendForbiddenResponse(httpResponse, "Authentication required. Please log in.");
            return;
        }

        // Check if user is admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            String username = (String) session.getAttribute("username");
            LogUtil.logSecurity(COMPONENT_NAME, String.format("doFilter", "Non-admin user %s (ID: %d) attempted to access admin endpoint: %s", username, userId, httpRequest.getRequestURI()));
            sendForbiddenResponse(httpResponse, "Access denied. Admin privileges required.");
            return;
        }

        // User is authenticated and is admin - proceed
        LogUtil.logInfo(COMPONENT_NAME, "doFilter", String.format("Admin user %d accessing: %s", userId, httpRequest.getRequestURI()));
        chain.doFilter(request, response);
    }

    /**
     * Sends a 403 Forbidden response with the specified message.
     * @param response the HttpServletResponse object
     * @param message the error message to include in the response
     * @throws IOException if an error occurs during response writing
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }
}
