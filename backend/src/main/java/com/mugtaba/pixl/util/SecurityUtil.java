package com.mugtaba.pixl.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.regex.Pattern;

/**
 * Utility class for security-related operations including input sanitization,
 * CSRF protection, and XSS prevention.
 */
public class SecurityUtil {

    // XSS prevention patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script>[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        """
        ('|(--)|;|\\|\\||\\|)|exec|execute|select|insert|update|delete|drop|create|alter|union|into|load_file|outfile
        """,
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitizes input to prevent XSS attacks.
     * @param input the input string to sanitize
     * @return sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // remove script tags
        String sanitized = SCRIPT_PATTERN.matcher(input).replaceAll("");

        // escape html special characters
        sanitized = sanitized
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");

        return sanitized;
    }

    /**
     * Validates input for potential SQL injection attempts
     * @param input the input string to validate
     * @return true if input appears safe, false if suspicious
     */
    public static boolean isSqlSafe(String input) {
        if (input == null) {
            return true;
        }

        return !SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Sets security headers in HTTP response.
     * @param response the HTTPServletResponse object
     */
    public static void setSecurityHeaders(HttpServletResponse response) {

        response.setHeader("X-Frame-Options", "DENY"); // prevent clickjacking
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff"); // prevent MIME sniffing
        response.setHeader(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'"
        );
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    }

    /**
     * Gets client IP address from request, considering proxy headers.
     * @param request the HttpServletRequest object
     * @return client IP address
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // get first IP if multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Validates email format.
     * @param email the email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    /**
     * Checks if a string contains only alphanumeric characters and allowed special chars.
     * @param input the input string
     * @param allowedChars additional allowed characters
     * @return true if valid, false otherwise
     */
    public static boolean isAlphanumeric(String input, String allowedChars) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String pattern = "^[a-zA-Z0-9" + Pattern.quote(allowedChars) + "]+$";
        return Pattern.matches(pattern, input);
    }

}
