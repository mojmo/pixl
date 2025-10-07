package com.mugtaba.pixl.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an artwork created by a user.
 */
public class Artwork {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("pixelData")
    private String pixelData; // JSON string of pixel data

    @JsonProperty("width")
    private int width;

    @JsonProperty("height")
    private int height;

    @JsonProperty("isPublic")
    private boolean isPublic;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonProperty("shareableLink")
    private String shareableLink;

    /** Default constructor */
    public Artwork() {}

    /**
     * Constructor to create a new Artwork instance.
     * @param title The title of the artwork.
     * @param description The description of the artwork.
     * @param userId The ID of the user who created the artwork.
     * @param pixelData The pixel data of the artwork.
     * @param width The width of the artwork.
     * @param height The height of the artwork.
     * @param isPublic Whether the artwork is public or not.
     */
    public Artwork(String title, String description, Long userId, String pixelData, int width, int height, boolean isPublic) {
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.pixelData = pixelData;
        this.width = width;
        this.height = height;
        this.isPublic = isPublic;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPixelData() { return pixelData; }
    public void setPixelData(String pixelData) { this.pixelData = pixelData; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    @JsonIgnore
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getShareableLink() { return shareableLink; }
    public void setShareableLink(String shareableLink) { this.shareableLink = shareableLink; }

    /**
     * Validates the artwork data to ensure all required fields are present and valid.
     * @return true if the artwork data is valid, false otherwise.
     */
    @JsonIgnore
    public boolean validateData() {
        return title != null && !title.trim().isEmpty()
            && pixelData != null && !pixelData.trim().isEmpty()
            && width > 0 && height > 0 && userId != null;
    }

    /**
     * Equality based on the unique identifier of the artwork.
     * @param o the object to compare with.
     * @return true if both objects represent the same artwork, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artwork artwork = (Artwork) o;
        return Objects.equals(id, artwork.id);
    }

    /**
     * Hash code based on the unique identifier of the artwork.
     * @return hash code of the artwork.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
