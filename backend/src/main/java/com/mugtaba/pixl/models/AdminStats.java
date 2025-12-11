package com.mugtaba.pixl.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model representing overall application statistics for admin dashboard.
 */
public class AdminStats {

    @JsonProperty("totalUsers")
    private long totalUsers;

    @JsonProperty("totalArtworks")
    private long totalArtworks;

    @JsonProperty("publicArtworks")
    private long publicArtworks;

    @JsonProperty("privateArtworks")
    private long privateArtworks;

    @JsonProperty("newUsersToday")
    private long newUsersToday;

    @JsonProperty("newUsersThisWeek")
    private long newUsersThisWeek;

    @JsonProperty("newUsersThisMonth")
    private long newUsersThisMonth;

    @JsonProperty("newArtworksToday")
    private long newArtworksToday;
    
    @JsonProperty("newArtworksThisWeek")
    private long newArtworksThisWeek;
    
    @JsonProperty("newArtworksThisMonth")
    private long newArtworksThisMonth;

    @JsonProperty("cacheStats")
    private Map<String, Object> cacheStats;

    @JsonProperty("generatedAt")
    private LocalDateTime generatedAt;

    public AdminStats() {
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalArtworks() { return totalArtworks; }
    public void setTotalArtworks(long totalArtworks) { this.totalArtworks = totalArtworks; }
    
    public long getPublicArtworks() { return publicArtworks; }
    public void setPublicArtworks(long publicArtworks) { this.publicArtworks = publicArtworks; }
    
    public long getPrivateArtworks() { return privateArtworks; }
    public void setPrivateArtworks(long privateArtworks) { this.privateArtworks = privateArtworks; }
    
    public long getNewUsersToday() { return newUsersToday; }
    public void setNewUsersToday(long newUsersToday) { this.newUsersToday = newUsersToday; }
    
    public long getNewUsersThisWeek() { return newUsersThisWeek; }
    public void setNewUsersThisWeek(long newUsersThisWeek) { this.newUsersThisWeek = newUsersThisWeek; }
    
    public long getNewUsersThisMonth() { return newUsersThisMonth; }
    public void setNewUsersThisMonth(long newUsersThisMonth) { this.newUsersThisMonth = newUsersThisMonth; }
    
    public long getNewArtworksToday() { return newArtworksToday; }
    public void setNewArtworksToday(long newArtworksToday) { this.newArtworksToday = newArtworksToday; }
    
    public long getNewArtworksThisWeek() { return newArtworksThisWeek; }
    public void setNewArtworksThisWeek(long newArtworksThisWeek) { this.newArtworksThisWeek = newArtworksThisWeek; }
    
    public long getNewArtworksThisMonth() { return newArtworksThisMonth; }
    public void setNewArtworksThisMonth(long newArtworksThisMonth) { this.newArtworksThisMonth = newArtworksThisMonth; }

    public Map<String, Object> getCacheStats() { return cacheStats; }
    public void setCacheStats(Map<String, Object> cacheStats) { this.cacheStats = cacheStats; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

}
