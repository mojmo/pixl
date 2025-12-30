package com.mugtaba.pixl.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Model Unit Tests")
class UserTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("constructor_WithDefaultConstructor_CreatesEmptyUser")
    void constructor_WithDefaultConstructor_CreatesEmptyUser() {
        // Act
        User user = new User();

        // Assert
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isNull();
    }

    @Test
    @DisplayName("constructor_WithUsernameAndEmail_SetsFields")
    void constructor_WithUsernameAndEmail_SetsFields() {
        // Arrange & Act
        User user = new User("testuser", "test@example.com");

        // Assert
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("constructor_WithAllFields_SetsAllFields")
    void constructor_WithAllFields_SetsAllFields() {
        // Arrange & Act
        User user = new User("testuser", "test@example.com", "hashedPassword", "salt123");

        // Assert
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashedPassword");
        assertThat(user.getSalt()).isEqualTo("salt123");
    }

    @Test
    @DisplayName("setId_WithValidId_SetsId")
    void setId_WithValidId_SetsId() {
        // Arrange
        User user = new User();

        // Act
        user.setId(123L);

        // Assert
        assertThat(user.getId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("setUsername_WithValidUsername_SetsUsername")
    void setUsername_WithValidUsername_SetsUsername() {
        // Arrange
        User user = new User();

        // Act
        user.setUsername("newusername");

        // Assert
        assertThat(user.getUsername()).isEqualTo("newusername");
    }

    @Test
    @DisplayName("setEmail_WithValidEmail_SetsEmail")
    void setEmail_WithValidEmail_SetsEmail() {
        // Arrange
        User user = new User();

        // Act
        user.setEmail("new@example.com");

        // Assert
        assertThat(user.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("setPasswordHash_WithValidHash_SetsPasswordHash")
    void setPasswordHash_WithValidHash_SetsPasswordHash() {
        // Arrange
        User user = new User();

        // Act
        user.setPasswordHash("newHash123");

        // Assert
        assertThat(user.getPasswordHash()).isEqualTo("newHash123");
    }

    @Test
    @DisplayName("setSalt_WithValidSalt_SetsSalt")
    void setSalt_WithValidSalt_SetsSalt() {
        // Arrange
        User user = new User();

        // Act
        user.setSalt("newSalt456");

        // Assert
        assertThat(user.getSalt()).isEqualTo("newSalt456");
    }

    @Test
    @DisplayName("setCreatedAt_WithValidDateTime_SetsCreatedAt")
    void setCreatedAt_WithValidDateTime_SetsCreatedAt() {
        // Arrange
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        // Act
        user.setCreatedAt(now);

        // Assert
        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("setUpdatedAt_WithValidDateTime_SetsUpdatedAt")
    void setUpdatedAt_WithValidDateTime_SetsUpdatedAt() {
        // Arrange
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        // Act
        user.setUpdatedAt(now);

        // Assert
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("setAdmin_WithTrue_SetsAdminToTrue")
    void setAdmin_WithTrue_SetsAdminToTrue() {
        // Arrange
        User user = new User();

        // Act
        user.setAdmin(true);

        // Assert
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("setAdmin_WithFalse_SetsAdminToFalse")
    void setAdmin_WithFalse_SetsAdminToFalse() {
        // Arrange
        User user = new User();

        // Act
        user.setAdmin(false);

        // Assert
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("equals_WithSameIdAndSameUsernameAndSameEmail_ReturnsTrue")
    void equals_WithSameIdAndSameUsernameAndSameEmail_ReturnsTrue() {
        // Arrange
        User user1 = new User("user1", "user1@example.com");
        user1.setId(1L);
        User user2 = new User("user1", "user1@example.com");
        user2.setId(1L);

        // Act & Assert
        assertThat(user1).isEqualTo(user2);
    }

    @Test
    @DisplayName("equals_WithSameIdAndDifferentUsername_ReturnsFalse")
    void equals_WithSameIdAndDifferentUsername_ReturnsFalse() {
        // Arrange
        User user1 = new User("user1", "user1@example.com");
        user1.setId(1L);
        User user2 = new User("user2", "user2@example.com");
        user2.setId(1L);

        // Act & Assert
        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("equals_WithDifferentIds_ReturnsFalse")
    void equals_WithDifferentIds_ReturnsFalse() {
        // Arrange
        User user1 = new User("user1", "user1@example.com");
        user1.setId(1L);
        User user2 = new User("user2", "user2@example.com");
        user2.setId(2L);

        // Act & Assert
        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("equals_WithSameInstance_ReturnsTrue")
    void equals_WithSameInstance_ReturnsTrue() {
        // Arrange
        User user = new User("user1", "user1@example.com");
        user.setId(1L);

        // Act & Assert
        assertThat(user).isEqualTo(user);
    }

    @Test
    @DisplayName("equals_WithNull_ReturnsFalse")
    void equals_WithNull_ReturnsFalse() {
        // Arrange
        User user = new User("user1", "user1@example.com");
        user.setId(1L);

        // Act & Assert
        assertThat(user).isNotEqualTo(null);
    }

    @Test
    @DisplayName("equals_WithDifferentClass_ReturnsFalse")
    void equals_WithDifferentClass_ReturnsFalse() {
        // Arrange
        User user = new User("user1", "user1@example.com");
        user.setId(1L);
        String differentObject = "Not a User";

        // Act & Assert
        assertThat(user).isNotEqualTo(differentObject);
    }

    @Test
    @DisplayName("hashCode_WithSameIdButDifferentUsernameAndEmail_ReturnsDifferentHashCode")
    void hashCode_WithSameIdButDifferentUsernameAndEmail_ReturnsDifferentHashCode() {
        // Arrange
        User user1 = new User("user1", "user1@example.com");
        user1.setId(1L);
        User user2 = new User("user2", "user2@example.com");
        user2.setId(1L);

        // Act & Assert
        assertThat(user1.hashCode()).isNotEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("hashCode_WithSameIdAndUsernameAndEmail_ReturnsSameHashCode")
    void hashCode_WithSameIdAndUsernameAndEmail_ReturnsSameHashCode() {
        // Arrange
        User user1 = new User("user1", "user1@example.com");
        user1.setId(1L);
        User user2 = new User("user1", "user1@example.com");
        user2.setId(1L);

        // Act & Assert
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("jsonSerialization_ExcludesPasswordHash")
    void jsonSerialization_ExcludesPasswordHash() throws JsonProcessingException {
        // Arrange
        User user = new User("testuser", "test@example.com", "hashedPassword", "salt123");
        user.setId(1L);

        // Act
        String json = objectMapper.writeValueAsString(user);

        // Assert
        assertThat(json).doesNotContain("hashedPassword")
                .doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("jsonSerialization_ExcludesSalt")
    void jsonSerialization_ExcludesSalt() throws JsonProcessingException {
        // Arrange
        User user = new User("testuser", "test@example.com", "hashedPassword", "salt123");
        user.setId(1L);

        // Act
        String json = objectMapper.writeValueAsString(user);

        // Assert
        assertThat(json).doesNotContain("salt123")
                .doesNotContain("salt");
    }

    @Test
    @DisplayName("jsonSerialization_IncludesUsername")
    void jsonSerialization_IncludesUsername() throws JsonProcessingException {
        // Arrange
        User user = new User("testuser", "test@example.com");
        user.setId(1L);

        // Act
        String json = objectMapper.writeValueAsString(user);

        // Assert
        assertThat(json).contains("testuser");
    }

    @Test
    @DisplayName("jsonSerialization_IncludesEmail")
    void jsonSerialization_IncludesEmail() throws JsonProcessingException {
        // Arrange
        User user = new User("testuser", "test@example.com");
        user.setId(1L);

        // Act
        String json = objectMapper.writeValueAsString(user);

        // Assert
        assertThat(json).contains("test@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("validatedForRegistration_WithInvalidUsername_ReturnsTrue")
    void validatedForRegistration_WithInvalidUsername_ReturnsTrue(String username) {
        // Arrange
        User user = new User(username, "test@example.com");

        // Act
        boolean result = user.validatedForRegistration();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedForRegistration_WithShortUsername_ReturnsTrue")
    void validatedForRegistration_WithShortUsername_ReturnsTrue() {
        // Arrange
        User user = new User("ab", "test@example.com");

        // Act
        boolean result = user.validatedForRegistration();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedForRegistration_WithLongUsername_ReturnsTrue")
    void validatedForRegistration_WithLongUsername_ReturnsTrue() {
        // Arrange
        String longUsername = "a".repeat(51);
        User user = new User(longUsername, "test@example.com");

        // Act
        boolean result = user.validatedForRegistration();

        // Assert
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("validatedForRegistration_WithNullOrEmptyEmail_ReturnsTrue")
    void validatedForRegistration_WithNullOrEmptyEmail_ReturnsTrue(String email) {
        // Arrange
        User user = new User("validuser", email);

        // Act
        boolean result = user.validatedForRegistration();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedForRegistration_WithInvalidUsernameCharacters_ReturnsTrue")
    void validatedForRegistration_WithInvalidUsernameCharacters_ReturnsTrue() {
        // Arrange
        User user = new User("user@name", "test@example.com");

        // Act
        boolean result = user.validatedForRegistration();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("setId_WithNull_SetsIdToNull")
    void setId_WithNull_SetsIdToNull() {
        // Arrange
        User user = new User();
        user.setId(123L);

        // Act
        user.setId(null);

        // Assert
        assertThat(user.getId()).isNull();
    }
}
