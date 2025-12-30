package com.mugtaba.pixl.integration;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.servlets.v1.AuthServlet;
import com.mugtaba.pixl.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthServlet Integration Tests")
class AuthServletIntegrationTest extends H2IntegrationTestBase {

    // Test wrapper to expose protected servlet methods
    private static class TestableAuthServlet extends AuthServlet {
        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doPost(request, response);
        }
        
        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doGet(request, response);
        }
    }

    private TestableAuthServlet authServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        authServlet = new TestableAuthServlet();
        authServlet.init();
        
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
    @DisplayName("POST /register - WithValidData - Returns201AndUser")
    void register_WithValidData_Returns201AndUser() throws Exception {
        // Arrange
        String requestBody = """
            {
                "username": "newuser",
                "email": "newuser@example.com",
                "password": "Password123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/register");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).contains("Account created successfully");
        
        // v1 servlet returns user data directly, not wrapped in "user" key
        Map<String, Object> userData = (Map<String, Object>) apiResponse.getData();
        assertThat(userData.get("username")).isEqualTo("newuser");
        assertThat(userData.get("email")).isEqualTo("newuser@example.com");
    }

    @Test
    @DisplayName("POST /register - WithDuplicateUsername - Returns400")
    void register_WithDuplicateUsername_Returns400() throws Exception {
        // Arrange - Register first user
        userService.registerUser("existinguser", "existing@example.com", "Password123!");
        
        String requestBody = """
            {
                "username": "existinguser",
                "email": "newemail@example.com",
                "password": "Password123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/register");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isFalse();
        assertThat(apiResponse.getError()).contains("Username already exists");
    }

    @Test
    @DisplayName("POST /register - WithInvalidEmail - Returns400")
    void register_WithInvalidEmail_Returns400() throws Exception {
        // Arrange
        String requestBody = """
            {
                "username": "testuser",
                "email": "invalid-email",
                "password": "Password123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/register");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isFalse();
        assertThat(apiResponse.getError()).isNotNull();
    }

    @Test
    @DisplayName("POST /register - WithWeakPassword - Returns400")
    void register_WithWeakPassword_Returns400() throws Exception {
        // Arrange
        String requestBody = """
            {
                "username": "testuser",
                "email": "test@example.com",
                "password": "weak"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/register");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isFalse();
        assertThat(apiResponse.getError()).isNotNull();
    }

    @Test
    @DisplayName("POST /login - WithValidCredentials - Returns200AndSession")
    void login_WithValidCredentials_Returns200AndSession() throws Exception {
        // Arrange - Register user first
        userService.registerUser("loginuser", "login@example.com", "Password123!");
        
        String requestBody = """
            {
                "username": "loginuser",
                "password": "Password123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/login");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(session).setAttribute(eq("userId"), any(Long.class));
        verify(session).setAttribute(eq("username"), eq("loginuser"));
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).contains("Logged in successfully");
    }

    @Test
    @DisplayName("POST /login - WithEmail - Returns200")
    void login_WithEmail_Returns200() throws Exception {
        // Arrange
        userService.registerUser("emailuser", "emailuser@example.com", "Password123!");
        
        String requestBody = """
            {
                "username": "emailuser@example.com",
                "password": "Password123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/login");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(session).setAttribute(eq("userId"), any(Long.class));
    }

    @Test
    @DisplayName("POST /login - WithWrongPassword - Returns401")
    void login_WithWrongPassword_Returns401() throws Exception {
        // Arrange
        userService.registerUser("testuser", "test@example.com", "Password123!");
        
        String requestBody = """
            {
                "username": "testuser",
                "password": "WrongPassword123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/login");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isFalse();
        assertThat(apiResponse.getError()).contains("Invalid");
    }

    @Test
    @DisplayName("POST /login - WithNonexistentUser - Returns401")
    void login_WithNonexistentUser_Returns401() throws Exception {
        // Arrange
        String requestBody = """
            {
                "username": "nonexistent",
                "password": "Password123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/login");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /session - WhenAuthenticated - Returns200")
    void checkSession_WhenAuthenticated_Returns200() throws Exception {
        // Arrange
        User user = userService.registerUser("sessionuser", "session@example.com", "Password123!");
        
        when(request.getPathInfo()).thenReturn("/session");
        when(session.getAttribute("userId")).thenReturn(user.getId());
        when(session.getAttribute("username")).thenReturn("sessionuser");

        // Act
        authServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).contains("Session is active");
    }

    @Test
    @DisplayName("POST /logout - WhenAuthenticated - Returns200AndInvalidatesSession")
    void logout_WhenAuthenticated_Returns200AndInvalidatesSession() throws Exception {
        // Arrange
        when(request.getPathInfo()).thenReturn("/logout");
        when(session.getAttribute("userId")).thenReturn(1L);

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(session).invalidate();
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).contains("Logged out successfully");
    }

    @Test
    @DisplayName("POST /invalid - Returns404")
    void invalidEndpoint_Returns404() throws Exception {
        // Arrange
        when(request.getPathInfo()).thenReturn("/invalid");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("POST /register - WithMalformedJSON - Returns400")
    void register_WithMalformedJSON_Returns400() throws Exception {
        // Arrange
        String malformedJson = "{ invalid json }";
        
        when(request.getPathInfo()).thenReturn("/register");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(malformedJson)));

        // Act
        authServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("Response Headers - AreSetCorrectly")
    void responseHeaders_AreSetCorrectly() throws Exception {
        // Arrange
        String requestBody = """
            {
                "username": "headertest",
                "email": "headertest@example.com",
                "password": "Password123!"
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/register");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Act
        authServlet.doPost(request, response);

        // Assert
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }
}
