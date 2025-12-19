package com.mugtaba.pixl.filters;

import com.mugtaba.pixl.util.SecurityUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to add security headers to all responses
 */
@WebFilter("/api/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        SecurityUtil.setSecurityHeaders(httpServletResponse);

        chain.doFilter(request, response);
    }

}
