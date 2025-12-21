package com.mugtaba.pixl.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    private String salt;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonIgnore
    private Boolean isAdmin;

    /** Default constructor */
    public User() {}

    /** Basic constructor without sensitive fields
     * 
     * @param username The username
     * @param email The email
     */
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    /** Full constructor with all fields
     * 
     * @param username The username
     * @param email The email
     * @param passwordHash The hashed password
     * @param salt The salt used for hashing
     */
    public User(String username, String email, String passwordHash, String salt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @JsonGetter("isAdmin")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)  // Only include if true
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    /**
     * Validates user data for registration.
     * - Username: 3-50 chars, alphanumeric + underscores
     * - Email: valid format, max 100 chars
     * 
     * @return true if valid data, false otherwise
     */
    @JsonIgnore
    public boolean validateForRegistration() {
        return username != null && username.trim().length() >= 3 && username.trim().length() <= 50 &&
                email != null && hasValidEmail(email) &&
                username.matches("^[a-zA-Z0-9_]+$"); // Only alphanumeric and underscore
    }

    /**
     * Validates user data for login.
     * - Either username or email must be provided
     * - If email is provided, it must be valid format
     * 
     * @return true if valid data, false otherwise
     */
    @JsonIgnore
    public boolean validateForLogin() {
        return (username != null && !username.trim().isEmpty()) ||
                (email != null && hasValidEmail(email));
    }

    /**
     * Validates email format.
     * - Must be a valid email format
     * - Max 100 chars
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    @JsonIgnore
    public boolean hasValidEmail(String email) {
        return email != null &&
                email.matches("^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$") &&
                email.length() <= 100;
    }

    /**
     * Creates a safe copy of the User object excluding sensitive fields.
     * 
     * @return A new User object with only public fields populated
     */
    public User toPublicUser() {
        User publicUser = new User();
        publicUser.setId(this.id);
        publicUser.setUsername(this.username);
        publicUser.setEmail(this.email);
        publicUser.setCreatedAt(this.createdAt);
        publicUser.setUpdatedAt(this.updatedAt);
        publicUser.setAdmin(this.isAdmin);

        return publicUser;
    }

    /**
     * Overrides equals to compare User objects based on their unique ID, username, and email.
     * 
     * @param o The object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username) &&
                Objects.equals(email, user.email);
    }

    /**
     * Overrides hashCode to generate a hash code based on the unique ID, username, and email.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }

    /**
     * Provides a string representation of the User object, excluding sensitive fields.
     * 
     * @return A string representation of the User object
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isAdmin=" + isAdmin +
                '}';
    }
}