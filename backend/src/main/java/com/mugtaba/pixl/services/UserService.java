package com.mugtaba.pixl.services;

import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.util.DatabaseUtil;
import com.mugtaba.pixl.util.PasswordUtil;
import java.sql.*;

/**
 * Service class for user-related operations including registration and authentication.
 * Handles database interactions for user management with secure password handling.
 */
public class UserService {

    /**
     * Registers a new user with the system.
     * Generates a salt, hashes the password, and stores user credentials in the database.
     *
     * @param username the username for the new account
     * @param email the email address for the new account
     * @param password the plain text password to be hashed and stored
     * @return true if registration was successful, false otherwise
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public boolean registerUser(String username, String email, String password) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);

        String sql = "INSERT INTO users (username, email, password_hash, salt) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, salt);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            // Check if it's a duplicate entry error
            if (e.getMessage().contains("duplicate") || e.getSQLState().equals("23505")) {
                throw new RuntimeException("Username or email already exists", e);
            }
            return false;
        }
    }

    /**
     * Authenticates a user by verifying provided credentials against stored values.
     * Retrieves user data from database, verifies password, and returns user object if successful.
     *
     * @param username the username to authenticate
     * @param password the plain text password to verify
     * @return User object if authentication is successful, null otherwise
     * @throws IllegalArgumentException if username or password is null or empty
     */
    public User loginUser(String username, String password) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String sql = "SELECT * FROM users WHERE LOWER(TRIM(username))=LOWER(?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");

                if (PasswordUtil.verifyPassword(password, storedHash, salt)) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            storedHash,
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during user login: " + e.getMessage());
            throw new RuntimeException("Database error during login", e);
        }

        return null;
    }
}
