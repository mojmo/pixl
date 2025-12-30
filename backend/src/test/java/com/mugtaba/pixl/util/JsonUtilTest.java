package com.mugtaba.pixl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JsonUtil Unit Tests")
class JsonUtilTest {

    @Test
    @DisplayName("toJson_WithValidObject_ReturnsJsonString")
    void toJson_WithValidObject_ReturnsJsonString() throws JsonProcessingException {
        // Arrange
        Map<String, String> testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", "value2");

        // Act
        String json = JsonUtil.toJson(testMap);

        // Assert
        assertThat(json).isNotNull()
                .contains("key1")
                .contains("value1")
                .contains("key2")
                .contains("value2");
    }

    @Test
    @DisplayName("toJson_WithNullObject_ReturnsNull")
    void toJson_WithNullObject_ReturnsNull() throws JsonProcessingException {
        // Act
        String json = JsonUtil.toJson(null);

        // Assert
        assertThat(json).isNull();
    }

    @Test
    @DisplayName("toJson_WithEmptyMap_ReturnsEmptyJsonObject")
    void toJson_WithEmptyMap_ReturnsEmptyJsonObject() throws JsonProcessingException {
        // Arrange
        Map<String, String> emptyMap = new HashMap<>();

        // Act
        String json = JsonUtil.toJson(emptyMap);

        // Assert
        assertThat(json).isNotNull().isEqualTo("{}");
    }

    @Test
    @DisplayName("fromJson_WithValidJson_ReturnsObject")
    void fromJson_WithValidJson_ReturnsObject() throws JsonProcessingException {
        // Arrange
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        // Act
        @SuppressWarnings("unchecked")
        Map<String, String> result = JsonUtil.fromJson(json, Map.class);

        // Assert
        assertThat(result).isNotNull()
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("fromJson_WithNullOrEmptyJson_ReturnsNull")
    void fromJson_WithNullOrEmptyJson_ReturnsNull(String json) throws JsonProcessingException {
        // Act
        Map<?, ?> result = JsonUtil.fromJson(json, Map.class);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromJson_WithWhitespaceOnlyJson_ReturnsNull")
    void fromJson_WithWhitespaceOnlyJson_ReturnsNull() throws JsonProcessingException {
        // Arrange
        String json = "   ";

        // Act
        Map<?, ?> result = JsonUtil.fromJson(json, Map.class);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromJson_WithInvalidJson_ThrowsJsonProcessingException")
    void fromJson_WithInvalidJson_ThrowsJsonProcessingException() {
        // Arrange
        String invalidJson = "{invalid json}";

        // Act & Assert
        assertThatThrownBy(() -> JsonUtil.fromJson(invalidJson, Map.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("fromJsonReader_WithValidReader_ReturnsObject")
    void fromJsonReader_WithValidReader_ReturnsObject() throws IOException {
        // Arrange
        String jsonString = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        BufferedReader reader = new BufferedReader(new StringReader(jsonString));

        // Act
        @SuppressWarnings("unchecked")
        Map<String, String> result = JsonUtil.fromJson(reader, Map.class);

        // Assert
        assertThat(result).isNotNull()
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
    }

    @Test
    @DisplayName("fromJsonReader_WithNullReader_ReturnsNull")
    void fromJsonReader_WithNullReader_ReturnsNull() throws IOException {
        // Act
        Map<?, ?> result = JsonUtil.fromJson((BufferedReader) null, Map.class);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromJsonReader_WithEmptyReader_ReturnsNull")
    void fromJsonReader_WithEmptyReader_ReturnsNull() throws IOException {
        // Arrange
        BufferedReader reader = new BufferedReader(new StringReader(""));

        // Act
        Map<?, ?> result = JsonUtil.fromJson(reader, Map.class);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("writeJson_WithValidObject_WritesJsonToWriter")
    void writeJson_WithValidObject_WritesJsonToWriter() throws IOException {
        // Arrange
        Map<String, String> testMap = new HashMap<>();
        testMap.put("key1", "value1");
        StringWriter writer = new StringWriter();

        // Act
        JsonUtil.writeJson(writer, testMap);

        // Assert
        String result = writer.toString();
        assertThat(result).contains("key1").contains("value1");
    }

    @Test
    @DisplayName("writeJson_WithNullObject_WritesNullToWriter")
    void writeJson_WithNullObject_WritesNullToWriter() throws IOException {
        // Arrange
        StringWriter writer = new StringWriter();

        // Act
        JsonUtil.writeJson(writer, null);

        // Assert
        String result = writer.toString();
        assertThat(result).isEqualTo("null");
    }

    @Test
    @DisplayName("toJson_WithComplexObject_ReturnsValidJson")
    void toJson_WithComplexObject_ReturnsValidJson() throws JsonProcessingException {
        // Arrange
        TestObject testObject = new TestObject("testName", 123);

        // Act
        String json = JsonUtil.toJson(testObject);

        // Assert
        assertThat(json).isNotNull()
                .contains("testName")
                .contains("123");
    }

    @Test
    @DisplayName("fromJson_WithComplexJson_ReturnsObject")
    void fromJson_WithComplexJson_ReturnsObject() throws JsonProcessingException {
        // Arrange
        String json = "{\"name\":\"testName\",\"value\":123}";

        // Act
        TestObject result = JsonUtil.fromJson(json, TestObject.class);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("testName");
        assertThat(result.getValue()).isEqualTo(123);
    }

    @Test
    @DisplayName("toJson_FromJson_RoundTrip_PreservesData")
    void toJson_FromJson_RoundTrip_PreservesData() throws JsonProcessingException {
        // Arrange
        TestObject original = new TestObject("testName", 456);

        // Act
        String json = JsonUtil.toJson(original);
        TestObject result = JsonUtil.fromJson(json, TestObject.class);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getValue()).isEqualTo(original.getValue());
    }

    // Helper test class
    public static class TestObject {
        private String name;
        private int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
