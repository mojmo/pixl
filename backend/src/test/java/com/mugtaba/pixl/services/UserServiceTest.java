package com.mugtaba.pixl.services;

import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.util.PasswordUtil;
import com.mugtaba.pixl.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService Unit Tests")
class UserServiceTest {

    // Note: UserService tests require database and email configuration
    // See UserDatabaseIntegrationTest for full integration testing with H2 database

    @Disabled
    @Test
    @DisplayName("registerUser_WithValidData_ReturnsUser")
    void registerUser_WithValidData_ReturnsUser() throws Exception {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String password = "Password123!";
        String salt = "testSalt123";
        String hashedPassword = "hashedPassword123";
        
        // for now, this test is disabled - see integration tests for full database testing
        assertThat(true).isTrue(); // placeholder assertion
    }

    @Test
    @DisplayName("passwordUtil_GenerateSalt_ReturnsNonNullSalt")
    void passwordUtil_GenerateSalt_ReturnsNonNullSalt() {
        // Arrange & Act
        String salt = PasswordUtil.generateSalt();

        // Assert
        assertThat(salt).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("passwordUtil_HashPassword_WithSameSaltAndPassword_ReturnsSameHash")
    void passwordUtil_HashPassword_WithSameSaltAndPassword_ReturnsSameHash() {
        // Arrange
        String password = "Password123!";
        String salt = PasswordUtil.generateSalt();

        // Act
        String hash1 = PasswordUtil.hashPassword(password, salt);
        String hash2 = PasswordUtil.hashPassword(password, salt);

        // Assert
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("passwordUtil_VerifyPassword_WithCorrectPassword_ReturnsTrue")
    void passwordUtil_VerifyPassword_WithCorrectPassword_ReturnsTrue() {
        // Arrange
        String password = "Password123!";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        // Act
        boolean result = PasswordUtil.verifyPassword(password, hash, salt);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("passwordUtil_VerifyPassword_WithWrongPassword_ReturnsFalse")
    void passwordUtil_VerifyPassword_WithWrongPassword_ReturnsFalse() {
        // Arrange
        String correctPassword = "Password123!";
        String wrongPassword = "WrongPass456!";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(correctPassword, salt);

        // Act
        boolean result = PasswordUtil.verifyPassword(wrongPassword, hash, salt);

        // Assert
        assertThat(result).isFalse();
    }
}