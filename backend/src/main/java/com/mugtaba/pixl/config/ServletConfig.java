package com.mugtaba.pixl.config;

import com.mugtaba.pixl.util.LogUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

/**
 * Servlet context listener for application configuration
 */
@WebListener
public class ServletConfig implements ServletContextListener {

    private static final String COMPONENT_NAME = "ServletConfig";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        LogUtil.logInfo(
            COMPONENT_NAME, "contextInitialized",
            "Initializing Pixl application"
        );

        configureSession(context);

        context.setAttribute("app.name", "Pixl API");
        context.setAttribute("app.version", "1.0.0");
        context.setAttribute("app.environment", getEnvironment());

        LogUtil.logInfo(
            COMPONENT_NAME, "contextInitialized",
            String.format("Pixl application initialized - Environment: %s", getEnvironment())
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LogUtil.logInfo(COMPONENT_NAME, "contextDestroyed", "Shutting down Pixl application");
        // cleanup resources if needed
    }

    /**
     * Configures session timeout and cookie settings
     * @param  context the servlet context
     */
    private void configureSession(ServletContext context) {
        SessionCookieConfig sessionConfig = context.getSessionCookieConfig();

        // set cookie properties
        sessionConfig.setName("PIXL_SESSION");
        sessionConfig.setHttpOnly(true);
        sessionConfig.setSecure(isProduction());
        sessionConfig.setMaxAge(2 * 60 * 60); // 2 hours
        sessionConfig.setPath("/");

        context.setSessionTimeout(2 * 60); // 2 hours

        LogUtil.logInfo(
            COMPONENT_NAME, "configureSession",
            String.format("Session configured: timeout=2h, httpOnly=true, secure=%s", isProduction())
        );
    }

    /**
     * Determines if the application is running in production environment
     */
    private boolean isProduction() {
        String env = System.getenv("APP_ENV");
        return "production".equalsIgnoreCase(env);
    }

    /**
     * Gets the current environment name
     */
    private String getEnvironment() {
        String env = System.getenv("APP_ENV");
        return env != null ? env : "development";
    }

}
