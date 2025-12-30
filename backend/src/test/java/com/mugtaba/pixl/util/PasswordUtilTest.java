package com.mugtaba.pixl.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PasswordUtil Unit Tests")
class PasswordUtilTest {

    @Test
    @DisplayName("generateSalt_WhenCalled_ReturnsNonNullSalt")
    void generateSalt_WhenCalled_ReturnsNonNullSalt() {
        // Arrange & Act
        String salt = PasswordUtil.generateSalt();

        // Assert
        assertThat(salt).isNotNull();
    }

    @Test
    @DisplayName("generateSalt_WhenCalled_ReturnsNonEmptySalt")
    void generateSalt_WhenCalled_ReturnsNonEmptySalt() {
        // Arrange & Act
        String salt = PasswordUtil.generateSalt();

        // Assert
        assertThat(salt).isNotEmpty();
    }

    @Test
    @DisplayName("generateSalt_WhenCalledMultipleTimes_ReturnsUniqueSalts")
    void generateSalt_WhenCalledMultipleTimes_ReturnsUniqueSalts() {
        // Arrange & Act
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();

        // Assert
        assertThat(salt1).isNotEqualTo(salt2);
    }

    @Test
    @DisplayName("hashPassword_WithValidInputs_ReturnsNonNullHash")
    void hashPassword_WithValidInputs_ReturnsNonNullHash() {
        // Arrange
        String password = "mySecurePassword123";
        String salt = PasswordUtil.generateSalt();

        // Act
        String hash = PasswordUtil.hashPassword(password, salt);

        // Assert
        assertThat(hash).isNotNull();
    }

    @Test
    @DisplayName("hashPassword_WithValidInputs_ReturnsNonEmptyHash")
    void hashPassword_WithValidInputs_ReturnsNonEmptyHash() {
        // Arrange
        String password = "mySecurePassword123";
        String salt = PasswordUtil.generateSalt();

        // Act
        String hash = PasswordUtil.hashPassword(password, salt);

        // Assert
        assertThat(hash).isNotEmpty();
    }

    @Test
    @DisplayName("hashPassword_WithNullPassword_ThrowsIllegalArgumentException")
    void hashPassword_WithNullPassword_ThrowsIllegalArgumentException() {
        // Arrange
        String salt = PasswordUtil.generateSalt();

        // Act & Assert
        assertThatThrownBy(() -> PasswordUtil.hashPassword(null, salt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password and salt cannot be null");
    }

    @Test
    @DisplayName("hashPassword_WithNullSalt_ThrowsIllegalArgumentException")
    void hashPassword_WithNullSalt_ThrowsIllegalArgumentException() {
        // Arrange
        String password = "mySecurePassword123";

        // Act & Assert
        assertThatThrownBy(() -> PasswordUtil.hashPassword(password, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password and salt cannot be null");
    }

    @Test
    @DisplayName("hashPassword_WithSameInputs_ReturnsSameHash")
    void hashPassword_WithSameInputs_ReturnsSameHash() {
        // Arrange
        String password = "mySecurePassword123";
        String salt = PasswordUtil.generateSalt();

        // Act
        String hash1 = PasswordUtil.hashPassword(password, salt);
        String hash2 = PasswordUtil.hashPassword(password, salt);

        // Assert
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("hashPassword_WithDifferentSalts_ReturnsDifferentHashes")
    void hashPassword_WithDifferentSalts_ReturnsDifferentHashes() {
        // Arrange
        String password = "mySecurePassword123";
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();

        // Act
        String hash1 = PasswordUtil.hashPassword(password, salt1);
        String hash2 = PasswordUtil.hashPassword(password, salt2);

        // Assert
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("hashPassword_WithDifferentPasswords_ReturnsDifferentHashes")
    void hashPassword_WithDifferentPasswords_ReturnsDifferentHashes() {
        // Arrange
        String password1 = "mySecurePassword123";
        String password2 = "differentPassword456";
        String salt = PasswordUtil.generateSalt();

        // Act
        String hash1 = PasswordUtil.hashPassword(password1, salt);
        String hash2 = PasswordUtil.hashPassword(password2, salt);

        // Assert
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("verifyPassword_WithCorrectPassword_ReturnsTrue")
    void verifyPassword_WithCorrectPassword_ReturnsTrue() {
        // Arrange
        String password = "mySecurePassword123";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        // Act
        boolean result = PasswordUtil.verifyPassword(password, hash, salt);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("verifyPassword_WithIncorrectPassword_ReturnsFalse")
    void verifyPassword_WithIncorrectPassword_ReturnsFalse() {
        // Arrange
        String correctPassword = "mySecurePassword123";
        String incorrectPassword = "wrongPassword456";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(correctPassword, salt);

        // Act
        boolean result = PasswordUtil.verifyPassword(incorrectPassword, hash, salt);

        // Assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("verifyPassword_WithNullOrEmptyPassword_ReturnsFalse")
    void verifyPassword_WithNullOrEmptyPassword_ReturnsFalse(String password) {
        // Arrange
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("validPassword", salt);

        // Act
        boolean result = PasswordUtil.verifyPassword(password, hash, salt);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("verifyPassword_WithNullHash_ReturnsFalse")
    void verifyPassword_WithNullHash_ReturnsFalse() {
        // Arrange
        String password = "mySecurePassword123";
        String salt = PasswordUtil.generateSalt();

        // Act
        boolean result = PasswordUtil.verifyPassword(password, null, salt);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("verifyPassword_WithNullSalt_ReturnsFalse")
    void verifyPassword_WithNullSalt_ReturnsFalse() {
        // Arrange
        String password = "mySecurePassword123";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        // Act
        boolean result = PasswordUtil.verifyPassword(password, hash, null);

        // Assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a", "12345678", "Password!@#$%^&*()", "VeryLongPasswordWith123Numbers!@#"})
    @DisplayName("hashPassword_WithVariousPasswords_ReturnsValidHash")
    void hashPassword_WithVariousPasswords_ReturnsValidHash(String password) {
        // Arrange
        String salt = PasswordUtil.generateSalt();

        // Act
        String hash = PasswordUtil.hashPassword(password, salt);

        // Assert
        assertThat(hash).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("hashPassword_WithEmptyPassword_ReturnsValidHash")
    void hashPassword_WithEmptyPassword_ReturnsValidHash() {
        // Arrange
        String password = "";
        String salt = PasswordUtil.generateSalt();

        // Act
        String hash = PasswordUtil.hashPassword(password, salt);

        // Assert
        assertThat(hash).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("verifyPassword_WithWrongSalt_ReturnsFalse")
    void verifyPassword_WithWrongSalt_ReturnsFalse() {
        // Arrange
        String password = "mySecurePassword123";
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt1);

        // Act
        boolean result = PasswordUtil.verifyPassword(password, hash, salt2);

        // Assert
        assertThat(result).isFalse();
    }
}
