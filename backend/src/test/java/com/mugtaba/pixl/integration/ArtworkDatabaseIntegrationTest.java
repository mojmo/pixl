package com.mugtaba.pixl.integration;

import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Artwork Service Integration Tests")
class ArtworkDatabaseIntegrationTest extends H2IntegrationTestBase {

    @Test
    @DisplayName("createArtwork_WithValidData_CreatesArtwork")
    void createArtwork_WithValidData_CreatesArtwork() throws SQLException {
        // Arrange
        User user = userService.registerUser("artuser", "art@example.com", "Password123!");
        
        Artwork artwork = new Artwork();
        artwork.setTitle("Test Artwork");
        artwork.setDescription("Test Description");
        artwork.setUserId(user.getId());
        artwork.setPixelData("#FF0000,#00FF00,#0000FF");
        artwork.setWidth(3);
        artwork.setHeight(1);
        artwork.setPublic(true);

        // Act
        Artwork createdArtwork = artworkService.createArtwork(artwork);

        // Assert
        assertThat(createdArtwork).isNotNull();
        assertThat(createdArtwork.getId()).isNotNull().isPositive();
        assertThat(createdArtwork.getTitle()).isEqualTo("Test Artwork");
        assertThat(createdArtwork.getDescription()).isEqualTo("Test Description");
        assertThat(createdArtwork.getUserId()).isEqualTo(user.getId());
        assertThat(createdArtwork.getShareableLink()).isNotNull();
        assertThat(createdArtwork.getCreatedAt()).isNotNull();
        assertThat(createdArtwork.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getArtworkById_WhenArtworkExists_ReturnsArtwork")
    void getArtworkById_WhenArtworkExists_ReturnsArtwork() throws SQLException {
        // Arrange
        User user = userService.registerUser("artuser2", "art2@example.com", "Password123!");
        
        Artwork artwork = new Artwork();
        artwork.setTitle("Find Me");
        artwork.setDescription("Test Description");
        artwork.setUserId(user.getId());
        artwork.setPixelData("#000000");
        artwork.setWidth(1);
        artwork.setHeight(1);
        artwork.setPublic(true);
        
        Artwork createdArtwork = artworkService.createArtwork(artwork);

        // Act
        Optional<Artwork> result = artworkService.getArtworkById(createdArtwork.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(createdArtwork.getId());
        assertThat(result.get().getTitle()).isEqualTo("Find Me");
        assertThat(result.get().getUserId()).isEqualTo(user.getId());
        assertThat(result.get().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    @DisplayName("getArtworkById_WhenArtworkDoesNotExist_ReturnsEmpty")
    void getArtworkById_WhenArtworkDoesNotExist_ReturnsEmpty() throws SQLException {
        // Act
        Optional<Artwork> result = artworkService.getArtworkById(999999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateArtwork_WithValidData_UpdatesArtwork")
    void updateArtwork_WithValidData_UpdatesArtwork() throws SQLException {
        // Arrange
        User user = userService.registerUser("artuser3", "art3@example.com", "Password123!");
        
        Artwork artwork = new Artwork();
        artwork.setTitle("Old Title");
        artwork.setDescription("Old Description");
        artwork.setUserId(user.getId());
        artwork.setPixelData("#000000");
        artwork.setWidth(1);
        artwork.setHeight(1);
        artwork.setPublic(false);
        
        Artwork createdArtwork = artworkService.createArtwork(artwork);
        
        // Update artwork properties
        createdArtwork.setTitle("New Title");
        createdArtwork.setDescription("New Description");
        createdArtwork.setPublic(true);

        // Act
        boolean updated = artworkService.updateArtwork(createdArtwork);

        // Assert
        assertThat(updated).isTrue();
        
        Optional<Artwork> result = artworkService.getArtworkById(createdArtwork.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("New Title");
        assertThat(result.get().getDescription()).isEqualTo("New Description");
        assertThat(result.get().isPublic()).isTrue();
    }

    @Test
    @DisplayName("deleteArtwork_WithValidId_DeletesArtwork")
    void deleteArtwork_WithValidId_DeletesArtwork() throws SQLException {
        // Arrange
        User user = userService.registerUser("artuser4", "art4@example.com", "Password123!");
        
        Artwork artwork = new Artwork();
        artwork.setTitle("Delete Me");
        artwork.setDescription("Test");
        artwork.setUserId(user.getId());
        artwork.setPixelData("#000000");
        artwork.setWidth(1);
        artwork.setHeight(1);
        artwork.setPublic(false);
        
        Artwork createdArtwork = artworkService.createArtwork(artwork);

        // Act
        boolean deleted = artworkService.deleteArtwork(createdArtwork.getId(), user.getId());

        // Assert
        assertThat(deleted).isTrue();
        
        Optional<Artwork> result = artworkService.getArtworkById(createdArtwork.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserArtworks_ReturnsAllUserArtworks")
    void getUserArtworks_ReturnsAllUserArtworks() throws SQLException {
        // Arrange
        User user = userService.registerUser("artuser5", "art5@example.com", "Password123!");
        
        createTestArtwork(user.getId(), "Art 1", false);
        createTestArtwork(user.getId(), "Art 2", true);
        createTestArtwork(user.getId(), "Art 3", false);

        // Act
        List<Artwork> artworks = artworkService.getArtworksByUser(user.getId());

        // Assert
        assertThat(artworks).hasSize(3);
        assertThat(artworks).extracting(Artwork::getTitle)
                .containsExactlyInAnyOrder("Art 1", "Art 2", "Art 3");
    }

    @Test
    @DisplayName("getPublicArtworks_ReturnsOnlyPublicArtworks")
    void getPublicArtworks_ReturnsOnlyPublicArtworks() throws SQLException {
        // Arrange
        User user1 = userService.registerUser("artuser6", "art6@example.com", "Password123!");
        User user2 = userService.registerUser("artuser7", "art7@example.com", "Password123!");
        
        createTestArtwork(user1.getId(), "Public 1", true);
        createTestArtwork(user1.getId(), "Private 1", false);
        createTestArtwork(user2.getId(), "Public 2", true);
        createTestArtwork(user2.getId(), "Private 2", false);

        // Act
        List<Artwork> publicArtworks = artworkService.getPublicArtwork(10, 0);

        // Assert
        assertThat(publicArtworks).hasSize(2);
        assertThat(publicArtworks).extracting(Artwork::getTitle)
                .containsExactlyInAnyOrder("Public 1", "Public 2");
        assertThat(publicArtworks).allMatch(Artwork::isPublic);
    }

    @Test
    @DisplayName("getArtworkByShareableLink_ReturnsCorrectArtwork")
    void getArtworkByShareableLink_ReturnsCorrectArtwork() throws SQLException {
        // Arrange
        User user = userService.registerUser("artuser8", "art8@example.com", "Password123!");
        
        Artwork artwork = new Artwork();
        artwork.setTitle("Shared Artwork");
        artwork.setDescription("Test");
        artwork.setUserId(user.getId());
        artwork.setPixelData("#000000");
        artwork.setWidth(1);
        artwork.setHeight(1);
        artwork.setPublic(true);
        
        Artwork createdArtwork = artworkService.createArtwork(artwork);

        // Act
        Optional<Artwork> result = artworkService.getArtworkByLink(createdArtwork.getShareableLink());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(createdArtwork.getId());
        assertThat(result.get().getTitle()).isEqualTo("Shared Artwork");
    }

    @Test
    @DisplayName("getPublicArtworksCount_ReturnsCorrectCount")
    void getPublicArtworksCount_ReturnsCorrectCount() throws SQLException {
        // Arrange
        User user = userService.registerUser("artuser9", "art9@example.com", "Password123!");
        
        createTestArtwork(user.getId(), "Public 1", true);
        createTestArtwork(user.getId(), "Public 2", true);
        createTestArtwork(user.getId(), "Private 1", false);

        // Act
        int count = artworkService.getPublicArtworkCount();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteArtwork_WhenWrongUserId_DoesNotDelete")
    void deleteArtwork_WhenWrongUserId_DoesNotDelete() throws SQLException {
        // Arrange
        User owner = userService.registerUser("owner", "owner@example.com", "Password123!");
        User otherUser = userService.registerUser("other", "other@example.com", "Password123!");
        
        Artwork artwork = new Artwork();
        artwork.setTitle("Protected Artwork");
        artwork.setDescription("Test");
        artwork.setUserId(owner.getId());
        artwork.setPixelData("#000000");
        artwork.setWidth(1);
        artwork.setHeight(1);
        artwork.setPublic(false);
        
        Artwork createdArtwork = artworkService.createArtwork(artwork);

        // Act - Try to delete with wrong user ID
        boolean deleted = artworkService.deleteArtwork(createdArtwork.getId(), otherUser.getId());

        // Assert
        assertThat(deleted).isFalse();
        
        Optional<Artwork> result = artworkService.getArtworkById(createdArtwork.getId());
        assertThat(result).isPresent();
    }

    // Helper method
    private Artwork createTestArtwork(Long userId, String title, boolean isPublic) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setTitle(title);
        artwork.setDescription("Test");
        artwork.setUserId(userId);
        artwork.setPixelData("#000000");
        artwork.setWidth(1);
        artwork.setHeight(1);
        artwork.setPublic(isPublic);
        
        return artworkService.createArtwork(artwork);
    }
}
