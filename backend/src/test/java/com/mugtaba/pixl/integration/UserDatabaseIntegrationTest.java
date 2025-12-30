package com.mugtaba.pixl.integration;

import com.mugtaba.pixl.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Service Integration Tests")
class UserDatabaseIntegrationTest extends H2IntegrationTestBase {

    @Test
    @DisplayName("registerUser_WithValidData_CreatesUser")
    void registerUser_WithValidData_CreatesUser() throws SQLException {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String password = "Password123!";

        // Act
        User createdUser = userService.registerUser(username, email, password);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull().isPositive();
        assertThat(createdUser.getUsername()).isEqualTo(username);
        assertThat(createdUser.getEmail()).isEqualTo(email);
        assertThat(createdUser.getPasswordHash()).isNotNull();
        assertThat(createdUser.getSalt()).isNotNull();
        assertThat(createdUser.getCreatedAt()).isNotNull();
        assertThat(createdUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("registerUser_WithDuplicateUsername_ThrowsException")
    void registerUser_WithDuplicateUsername_ThrowsException() throws SQLException {
        // Arrange
        String username = "duplicate";
        String email1 = "first@example.com";
        String email2 = "second@example.com";
        String password = "Password123!";

        userService.registerUser(username, email1, password);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(username, email2, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    @DisplayName("registerUser_WithDuplicateEmail_ThrowsException")
    void registerUser_WithDuplicateEmail_ThrowsException() throws SQLException {
        // Arrange
        String username1 = "user1";
        String username2 = "user2";
        String email = "duplicate@example.com";
        String password = "Password123!";

        userService.registerUser(username1, email, password);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(username2, email, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("registerUser_WithShortPassword_ThrowsException")
    void registerUser_WithShortPassword_ThrowsException() {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String shortPassword = "Pass1!";

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(username, email, shortPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 8 characters long");
    }

    @Test
    @DisplayName("authenticateUser_WithCorrectCredentials_ReturnsUser")
    void authenticateUser_WithCorrectCredentials_ReturnsUser() throws SQLException {
        // Arrange
        String username = "authuser";
        String email = "auth@example.com";
        String password = "Password123!";
        
        userService.registerUser(username, email, password);

        // Act
        Optional<User> result = userService.authenticateUser(username, password);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
        assertThat(result.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("authenticateUser_WithWrongPassword_ReturnsEmpty")
    void authenticateUser_WithWrongPassword_ReturnsEmpty() throws SQLException {
        // Arrange
        String username = "authuser2";
        String email = "auth2@example.com";
        String correctPassword = "Password123!";
        String wrongPassword = "WrongPass456!";
        
        userService.registerUser(username, email, correctPassword);

        // Act
        Optional<User> result = userService.authenticateUser(username, wrongPassword);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("authenticateUser_WithEmail_ReturnsUser")
    void authenticateUser_WithEmail_ReturnsUser() throws SQLException {
        // Arrange
        String username = "authuser3";
        String email = "auth3@example.com";
        String password = "Password123!";
        
        userService.registerUser(username, email, password);

        // Act - Use email instead of username
        Optional<User> result = userService.authenticateUser(email, password);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
        assertThat(result.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("getUserById_WhenUserExists_ReturnsUser")
    void getUserById_WhenUserExists_ReturnsUser() throws SQLException {
        // Arrange
        String username = "findmebyid";
        String email = "findbyid@example.com";
        String password = "Password123!";
        
        User createdUser = userService.registerUser(username, email, password);

        // Act
        Optional<User> result = userService.getUserById(createdUser.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(createdUser.getId());
        assertThat(result.get().getUsername()).isEqualTo(username);
        assertThat(result.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("getUserById_WhenUserDoesNotExist_ReturnsEmpty")
    void getUserById_WhenUserDoesNotExist_ReturnsEmpty() throws SQLException {
        // Act
        Optional<User> result = userService.getUserById(999999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateUserProfile_WithValidData_UpdatesUser")
    void updateUserProfile_WithValidData_UpdatesUser() throws SQLException {
        // Arrange
        String originalUsername = "oldname";
        String originalEmail = "old@example.com";
        String password = "Password123!";
        
        User createdUser = userService.registerUser(originalUsername, originalEmail, password);
        
        String newUsername = "newname";
        String newEmail = "new@example.com";
        createdUser.setUsername(newUsername);
        createdUser.setEmail(newEmail);

        // Act
        boolean updated = userService.updateProfile(createdUser.getId(), newUsername, newEmail);

        // Assert
        assertThat(updated).isTrue();
        
        Optional<User> result = userService.getUserById(createdUser.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(newUsername);
        assertThat(result.get().getEmail()).isEqualTo(newEmail);
    }

    @Test
    @DisplayName("getUserByUsername_WhenUserExists_ReturnsUser")
    void getUserByUsername_WhenUserExists_ReturnsUser() throws SQLException {
        // Arrange
        String username = "findmebyusername";
        String email = "findbyusername@example.com";
        String password = "Password123!";
        
        userService.registerUser(username, email, password);

        // Act
        Optional<User> result = userService.getUserByUsername(username);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
        assertThat(result.get().getEmail()).isEqualTo(email);
    }
}
