package com.mugtaba.pixl.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorsFilter Unit Tests")
class CorsFilterTest {

    private CorsFilter corsFilter;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @BeforeEach
    void setUp() {
        corsFilter = new CorsFilter();
    }

    @Test
    @DisplayName("doFilter_WithAllowedOrigin_SetsCorsHeaders")
    void doFilter_WithAllowedOrigin_SetsCorsHeaders() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn("http://localhost:5173");
        when(mockRequest.getMethod()).thenReturn("GET");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        verify(mockResponse).setHeader("Access-Control-Allow-Credentials", "true");
        verify(mockResponse).setHeader(eq("Access-Control-Allow-Methods"), anyString());
        verify(mockResponse).setHeader(eq("Access-Control-Allow-Headers"), anyString());
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:3000"})
    @DisplayName("doFilter_WithAllowedOrigins_SetsCorsHeaders")
    void doFilter_WithAllowedOrigins_SetsCorsHeaders(String origin) throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn(origin);
        when(mockRequest.getMethod()).thenReturn("GET");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setHeader("Access-Control-Allow-Origin", origin);
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("doFilter_WithDisallowedOrigin_DoesNotSetCorsHeaders")
    void doFilter_WithDisallowedOrigin_DoesNotSetCorsHeaders() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn("http://evil.com");
        when(mockRequest.getMethod()).thenReturn("GET");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse, never()).setHeader(eq("Access-Control-Allow-Origin"), anyString());
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("doFilter_WithNullOrigin_DoesNotSetCorsHeaders")
    void doFilter_WithNullOrigin_DoesNotSetCorsHeaders() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn(null);
        when(mockRequest.getMethod()).thenReturn("GET");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse, never()).setHeader(eq("Access-Control-Allow-Origin"), anyString());
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("doFilter_WithOptionsMethod_ReturnsOkWithoutChainingFilter")
    void doFilter_WithOptionsMethod_ReturnsOkWithoutChainingFilter() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn("http://localhost:5173");
        when(mockRequest.getMethod()).thenReturn("OPTIONS");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
        verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("doFilter_WithOptionsMethodAndNoOrigin_ReturnsOk")
    void doFilter_WithOptionsMethodAndNoOrigin_ReturnsOk() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn(null);
        when(mockRequest.getMethod()).thenReturn("OPTIONS");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
        verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("doFilter_WithGetMethod_ChainsFilter")
    void doFilter_WithGetMethod_ChainsFilter() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn("http://localhost:5173");
        when(mockRequest.getMethod()).thenReturn("GET");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("doFilter_WithPostMethod_ChainsFilter")
    void doFilter_WithPostMethod_ChainsFilter() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn("http://localhost:5173");
        when(mockRequest.getMethod()).thenReturn("POST");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("doFilter_WithAllowedOrigin_SetsMaxAge")
    void doFilter_WithAllowedOrigin_SetsMaxAge() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getHeader("Origin")).thenReturn("http://localhost:5173");
        when(mockRequest.getMethod()).thenReturn("GET");

        // Act
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setHeader("Access-Control-Max-Age", "3600");
    }
}
