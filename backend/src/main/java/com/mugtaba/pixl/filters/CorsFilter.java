package com.mugtaba.pixl.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter to handle Cross-Origin Resource Sharing (CORS) for API requests.
 * Allows controlled access from specified origins.
 */
@WebFilter("/api/*")
public class CorsFilter implements Filter {

    // allowed origins
    private static final Set<String> ALLOWED_ORIGINS = new HashSet<>(Arrays.asList(
       "http://localhost:5173",
       "http://localhost:3000"
    ));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");

        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type Authorization, X-Requested-With, X-CSRF-Token");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
        }

        // handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(httpRequest, httpResponse);
    }
}
