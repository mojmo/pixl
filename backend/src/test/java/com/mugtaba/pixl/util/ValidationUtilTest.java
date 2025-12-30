package com.mugtaba.pixl.util;

import com.mugtaba.pixl.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidationUtil Unit Tests")
class ValidationUtilTest {

    @Test
    @DisplayName("validatePagination_WithValidValues_DoesNotThrowException")
    void validatePagination_WithValidValues_DoesNotThrowException() {
        // Arrange
        int page = 1;
        int limit = 10;

        // Act & Assert
        assertThatCode(() -> ValidationUtil.validatePagination(page, limit))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validatePagination_WithPageLessThanOne_ThrowsValidationException")
    void validatePagination_WithPageLessThanOne_ThrowsValidationException() {
        // Arrange
        int page = 0;
        int limit = 10;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validatePagination(page, limit))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Page number must be 1 or greater");
    }

    @Test
    @DisplayName("validatePagination_WithLimitLessThanOne_ThrowsValidationException")
    void validatePagination_WithLimitLessThanOne_ThrowsValidationException() {
        // Arrange
        int page = 1;
        int limit = 0;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validatePagination(page, limit))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Limit must be 1 or greater");
    }

    @Test
    @DisplayName("validatePagination_WithLimitGreaterThan100_ThrowsValidationException")
    void validatePagination_WithLimitGreaterThan100_ThrowsValidationException() {
        // Arrange
        int page = 1;
        int limit = 101;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validatePagination(page, limit))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Limit cannot exceed 100 items per page");
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,50", "1,100", "10,100", "100,1"})
    @DisplayName("validatePagination_WithBoundaryValues_DoesNotThrowException")
    void validatePagination_WithBoundaryValues_DoesNotThrowException(int page, int limit) {
        // Act & Assert
        assertThatCode(() -> ValidationUtil.validatePagination(page, limit))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateStringNotEmpty_WithValidString_DoesNotThrowException")
    void validateStringNotEmpty_WithValidString_DoesNotThrowException() {
        // Arrange
        String value = "validString";
        String fieldName = "testField";

        // Act & Assert
        assertThatCode(() -> ValidationUtil.validateStringNotEmpty(value, fieldName))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("validateStringNotEmpty_WithNullOrEmptyString_ThrowsValidationException")
    void validateStringNotEmpty_WithNullOrEmptyString_ThrowsValidationException(String value) {
        // Arrange
        String fieldName = "testField";

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validateStringNotEmpty(value, fieldName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("testField is required and cannot be empty");
    }

    @Test
    @DisplayName("validateStringLength_WithValidLength_DoesNotThrowException")
    void validateStringLength_WithValidLength_DoesNotThrowException() {
        // Arrange
        String value = "validString";
        String fieldName = "testField";
        int minLength = 5;
        int maxLength = 20;

        // Act & Assert
        assertThatCode(() -> ValidationUtil.validateStringLength(value, fieldName, minLength, maxLength))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateStringLength_WithNullValue_ThrowsValidationException")
    void validateStringLength_WithNullValue_ThrowsValidationException() {
        // Arrange
        String fieldName = "testField";
        int minLength = 5;
        int maxLength = 20;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validateStringLength(null, fieldName, minLength, maxLength))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("testField is required");
    }

    @Test
    @DisplayName("validateStringLength_WithTooShortString_ThrowsValidationException")
    void validateStringLength_WithTooShortString_ThrowsValidationException() {
        // Arrange
        String value = "abc";
        String fieldName = "testField";
        int minLength = 5;
        int maxLength = 20;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validateStringLength(value, fieldName, minLength, maxLength))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("testField must be at least 5 characters long");
    }

    @Test
    @DisplayName("validateStringLength_WithTooLongString_ThrowsValidationException")
    void validateStringLength_WithTooLongString_ThrowsValidationException() {
        // Arrange
        String value = "ThisIsAVeryLongStringThatExceedsTheMaximumLength";
        String fieldName = "testField";
        int minLength = 5;
        int maxLength = 20;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validateStringLength(value, fieldName, minLength, maxLength))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("testField cannot exceed 20 characters");
    }

    @Test
    @DisplayName("validateRange_WithValidValue_DoesNotThrowException")
    void validateRange_WithValidValue_DoesNotThrowException() {
        // Arrange
        int value = 50;
        String fieldName = "testField";
        int min = 1;
        int max = 100;

        // Act & Assert
        assertThatCode(() -> ValidationUtil.validateRange(value, fieldName, min, max))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateRange_WithValueBelowMin_ThrowsValidationException")
    void validateRange_WithValueBelowMin_ThrowsValidationException() {
        // Arrange
        int value = 0;
        String fieldName = "testField";
        int min = 1;
        int max = 100;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validateRange(value, fieldName, min, max))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("testField must be between 1and 100");
    }

    @Test
    @DisplayName("validateRange_WithValueAboveMax_ThrowsValidationException")
    void validateRange_WithValueAboveMax_ThrowsValidationException() {
        // Arrange
        int value = 101;
        String fieldName = "testField";
        int min = 1;
        int max = 100;

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.validateRange(value, fieldName, min, max))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("testField must be between 1and 100");
    }

    @ParameterizedTest
    @CsvSource({"1,1,100", "100,1,100", "50,1,100"})
    @DisplayName("validateRange_WithBoundaryValues_DoesNotThrowException")
    void validateRange_WithBoundaryValues_DoesNotThrowException(int value, int min, int max) {
        // Arrange
        String fieldName = "testField";

        // Act & Assert
        assertThatCode(() -> ValidationUtil.validateRange(value, fieldName, min, max))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("parsePageParameter_WithNullValue_ReturnsDefaultPageOne")
    void parsePageParameter_WithNullValue_ReturnsDefaultPageOne() throws ValidationException {
        // Act
        int result = ValidationUtil.parsePageParameter(null);

        // Assert
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("parsePageParameter_WithEmptyString_ReturnsDefaultPageOne")
    void parsePageParameter_WithEmptyString_ReturnsDefaultPageOne() throws ValidationException {
        // Act
        int result = ValidationUtil.parsePageParameter("");

        // Assert
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("parsePageParameter_WithValidPageNumber_ReturnsPageNumber")
    void parsePageParameter_WithValidPageNumber_ReturnsPageNumber() throws ValidationException {
        // Act
        int result = ValidationUtil.parsePageParameter("5");

        // Assert
        assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("parsePageParameter_WithPageLessThanOne_ThrowsValidationException")
    void parsePageParameter_WithPageLessThanOne_ThrowsValidationException() {
        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.parsePageParameter("0"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Page number must be 1 or greater");
    }

    @Test
    @DisplayName("parsePageParameter_WithInvalidFormat_ThrowsValidationException")
    void parsePageParameter_WithInvalidFormat_ThrowsValidationException() {
        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.parsePageParameter("abc"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid page number format");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "10", "100", "999"})
    @DisplayName("parsePageParameter_WithValidValues_ReturnsCorrectPage")
    void parsePageParameter_WithValidValues_ReturnsCorrectPage(String pageParam) throws ValidationException {
        // Act
        int result = ValidationUtil.parsePageParameter(pageParam);

        // Assert
        assertThat(result).isEqualTo(Integer.parseInt(pageParam));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-10", "-999"})
    @DisplayName("parsePageParameter_WithNegativeValues_ThrowsValidationException")
    void parsePageParameter_WithNegativeValues_ThrowsValidationException(String pageParam) {
        // Act & Assert
        assertThatThrownBy(() -> ValidationUtil.parsePageParameter(pageParam))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Page number must be 1 or greater");
    }
}
