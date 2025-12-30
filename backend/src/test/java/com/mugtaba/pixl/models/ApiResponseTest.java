package com.mugtaba.pixl.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ApiResponse Model Unit Tests")
class ApiResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("success_WithMessageAndData_CreatesSuccessResponse")
    void success_WithMessageAndData_CreatesSuccessResponse() {
        // Arrange
        String message = "Operation successful";
        String data = "test data";

        // Act
        ApiResponse<String> response = ApiResponse.success(message, data);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("success_WithMessageOnly_CreatesSuccessResponseWithNullData")
    void success_WithMessageOnly_CreatesSuccessResponseWithNullData() {
        // Arrange
        String message = "Operation successful";

        // Act
        ApiResponse<Object> response = ApiResponse.success(message);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error_WithMessage_CreatesErrorResponse")
    void error_WithMessage_CreatesErrorResponse() {
        // Arrange
        String message = "Operation failed";

        // Act
        ApiResponse<Object> response = ApiResponse.error(message);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo(message);
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("constructor_WithAllParameters_SetsAllFields")
    void constructor_WithAllParameters_SetsAllFields() {
        // Arrange
        boolean success = true;
        String message = "Test message";
        String data = "Test data";

        // Act
        ApiResponse<String> response = new ApiResponse<>(success, message, data);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("setSuccess_WithFalse_SetsSuccessToFalse")
    void setSuccess_WithFalse_SetsSuccessToFalse() {
        // Arrange
        ApiResponse<Object> response = new ApiResponse<>();

        // Act
        response.setSuccess(false);

        // Assert
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("setMessage_WithValidMessage_SetsMessage")
    void setMessage_WithValidMessage_SetsMessage() {
        // Arrange
        ApiResponse<Object> response = new ApiResponse<>();
        String message = "New message";

        // Act
        response.setMessage(message);

        // Assert
        assertThat(response.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("setData_WithValidData_SetsData")
    void setData_WithValidData_SetsData() {
        // Arrange
        ApiResponse<String> response = new ApiResponse<>();
        String data = "New data";

        // Act
        response.setData(data);

        // Assert
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("jsonSerialization_WithSuccessResponse_SerializesCorrectly")
    void jsonSerialization_WithSuccessResponse_SerializesCorrectly() throws Exception {
        // Arrange
        ApiResponse<String> response = ApiResponse.success("Success", "data");

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json).contains("\"success\":true")
                .contains("\"message\":\"Success\"")
                .contains("\"data\":\"data\"");
    }

    @Test
    @DisplayName("jsonSerialization_WithErrorResponse_SerializesCorrectly")
    void jsonSerialization_WithErrorResponse_SerializesCorrectly() throws Exception {
        // Arrange
        ApiResponse<Object> response = ApiResponse.error("Error occurred");

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json).contains("\"success\":false")
                .contains("\"error\":\"Error occurred\"");
    }

    @Test
    @DisplayName("jsonDeserialization_WithValidJson_DeserializesCorrectly")
    void jsonDeserialization_WithValidJson_DeserializesCorrectly() throws Exception {
        // Arrange
        String json = "{\"success\":true,\"message\":\"Test\",\"data\":\"value\"}";

        // Act
        @SuppressWarnings("unchecked")
        ApiResponse<String> response = objectMapper.readValue(json, ApiResponse.class);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Test");
    }

    @Test
    @DisplayName("success_WithNullMessage_CreatesResponseWithNullMessage")
    void success_WithNullMessage_CreatesResponseWithNullMessage() {
        // Act
        ApiResponse<Object> response = ApiResponse.success(null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    @DisplayName("error_WithNullMessage_CreatesResponseWithNullMessage")
    void error_WithNullMessage_CreatesResponseWithNullMessage() {
        // Act
        ApiResponse<Object> response = ApiResponse.error(null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isNull();
    }
}
