package com.mugtaba.pixl.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Custom Exception Unit Tests")
class ExceptionsTest {

    @Test
    @DisplayName("ValidationException_WithMessage_CreatesException")
    void validationException_WithMessage_CreatesException() {
        // Arrange
        String message = "Validation failed";

        // Act
        ValidationException exception = new ValidationException(message);

        // Assert
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains(message);
    }

    @Test
    @DisplayName("UnauthorizedException_WithMessage_CreatesException")
    void unauthorizedException_WithMessage_CreatesException() {
        // Arrange
        String message = "Unauthorized access";

        // Act
        UnauthorizedException exception = new UnauthorizedException(message);

        // Assert
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains(message);
    }

    @Test
    @DisplayName("ForbiddenException_WithMessage_CreatesException")
    void forbiddenException_WithMessage_CreatesException() {
        // Arrange
        String message = "Access forbidden";

        // Act
        ForbiddenException exception = new ForbiddenException(message);

        // Assert
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains(message);
    }

    @Test
    @DisplayName("ResourceNotFoundException_WithResourceName_CreatesException")
    void resourceNotFoundException_WithResourceName_CreatesException() {
        // Arrange
        String resourceName = "User";

        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceName);

        // Assert
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains(resourceName);
    }

    @Test
    @DisplayName("DatabaseException_WithMessageAndCause_CreatesException")
    void databaseException_WithMessageAndCause_CreatesException() {
        // Arrange
        String message = "Database error";
        Throwable cause = new Exception("Root cause");

        // Act
        DatabaseException exception = new DatabaseException(message, cause);

        // Assert
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains(message);
    }

    @Test
    @DisplayName("PixlException_IsBaseException_ForAllCustomExceptions")
    void pixlException_IsBaseException_ForAllCustomExceptions() {
        // Assert
        assertThat(new ValidationException("test")).isInstanceOf(PixlException.class);
        assertThat(new UnauthorizedException("test")).isInstanceOf(PixlException.class);
        assertThat(new ForbiddenException("test")).isInstanceOf(PixlException.class);
        assertThat(new ResourceNotFoundException("test")).isInstanceOf(PixlException.class);
        assertThat(new DatabaseException("test", new RuntimeException("Root cause"))).isInstanceOf(PixlException.class);
    }
}
