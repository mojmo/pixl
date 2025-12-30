package com.mugtaba.pixl.integration;

import com.mugtaba.pixl.models.ApiResponse;
import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.servlets.v1.ArtworkServlet;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ArtworkServlet Integration Tests")
class ArtworkServletIntegrationTest extends H2IntegrationTestBase {

    // Test wrapper to expose protected servlet methods
    private static class TestableArtworkServlet extends ArtworkServlet {
        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doGet(request, response);
        }
        
        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doPost(request, response);
        }
        
        @Override
        public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doPut(request, response);
        }
        
        @Override
        public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super.doDelete(request, response);
        }
    }

    private TestableArtworkServlet artworkServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;
    private PrintWriter printWriter;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        artworkServlet = new TestableArtworkServlet();
        artworkServlet.init();
        
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
        
        // Create test user
        testUser = userService.registerUser("artuser", "art@example.com", "Password123!");
    }

    @Test
    @DisplayName("POST /artworks - WithValidData - Returns201AndArtwork")
    void createArtwork_WithValidData_Returns201AndArtwork() throws Exception {
        // Arrange
        String requestBody = """
            {
                "title": "Test Artwork",
                "description": "Test Description",
                "pixelData": "#FF0000,#00FF00,#0000FF",
                "width": 3,
                "height": 1,
                "isPublic": true
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(session.getAttribute("userId")).thenReturn(testUser.getId());
        when(session.getAttribute("username")).thenReturn(testUser.getUsername());

        // Act
        artworkServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).contains("created successfully");
        
        Map<String, Object> artworkData = (Map<String, Object>) apiResponse.getData();
        assertThat(artworkData.get("title")).isEqualTo("Test Artwork");
        assertThat(artworkData.get("shareableLink")).isNotNull();
    }

    @Test
    @DisplayName("POST /artworks - WhenNotAuthenticated - Returns401")
    void createArtwork_WhenNotAuthenticated_Returns401() throws Exception {
        // Arrange
        String requestBody = """
            {
                "title": "Test",
                "pixelData": "#000000",
                "width": 1,
                "height": 1
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(session.getAttribute("userId")).thenReturn(null);

        // Act
        artworkServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("POST /artworks - WithInvalidData - Returns400")
    void createArtwork_WithInvalidData_Returns400() throws Exception {
        // Arrange - Missing required fields
        String requestBody = """
            {
                "title": "",
                "pixelData": ""
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(session.getAttribute("userId")).thenReturn(testUser.getId());

        // Act
        artworkServlet.doPost(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /artworks/{id} - WhenExists - Returns200AndArtwork")
    void getArtworkById_WhenExists_Returns200AndArtwork() throws Exception {
        // Arrange
        Artwork artwork = createTestArtwork("Find Me", true);
        
        when(request.getPathInfo()).thenReturn("/" + artwork.getId());
        when(session.getAttribute("userId")).thenReturn(testUser.getId());

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        Map<String, Object> artworkData = (Map<String, Object>) apiResponse.getData();
        assertThat(artworkData.get("title")).isEqualTo("Find Me");
    }

    @Test
    @DisplayName("GET /artworks/{id} - WhenNotExists - Returns404")
    void getArtworkById_WhenNotExists_Returns404() throws Exception {
        // Arrange
        when(request.getPathInfo()).thenReturn("/999999");

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("GET /artworks/{id} - WithInvalidId - Returns400")
    void getArtworkById_WithInvalidId_Returns400() throws Exception {
        // Arrange
        when(request.getPathInfo()).thenReturn("/invalid");

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /artworks?type=public - ReturnsPublicArtworks")
    void getArtworks_ReturnsPublicArtworks() throws Exception {
        // Arrange
        createTestArtwork("Public 1", true);
        createTestArtwork("Public 2", true);
        createTestArtwork("Private 1", false);
        
        when(request.getPathInfo()).thenReturn("/");
        when(request.getParameter("type")).thenReturn("public");
        when(request.getParameter("page")).thenReturn("1");
        when(request.getParameter("limit")).thenReturn("10");

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) apiResponse.getData();
        Map<String, Object> pagination = (Map<String, Object>) data.get("pagination");
        List<Map<String, Object>> artworks = (List<Map<String, Object>>) data.get("artworks");
        assertThat(artworks).hasSize(2);
        assertThat(pagination.get("totalCount")).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /artworks/share/{link} - WhenExists - Returns200")
    void getArtworkByLink_WhenExists_Returns200() throws Exception {
        // Arrange
        Artwork artwork = createTestArtwork("Shared Art", true);
        
        when(request.getPathInfo()).thenReturn("/share/" + artwork.getShareableLink());

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        Map<String, Object> artworkData = (Map<String, Object>) apiResponse.getData();
        assertThat(artworkData.get("title")).isEqualTo("Shared Art");
    }

    @Test
    @DisplayName("GET /artworks/user/{userId} - ReturnsUserArtworks")
    void getUserArtworks_ReturnsUserArtworks() throws Exception {
        // Arrange
        createTestArtwork("Art 1", true);
        createTestArtwork("Art 2", false);
        
        when(request.getPathInfo()).thenReturn("/user/" + testUser.getId());
        when(session.getAttribute("userId")).thenReturn(testUser.getId());

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        List<Map<String, Object>> artworks = (List<Map<String, Object>>) apiResponse.getData();
        assertThat(artworks).hasSize(2);
    }

    @Test
    @DisplayName("PUT /artworks/{id} - WithValidData - Returns200")
    void updateArtwork_WithValidData_Returns200() throws Exception {
        // Arrange
        Artwork artwork = createTestArtwork("Old Title", true);
        
        String requestBody = """
            {
                "title": "New Title",
                "description": "New Description",
                "pixelData": "#FFFFFF",
                "width": 1,
                "height": 1,
                "isPublic": false
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/" + artwork.getId());
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(session.getAttribute("userId")).thenReturn(testUser.getId());

        // Act
        artworkServlet.doPut(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).contains("updated successfully");
    }

    @Test
    @DisplayName("PUT /artworks/{id} - WhenNotOwner - Returns403")
    void updateArtwork_WhenNotOwner_Returns403() throws Exception {
        // Arrange
        Artwork artwork = createTestArtwork("Test", true);
        User otherUser = userService.registerUser("other", "other@example.com", "Password123!");
        
        String requestBody = """
            {
                "title": "Hacked",
                "pixelData": "#000000",
                "width": 1,
                "height": 1
            }
            """;
        
        when(request.getPathInfo()).thenReturn("/" + artwork.getId());
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(session.getAttribute("userId")).thenReturn(otherUser.getId());

        // Act
        artworkServlet.doPut(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("DELETE /artworks/{id} - WhenOwner - Returns200")
    void deleteArtwork_WhenOwner_Returns200() throws Exception {
        // Arrange
        Artwork artwork = createTestArtwork("To Delete", true);
        
        when(request.getPathInfo()).thenReturn("/" + artwork.getId());
        when(session.getAttribute("userId")).thenReturn(testUser.getId());

        // Act
        artworkServlet.doDelete(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).contains("deleted successfully");
    }

    @Test
    @DisplayName("DELETE /artworks/{id} - WhenNotOwner - Returns403")
    void deleteArtwork_WhenNotOwner_Returns403() throws Exception {
        // Arrange
        Artwork artwork = createTestArtwork("Protected", true);
        User otherUser = userService.registerUser("hacker", "hacker@example.com", "Password123!");
        
        when(request.getPathInfo()).thenReturn("/" + artwork.getId());
        when(session.getAttribute("userId")).thenReturn(otherUser.getId());

        // Act
        artworkServlet.doDelete(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("DELETE /artworks/{id} - WhenNotExists - Returns404")
    void deleteArtwork_WhenNotExists_Returns404() throws Exception {
        // Arrange
        when(request.getPathInfo()).thenReturn("/999999");
        when(session.getAttribute("userId")).thenReturn(testUser.getId());

        // Act
        artworkServlet.doDelete(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("GET /artworks - WithPagination - ReturnsCorrectPage")
    void getArtworks_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange - Create 5 public artworks
        for (int i = 1; i <= 5; i++) {
            createTestArtwork("Art " + i, true);
        }
        
        when(request.getPathInfo()).thenReturn("/");
        when(request.getParameter("type")).thenReturn("public");
        when(request.getParameter("page")).thenReturn("2");
        when(request.getParameter("limit")).thenReturn("2");

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_OK);
        
        String responseBody = responseWriter.toString();
        ApiResponse apiResponse = JsonUtil.fromJson(responseBody, ApiResponse.class);
        
        Map<String, Object> data = (Map<String, Object>) apiResponse.getData();
        Map<String, Object> pagination = (Map<String, Object>) data.get("pagination");
        List<Map<String, Object>> artworks = (List<Map<String, Object>>) data.get("artworks");
        assertThat(artworks).hasSize(2);
        assertThat(pagination.get("totalCount")).isEqualTo(5);
        assertThat(pagination.get("currentPage")).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /artworks - WithInvalidPagination - Returns400")
    void getArtworks_WithInvalidPagination_Returns400() throws Exception {
        // Arrange
        when(request.getPathInfo()).thenReturn("/");
        when(request.getParameter("page")).thenReturn("-1");
        when(request.getParameter("limit")).thenReturn("10");

        // Act
        artworkServlet.doGet(request, response);
        printWriter.flush();

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("Response ContentType - IsJSON")
    void responseContentType_IsJSON() throws Exception {
        // Arrange
        createTestArtwork("Test", true);
        when(request.getPathInfo()).thenReturn("/");

        // Act
        artworkServlet.doGet(request, response);

        // Assert
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    // Helper method
    private Artwork createTestArtwork(String title, boolean isPublic) throws Exception {
        Artwork artwork = new Artwork();
        artwork.setTitle(title);
        artwork.setDescription("Test");
        artwork.setUserId(testUser.getId());
        artwork.setPixelData("#000000");
        artwork.setWidth(1);
        artwork.setHeight(1);
        artwork.setPublic(isPublic);
        
        return artworkService.createArtwork(artwork);
    }
}
