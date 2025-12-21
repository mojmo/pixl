package com.mugtaba.pixl.filters;

import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.ResponseUtil;

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
            ResponseUtil.sendUnauthorized(httpResponse, "Authentication required to access admin resources");
            return;
        }

        // Check if user is admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            LogUtil.logSecurity(
                COMPONENT_NAME, String.format(
                    "Non-admin user %s (ID: %s) attempted to access admin endpoint: %s", 
                    session.getAttribute("username"), session.getAttribute("userId"), httpRequest.getRequestURI())
                );
            ResponseUtil.sendForbidden(httpResponse, "Access denied. Admin privileges required.");
            return;
        }

        // User is authenticated and is admin - proceed
        LogUtil.logInfo(
            COMPONENT_NAME, "doFilter",
            String.format("Admin user %s accessing: %s", session.getAttribute("userId"), httpRequest.getRequestURI())
        );
        chain.doFilter(request, response);
    }
}
