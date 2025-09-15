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

    /**
     * Retrieves an artwork by its unique identifier.
     *
     * @param id the unique identifier of the artwork to retrieve
     * @return the artwork object if found, null if not found or if an error occurs
     * @throws IllegalArgumentException if the id is not a positive integer
     */
    public Artwork getArtworkById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Artwork ID must be a positive number");
        }

        String sql = "SELECT * FROM artworks WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToArtwork(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting artwork by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Retrieves all artworks created by specific user.
     *
     * @param userId the unique identifier of the user
     * @return a list of artwork objects created by the user, empty list if none found or error occurs
     * @throws IllegalArgumentException if the userId is not a positive integer
     */
    public List<Artwork> getArtworksByUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }

        List<Artwork> artworks = new ArrayList<>();
        String sql = "SELECT * FROM artworks WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                artworks.add(mapResultSetToArtwork(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user artworks: " + e.getMessage());
        }

        return artworks;
    }

    /**
     * Retrieves all the artworks from the database
     *
     * @return a List of all Artwork objects, empty list if none found or error occurs
     */
    public List<Artwork> getAllArtworks() {
        List<Artwork> artworks = new ArrayList<>();
        String sql = "SELECT * FROM artworks ORDER BY created_at DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                artworks.add(mapResultSetToArtwork(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all artworks: " + e.getMessage());
        }

        return artworks;
    }

    /**
     * Helper method to map ResultSet row to an Artwork object.
     *
     * @param rs the ResultSet containing artwork data
     * @return an Artwork object with data from Resultset
     * @throws SQLException if a database access error occurs
     */
    private Artwork mapResultSetToArtwork(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();

        artwork.setId(rs.getInt("id"));
        artwork.setUserId(rs.getInt("user_id"));
        artwork.setTitle(rs.getString("title"));
        artwork.setPixels(rs.getString("pixels"));
        artwork.setWidth(rs.getInt("width"));
        artwork.setHeight(rs.getInt("height"));
        artwork.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            artwork.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return artwork;
    }
}
