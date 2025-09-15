package com.mugtaba.pixl.services;

import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.util.DatabaseUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for artwork-related operations including creation, retrieval,
 * updating, and deletion of artwork records. Handles database interactions
 * for artwork management.
 */
public class ArtworkService {

    /**
     * Creates a new artwork record in the database.
     * Inserts the artwork details including user ID, title, pixel data, width, and height.
     * Sets the generated ID, created_at, and updated_at back to the artwork object upon successful creation.
     *
     * @param artwork the Artwork object containing the artwork details to be created,
     *                must not be null and should have a valid fields.
     * @return true if the artwork was successfully created and ID was generated,
     * false if the creation failed or database error occurred.
     * @throws IllegalArgumentException if the artwork parameter is null or contains invalid data.
     */
    public boolean createArtwork(Artwork artwork) {
        // Input validation
        if (artwork == null) {
            throw new IllegalArgumentException("Artwork cannot be null");
        }
        if (artwork.getUserId() <= 0) {
            throw new IllegalArgumentException("User ID must be a positive integer");
        }
        if (artwork.getTitle() == null || artwork.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Artwork title cannot be null or empty");
        }
        if (artwork.getPixels() == null || artwork.getPixels().trim().isEmpty()) {
            throw new IllegalArgumentException("Pixel data cannot be null or empty");
        }
        if (artwork.getWidth() <= 0 || artwork.getHeight() <= 0) {
            throw new IllegalArgumentException("Width and height must be positive integers");
        }

        String sql = "INSERT INTO artworks (user_id, title, pixels, width, height) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, artwork.getUserId());
            stmt.setString(2, artwork.getTitle());
            stmt.setString(3, artwork.getPixels());
            stmt.setInt(4, artwork.getWidth());
            stmt.setInt(5, artwork.getHeight());

            int affectedRows = stmt.executeUpdate();

            // Check if the insert was successful and retrieve the generated ID
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        artwork.setId(generatedKeys.getInt(1));
                        artwork.setCreatedAt(generatedKeys.getTimestamp("created_at").toLocalDateTime());
                        artwork.setUpdatedAt(generatedKeys.getTimestamp("updated_at").toLocalDateTime());
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error creating artwork: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            return false;
        }
    }
}
