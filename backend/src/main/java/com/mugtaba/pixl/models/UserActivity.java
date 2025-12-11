package com.mugtaba.pixl.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model representing user activity for admin monitoring.
 */
public class UserActivity {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("artworkCount")
    private Long artworkCount;

    @JsonProperty("publicArtworkCount")
    private Long publicArtworkCount;

    @JsonProperty("isAdmin")
    private boolean isAdmin;

    // Getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Long getArtworkCount() { return artworkCount; }
    public void setArtworkCount(Long artworkCount) { this.artworkCount = artworkCount; }
    
    public Long getPublicArtworkCount() { return publicArtworkCount; }
    public void setPublicArtworkCount(Long publicArtworkCount) { this.publicArtworkCount = publicArtworkCount; }
    
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}
