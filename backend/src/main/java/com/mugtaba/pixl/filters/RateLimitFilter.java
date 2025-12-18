package com.mugtaba.pixl.filters;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.util.CacheUtil;
import com.mugtaba.pixl.util.JsonUtil;
import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.SecurityUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter to implement rate limiting for API endpoints.
 * Prevents brute force attacks and DDOS attempts.
 */
@WebFilter("/api/*")
public class RateLimitFilter implements Filter {

    private static final String COMPONENT_NAME = "RateLimitFilter";

    // rate limit configurations
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_LOGIN_ATTEMPTS_PER_HOUR = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    // cache keys
    private static final String RATE_LIMIT_KEY = "rate_limit_%s";
    private static final String LOGIN_ATTEMPT_KEY = "login_attempt_%s";
    private static final String LOCKOUT_KEY = "lockout_%s";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = SecurityUtil.getClientIp(httpRequest);
        String requestUri = httpRequest.getRequestURI();

        // check if IP is locked out
        if (isLockedOut(clientIp)) {
            LogUtil.logWarning(
                    COMPONENT_NAME, "doFilter",
                    String.format("Blocked request from locked out IP: %s", clientIp)
            );
            sendRateLimitResponse(httpResponse, "Too many failed attempts. Please try again later.");
        }

        // check general rate limit
        if (isRateLimited(clientIp)) {
            LogUtil.logWarning(
                    COMPONENT_NAME, "doFilter",
                    String.format("Rate limit exceeded for IP: %s, URI: %s", clientIp, requestUri)
            );
        }

        // track login attempts for brute force protection
        if (requestUri.contains("/api/auth/login")) {
            trackLoginAttempt(clientIp);
        }

        chain.doFilter(httpRequest,httpResponse);
    }

    /**
     * Checks if an IP exceeded the general rate limit
     * @param clientIp IP of the client
     * @return true if IP exceeded rate limit, false otherwise
     */
    private boolean isRateLimited(String clientIp) {
        String cacheKey = String.format(RATE_LIMIT_KEY, clientIp);
        AtomicInteger requestCount = CacheUtil.get(cacheKey, AtomicInteger.class);

        if (requestCount == null) {
            requestCount = new AtomicInteger(1);
            CacheUtil.put(cacheKey, requestCount, 1);
            return false;
        }

        int count = requestCount.incrementAndGet();
        return count > MAX_REQUESTS_PER_MINUTE;
    }

    /**
     * Checks if an IP currently locked out.
     * @param clientIp IP of the client
     * @return true if IP is locked out, false otherwise
     */
    private boolean isLockedOut(String clientIp) {
        String lockoutKey = String.format(LOCKOUT_KEY, clientIp);
        Boolean isLocked = CacheUtil.get(lockoutKey, Boolean.class);
        return isLocked != null && isLocked;
    }

    /**
     * Tracks login attempts for brute force protection
     * @param clientIp IP of the client
     */
    private void trackLoginAttempt(String clientIp) {
        String attemptKey = String.format(LOGIN_ATTEMPT_KEY, clientIp);
        AtomicInteger attempts = CacheUtil.get(attemptKey, AtomicInteger.class);

        if (attempts == null) {
            attempts = new AtomicInteger(1);
            CacheUtil.put(attemptKey, attempts, 60); // 1 hour TTL
        } else {
            int attemptCount = attempts.incrementAndGet();

            if (attemptCount > MAX_LOGIN_ATTEMPTS_PER_HOUR) {
                // lock out the IP
                String lockoutKey = String.format(LOCKOUT_KEY, clientIp);
                CacheUtil.put(lockoutKey, true, LOCKOUT_DURATION_MINUTES);

                    LogUtil.logWarning(
                            COMPONENT_NAME, "trackLoginAttempt",
                            String.format("IP locked out due to excessive login attempts: %s", clientIp)
                    );
            }
        }
    }

    /**
     * Sends a 429 Too many requests response with the specified message.
     * @param response the HttpServletResponse object
     * @param message the error message to include in the response
     * @throws IOException if an error occurs during response writing
     */
    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429); // Too many requests
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.setHeader("Retry-After", "60");

        ApiResponse<Object> apiResponse = ApiResponse.error(message, null, 429);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Resets login attempts for a successful login.
     * @param clientIp IP of the client
     */
    public static void resetLoginAttempts(String clientIp) {
        String attemptKey = String.format(LOGIN_ATTEMPT_KEY, clientIp);
        CacheUtil.remove(attemptKey);

        String lockoutKey = String.format(LOCKOUT_KEY, clientIp);
        CacheUtil.remove(lockoutKey);
    }

}
