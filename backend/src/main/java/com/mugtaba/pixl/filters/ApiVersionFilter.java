package com.mugtaba.pixl.filters;

import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.ResponseUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter to validate API version in request path.
 * Ensures clients use supported API versions and provides
 * clear error messages for unsupported vsersions.
 */
@WebFilter("/api/*")
public class ApiVersionFilter implements Filter {

    private static final String COMPONENT_NAME = "ApiVersionFilter";

    // supported API version
    private static final String LATEST_VERSION = "v1";
    private static final String[] SUPPORTED_VERSIONS = {"v1"};

    // pattern to match versioned API paths: /api/v{number}/...
    private static final Pattern VERSION_PATTERN = Pattern.compile("^/api/(v\\d+)(/.*)?$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // remove context path to get the actual API path
        String apiPath = requestUri.substring(contextPath.length());

        // check if this is a versioned API request
        Matcher matcher = VERSION_PATTERN.matcher(apiPath);

        if (matcher.matches()) {
            String version = matcher.group(1);

            if (!isVersionSupported(version)) {
                LogUtil.logWarning(
                    COMPONENT_NAME, "doFilter",
                    String.format(
                        "Unsupported API version requested: %s from IP: %s",
                        version, request.getRemoteAddr()
                    )
                );

                ResponseUtil.sendError(
                    httpResponse,
                    HttpServletResponse.SC_NOT_FOUND,
                    String.format(
                        "API version %s is not supported. Supported versions: %s. Latest version: %s.",
                        version, String.join(", ", SUPPORTED_VERSIONS), LATEST_VERSION
                    )
                );
                return;
            }

            // add version to request attributes for servlet to use if needed
            httpRequest.setAttribute("api.version", version);

            LogUtil.logInfo(
                COMPONENT_NAME, "doFilter",
                String.format(
                    "API request: %s %s (version: %s)",
                    httpRequest.getMethod(), apiPath, version
                )
            );

        } else if (apiPath.startsWith("/api/")) {
            // API request without version
            LogUtil.logWarning(
                COMPONENT_NAME, "doFilter",
                String.format("API request without version: %s from IP: %s", apiPath, request.getRemoteAddr())
            );

            ResponseUtil.sendError(
                httpResponse, HttpServletResponse.SC_BAD_REQUEST,
                String.format("API version is required. Please use /api/%s/... format.", LATEST_VERSION)
            );
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Checks if the requested API version is supported.
     * @param version the version to check (e.g., "v1")
     * @return true if supported, false otherwise
     */
    private boolean isVersionSupported(String version) {
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            if (supportedVersion.equalsIgnoreCase(version)) {
                return true;
            }
        }
        return false;
    }

}
