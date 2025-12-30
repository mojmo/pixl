package com.mugtaba.pixl.integration;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.servlets.v1.UserServlet;
import com.mugtaba.pixl.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserServlet Integration Tests")
class UserServletIntegrationTest extends H2IntegrationTestBase {

    // Test wrapper to expose protected servlet methods
    private static class TestableUserServlet extends UserServlet {
        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doGet(request, response);
        }
        
        @Override
        public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doPut(request, response);
        }
    }

    private TestableUserServlet userServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        userServlet = new TestableUserServlet();
        userServlet.init();
        
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        
        when(response.getWriter()).thenReturn(printWriter);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getContentType()).thenReturn("application/json");
    }

    @Test
    @DisplayName("GET /me - WhenAuthenticated - Returns200AndUserData")
    void getProfile_WhenAuthenticated_Returns200AndUserData() throws Exception {
        // Arrange
        User user = userService.registerUser("profileuser", "profile@example.com", "Password123!");
        
        when(request.getPathInfo()).thenReturn("/me");
        when(session.getAttribute("userId")).thenReturn(user.getId());

        // Act
        userServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        Map<String, Object> userData = (Map<String, Object>) apiResponse.getData();
        assertThat(userData.get("username")).isEqualTo("profileuser");
        assertThat(userData.get("email")).isEqualTo("profile@example.com");
    }

    @Test
    @DisplayName("GET /me - WhenNotAuthenticated - Returns401")
    void getProfile_WhenNotAuthenticated_Returns401() throws Exception {
        // Arrange
        when(request.getPathInfo()).thenReturn("/me");
        when(session.getAttribute("userId")).thenReturn(null);

        // Act
        userServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("PUT /me - WhenNotAuthenticated - Returns401")
    void updateProfile_WhenNotAuthenticated_Returns401() throws Exception {
        // Arrange
        String requestBody = """
            {
                "username": "newusername",
                "email": "new@example.com"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/me");
        when(session.getAttribute("userId")).thenReturn(null);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        userServlet.doPut(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
