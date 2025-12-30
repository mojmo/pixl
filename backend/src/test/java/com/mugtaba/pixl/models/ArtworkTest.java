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

@DisplayName("Artwork Model Unit Tests")
class ArtworkTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("constructor_WithDefaultConstructor_CreatesEmptyArtwork")
    void constructor_WithDefaultConstructor_CreatesEmptyArtwork() {
        // Act
        Artwork artwork = new Artwork();

        // Assert
        assertThat(artwork).isNotNull();
        assertThat(artwork.getId()).isNull();
        assertThat(artwork.getTitle()).isNull();
    }

    @Test
    @DisplayName("constructor_WithAllParameters_SetsAllFields")
    void constructor_WithAllParameters_SetsAllFields() {
        // Arrange & Act
        Artwork artwork = new Artwork("Test Title", "Test Description", 1L, "pixelData", 100, 200, true);

        // Assert
        assertThat(artwork.getTitle()).isEqualTo("Test Title");
        assertThat(artwork.getDescription()).isEqualTo("Test Description");
        assertThat(artwork.getUserId()).isEqualTo(1L);
        assertThat(artwork.getPixelData()).isEqualTo("pixelData");
        assertThat(artwork.getWidth()).isEqualTo(100);
        assertThat(artwork.getHeight()).isEqualTo(200);
        assertThat(artwork.isPublic()).isTrue();
        assertThat(artwork.getCreatedAt()).isNotNull();
        assertThat(artwork.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("setId_WithValidId_SetsId")
    void setId_WithValidId_SetsId() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setId(123L);

        // Assert
        assertThat(artwork.getId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("setTitle_WithValidTitle_SetsTitle")
    void setTitle_WithValidTitle_SetsTitle() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setTitle("New Title");

        // Assert
        assertThat(artwork.getTitle()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("setDescription_WithValidDescription_SetsDescription")
    void setDescription_WithValidDescription_SetsDescription() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setDescription("New Description");

        // Assert
        assertThat(artwork.getDescription()).isEqualTo("New Description");
    }

    @Test
    @DisplayName("setUserId_WithValidUserId_SetsUserId")
    void setUserId_WithValidUserId_SetsUserId() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setUserId(456L);

        // Assert
        assertThat(artwork.getUserId()).isEqualTo(456L);
    }

    @Test
    @DisplayName("setUsername_WithValidUsername_SetsUsername")
    void setUsername_WithValidUsername_SetsUsername() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setUsername("testuser");

        // Assert
        assertThat(artwork.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("setPixelData_WithValidPixelData_SetsPixelData")
    void setPixelData_WithValidPixelData_SetsPixelData() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setPixelData("newPixelData");

        // Assert
        assertThat(artwork.getPixelData()).isEqualTo("newPixelData");
    }

    @Test
    @DisplayName("setWidth_WithValidWidth_SetsWidth")
    void setWidth_WithValidWidth_SetsWidth() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setWidth(500);

        // Assert
        assertThat(artwork.getWidth()).isEqualTo(500);
    }

    @Test
    @DisplayName("setHeight_WithValidHeight_SetsHeight")
    void setHeight_WithValidHeight_SetsHeight() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setHeight(600);

        // Assert
        assertThat(artwork.getHeight()).isEqualTo(600);
    }

    @Test
    @DisplayName("setPublic_WithTrue_SetsPublicToTrue")
    void setPublic_WithTrue_SetsPublicToTrue() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setPublic(true);

        // Assert
        assertThat(artwork.isPublic()).isTrue();
    }

    @Test
    @DisplayName("setPublic_WithFalse_SetsPublicToFalse")
    void setPublic_WithFalse_SetsPublicToFalse() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setPublic(false);

        // Assert
        assertThat(artwork.isPublic()).isFalse();
    }

    @Test
    @DisplayName("setCreatedAt_WithValidDateTime_SetsCreatedAt")
    void setCreatedAt_WithValidDateTime_SetsCreatedAt() {
        // Arrange
        Artwork artwork = new Artwork();
        LocalDateTime now = LocalDateTime.now();

        // Act
        artwork.setCreatedAt(now);

        // Assert
        assertThat(artwork.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("setUpdatedAt_WithValidDateTime_SetsUpdatedAt")
    void setUpdatedAt_WithValidDateTime_SetsUpdatedAt() {
        // Arrange
        Artwork artwork = new Artwork();
        LocalDateTime now = LocalDateTime.now();

        // Act
        artwork.setUpdatedAt(now);

        // Assert
        assertThat(artwork.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("setShareableLink_WithValidLink_SetsShareableLink")
    void setShareableLink_WithValidLink_SetsShareableLink() {
        // Arrange
        Artwork artwork = new Artwork();

        // Act
        artwork.setShareableLink("abc123");

        // Assert
        assertThat(artwork.getShareableLink()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("validatedData_WithNullTitle_ReturnsTrue")
    void validatedData_WithNullTitle_ReturnsTrue() {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setPixelData("pixelData");
        artwork.setWidth(100);
        artwork.setHeight(200);
        artwork.setUserId(1L);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("validatedData_WithInvalidTitle_ReturnsTrue")
    void validatedData_WithInvalidTitle_ReturnsTrue(String title) {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setTitle(title);
        artwork.setPixelData("pixelData");
        artwork.setWidth(100);
        artwork.setHeight(200);
        artwork.setUserId(1L);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("validatedData_WithInvalidPixelData_ReturnsTrue")
    void validatedData_WithInvalidPixelData_ReturnsTrue(String pixelData) {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setTitle("Valid Title");
        artwork.setPixelData(pixelData);
        artwork.setWidth(100);
        artwork.setHeight(200);
        artwork.setUserId(1L);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedData_WithZeroWidth_ReturnsTrue")
    void validatedData_WithZeroWidth_ReturnsTrue() {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setTitle("Valid Title");
        artwork.setPixelData("pixelData");
        artwork.setWidth(0);
        artwork.setHeight(200);
        artwork.setUserId(1L);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedData_WithNegativeWidth_ReturnsTrue")
    void validatedData_WithNegativeWidth_ReturnsTrue() {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setTitle("Valid Title");
        artwork.setPixelData("pixelData");
        artwork.setWidth(-1);
        artwork.setHeight(200);
        artwork.setUserId(1L);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedData_WithZeroHeight_ReturnsTrue")
    void validatedData_WithZeroHeight_ReturnsTrue() {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setTitle("Valid Title");
        artwork.setPixelData("pixelData");
        artwork.setWidth(100);
        artwork.setHeight(0);
        artwork.setUserId(1L);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedData_WithNullUserId_ReturnsTrue")
    void validatedData_WithNullUserId_ReturnsTrue() {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setTitle("Valid Title");
        artwork.setPixelData("pixelData");
        artwork.setWidth(100);
        artwork.setHeight(200);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatedData_WithValidData_ReturnsFalse")
    void validatedData_WithValidData_ReturnsFalse() {
        // Arrange
        Artwork artwork = new Artwork("Valid Title", "Description", 1L, "pixelData", 100, 200, true);

        // Act
        boolean result = artwork.isNotValidatedData();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("equals_WithSameIdAndDifferentUsername_ReturnsFalse")
    void equals_WithSameId_ReturnsTrue() {
        // Arrange
        Artwork artwork1 = new Artwork();
        artwork1.setId(1L);
        Artwork artwork2 = new Artwork();
        artwork2.setId(1L);

        // Act & Assert
        assertThat(artwork1).isEqualTo(artwork2);
    }

    @Test
    @DisplayName("equals_WithDifferentIds_ReturnsFalse")
    void equals_WithDifferentIds_ReturnsFalse() {
        // Arrange
        Artwork artwork1 = new Artwork();
        artwork1.setId(1L);
        Artwork artwork2 = new Artwork();
        artwork2.setId(2L);

        // Act & Assert
        assertThat(artwork1).isNotEqualTo(artwork2);
    }

    @Test
    @DisplayName("equals_WithSameInstance_ReturnsTrue")
    void equals_WithSameInstance_ReturnsTrue() {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setId(1L);

        // Act & Assert
        assertThat(artwork).isEqualTo(artwork);
    }

    @Test
    @DisplayName("equals_WithNull_ReturnsFalse")
    void equals_WithNull_ReturnsFalse() {
        // Arrange
        Artwork artwork = new Artwork();
        artwork.setId(1L);

        // Act & Assert
        assertThat(artwork).isNotEqualTo(null);
    }

    @Test
    @DisplayName("hashCode_WithSameId_ReturnsSameHashCode")
    void hashCode_WithSameId_ReturnsSameHashCode() {
        // Arrange
        Artwork artwork1 = new Artwork();
        artwork1.setId(1L);
        Artwork artwork2 = new Artwork();
        artwork2.setId(1L);

        // Act & Assert
        assertThat(artwork1.hashCode()).isEqualTo(artwork2.hashCode());
    }

    @Test
    @DisplayName("jsonSerialization_IncludesAllPublicFields")
    void jsonSerialization_IncludesAllPublicFields() throws JsonProcessingException {
        // Arrange
        Artwork artwork = new Artwork("Test Title", "Test Description", 1L, "pixelData", 100, 200, true);
        artwork.setId(1L);
        artwork.setUsername("testuser");
        artwork.setShareableLink("abc123");

        // Act
        String json = objectMapper.writeValueAsString(artwork);

        // Assert
        assertThat(json)
                .contains("Test Title")
                .contains("Test Description")
                .contains("pixelData")
                .contains("testuser")
                .contains("abc123");
    }
}
