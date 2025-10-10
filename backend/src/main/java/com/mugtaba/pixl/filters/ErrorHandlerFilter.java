package com.mugtaba.pixl.filters;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.util.JsonUtil;
import com.mugtaba.pixl.util.LogUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Filter to handle errors for API requests.
 * This filter intercepts all requests and checks for unhandled exceptions or 404 errors on API endpoints.
 * If an exception or 404 error is encountered, it logs the error and sends an appropriate response.
 */
@WebFilter("/*")
public class ErrorHandlerFilter implements Filter {

    /**
     * Handles the filtering of requests and responses.
     * 
     * @param request the ServletRequest object
     * @param response the ServletResponse object
     * @param chain the FilterChain object to pass the request and response to the next entity in the chain
     * @throws IOException if an input or output exception occurs
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Wrap response to capture output and status
        ResponseCapture responseCapture = new ResponseCapture(httpResponse);
        
        try {
            chain.doFilter(request, responseCapture);
            
            // Check if this is an API request and no content was written
            if (httpRequest.getRequestURI().startsWith("/api/")) {
                
                // If 404 and no response was written by servlets, send API error
                if (responseCapture.getStatus() == HttpServletResponse.SC_NOT_FOUND && 
                    !responseCapture.hasContent()) {
                    
                    handleApiNotFound(httpRequest, httpResponse);
                    return;
                }
                
                // If response was written by servlet, copy it to original response
                if (responseCapture.hasContent()) {
                    httpResponse.setStatus(responseCapture.getStatus());
                    httpResponse.setContentType(responseCapture.getContentType());
                    httpResponse.getWriter().write(responseCapture.getContent());
                    return;
                }
            }
            
            // For non-API requests, copy response normally
            if (responseCapture.hasContent()) {
                httpResponse.setStatus(responseCapture.getStatus());
                httpResponse.setContentType(responseCapture.getContentType());
                httpResponse.getWriter().write(responseCapture.getContent());
            }
            
        } catch (Exception e) {
            LogUtil.logError(
                "ErrorHandlerFilter", "doFilter", 
                "Unhandled exception in request processing", e
            );
            
            if (httpRequest.getRequestURI().startsWith("/api/")) {
                sendApiErrorResponse(
                    httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred. Please try again later."
                );
            } else {
                throw e;
            }
        }
    }

    /**
     * Handles 404 Not Found errors for API endpoints.
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws IOException if an input or output exception occurs
     */
    private void handleApiNotFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        LogUtil.logWarning(
            "ErrorHandlerFilter", "handleApiNotFound",
            String.format("API endpoint not found: %s %s", method, uri)
        );

        String message = String.format("The API endpoint '%s %s' not found. Please check the URL and try again.", method, uri);

        sendApiErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, message);
    }

    /**
     * Sends a standardized JSON error response for API requests.
     * @param response the HttpServletResponse object
     * @param statusCode the HTTP status code to set in the response
     * @param message the error message to include in the response body
     * @throws IOException if an input or output exception occurs
     */
    private void sendApiErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        JsonUtil.writeJson(response.getWriter(), apiResponse);
    }

    /**
     * Wrapper for {HttpServletResponse} to check if the response has been written to.
     */
    private static class ResponseCapture extends HttpServletResponseWrapper {
        private final StringWriter stringWriter;
        private final PrintWriter writer;
        private int status = SC_OK;
        private String contentType = "application/json";
        
        public ResponseCapture(HttpServletResponse response) {
            super(response);
            stringWriter = new StringWriter();
            writer = new PrintWriter(stringWriter);
        }
        
        @Override
        public PrintWriter getWriter() throws IOException {
            return writer;
        }
        
        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }
        
        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }
        
        @Override
        public void setContentType(String type) {
            this.contentType = type;
            super.setContentType(type);
        }
        
        public int getStatus() {
            return status;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public String getContent() {
            writer.flush();
            return stringWriter.toString();
        }
        
        public boolean hasContent() {
            writer.flush();
            return stringWriter.toString().length() > 0;
        }
    }

}
