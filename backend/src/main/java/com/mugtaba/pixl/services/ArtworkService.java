package com.mugtaba.pixl.services;

import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.util.DatabaseUtil;
import com.mugtaba.pixl.util.LogUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for handling database operations related to artworks.
 * Provides methods for creating, retrieving, updating, and deleting artworks.
 */
public class ArtworkService {

    private static final String INSERT_ARTWORK =
        "INSERT INTO artworks (title, description, user_id, pixel_data, width, height, is_public, shareable_link, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_ARTWORK =
        "UPDATE artworks SET title = ?, description = ?, pixel_data = ?, width = ?, height = ?, is_public = ?, updated_at = ? " +
        "WHERE id = ? AND user_id = ?";

    private static final String SELECT_ARTWORK_BY_ID =
        "SELECT a.*, u.username FROM artworks a JOIN users u ON a.user_id = u.id WHERE a.id = ?";

    private static final String SELECT_ARTWORKS_BY_USER =
        "SELECT a.*, u.username FROM artworks a JOIN users u ON a.user_id = u.id WHERE user_id = ? ORDER BY a.updated_at DESC";

    private static final String SELECT_PUBLIC_ARTWORKS =
        "SELECT a.*, u.username FROM artworks a JOIN users u ON a.user_id = u.id WHERE a.is_public = true ORDER BY a.created_at DESC LIMIT ? OFFSET ?";

    private static final String SELECT_ARTWORK_BY_LINK =
        "SELECT a.*, u.username FROM artworks a JOIN users u ON a.user_id = u.id WHERE a.shareable_link = ?";

    private static final String DELETE_ARTWORK =
        "DELETE FROM artworks WHERE id = ? AND user_id = ?";

    private static final String COUNT_PUBLIC_ARTWORKS =
        "SELECT COUNT(*) FROM artworks WHERE is_public = true";
    
    /**
     * Creates a new artwork record in the database.
     * 
     * @param artwork the Artwork object containing the artwork details
     * @return the created Artwork object
     * @throws SQLException if an error occurs while accessing the database
     */
    public Artwork createArtwork(Artwork artwork) throws SQLException {

        if (!artwork.validateData()) {
            throw new IllegalArgumentException("Invalid artwork data");
        }

        String shareableLink = generateShareableLink();

        artwork.setShareableLink(shareableLink);
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(INSERT_ARTWORK, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, artwork.getTitle());
            stmt.setString(2, artwork.getDescription());
            stmt.setLong(3, artwork.getUserId());
            stmt.setString(4, artwork.getPixelData());
            stmt.setInt(5, artwork.getWidth());
            stmt.setInt(6, artwork.getHeight());
            stmt.setBoolean(7, artwork.isPublic());
            stmt.setString(8, shareableLink);
            stmt.setTimestamp(9, Timestamp.valueOf(artwork.getCreatedAt()));
            stmt.setTimestamp(10, Timestamp.valueOf(artwork.getUpdatedAt()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating artwork failed, no rows affected");
            }


            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    artwork.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating artwork failed, no ID obtained");
                }
            }
        }

        return artwork;
    }

    /**
     * Updates an existing artwork record in the database.
     * 
     * @param artwork the Artwork object containing the updated artwork details
     * @return true if the artwork was successfully updated, false if the update failed
     * @throws SQLException if an error occurs while accessing the database
     */
    public boolean updateArtwork(Artwork artwork) throws SQLException {

        if (!artwork.validateData() || artwork.getId() == null) {
            throw new IllegalArgumentException("Invalid artwork data or missing ID");
        }

        artwork.setUpdatedAt(LocalDateTime.now());

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(UPDATE_ARTWORK)) {
            
            stmt.setString(1, artwork.getTitle());
            stmt.setString(2, artwork.getDescription());
            stmt.setString(3, artwork.getPixelData());
            stmt.setInt(4, artwork.getWidth());
            stmt.setInt(5, artwork.getHeight());
            stmt.setBoolean(6, artwork.isPublic());
            stmt.setTimestamp(7, Timestamp.valueOf(artwork.getUpdatedAt()));
            stmt.setLong(8, artwork.getId());
            stmt.setLong(9, artwork.getUserId());
            
            int affectedRows =  stmt.executeUpdate();
            
            if (affectedRows > 0) {
                Optional<Artwork> artworkOpt = getArtworkById(artwork.getId());
                if (artworkOpt.isPresent()) {
                    Artwork updatedArtwork = artworkOpt.get();
                    artwork.setUsername(updatedArtwork.getUsername());
                    artwork.setShareableLink(updatedArtwork.getShareableLink());
                    artwork.setCreatedAt(updatedArtwork.getCreatedAt());
                    return true;
                }
            }
            
            return false;
        }
    }

    /**
     * Retrieves an artwork by its ID from the database.
     * 
     * @param id the ID of the artwork
     * @return an Optional containing the Artwork object if found, or an empty Optional if not found
     * @throws SQLException if an error occurs while accessing the database
     */
    public Optional<Artwork> getArtworkById(long id) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ARTWORK_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToArtwork(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieves an artwork by its shareable link.
     * 
     * @param shareableLink the shareable link of the artwork
     * @return an Optional containing the Artwork object if found, or an empty Optional if not found
     * @throws SQLException if an error occurs while accessing the database
     */
    public Optional<Artwork> getArtworkByLink(String shareableLink) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ARTWORK_BY_LINK)) {

            stmt.setString(1, shareableLink);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToArtwork(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieves a list of artworks for a specific user from the database.
     * @param userId the ID of the user
     * @return a list of artworks
     * @throws SQLException if there is an error accessing the database
     */
    public List<Artwork> getArtworksByUser(Long userId) throws SQLException {
        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ARTWORKS_BY_USER)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }
        }

        return artworks;
    }

    /**
     * Retrieves a list of public artworks from the database.
     * @param limit the maximum number of artworks to retrieve
     * @param offset the offset for pagination
     * @return a list of public artworks
     * @throws SQLException if there is an error accessing the database
     */
    public List<Artwork> getPublicArtwork(int limit, int offset) throws SQLException {
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        
        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_PUBLIC_ARTWORKS)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(
                "ArtworkService", "getPublicArtwork",
                String.format(
                    "Database error retrieving public artworks (limit=%d, offset=%d)",
                    limit, offset
                ),
                e
            );
            throw new SQLException("Unable to retrieve public artworks", e);
        }

        return artworks;
    }

    /**
     * Counts the number of public artworks in the database.
     * @return the count of public artworks
     * @throws SQLException if there is an error accessing the database
     */
    public int getPublicArtworkCount() throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(COUNT_PUBLIC_ARTWORKS);
            ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    /**
     * Deletes an artwork from the database.
     * @param artworkId the unique identifier of the artwork to delete
     * @param userId the ID of user who owns the artwork
     * @return true if the artwork was successfully deleted, false otherwise
     * @throws SQLException if there is an error accessing the database
     */
    public boolean deleteArtwork(Long artworkId, Long userId) throws SQLException {

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(DELETE_ARTWORK)) {

            stmt.setLong(1, artworkId);
            stmt.setLong(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Checks if the user owns the artwork
     * 
     * @param artworkId the ID of the artwork
     * @param userId the ID of the user
     * @return true if the user owns the artwork, false otherwise
     * @throws SQLException if there is an error accessing the database
     */
    public boolean isArtworkOwner(Long artworkId, Long userId) throws SQLException {
        Optional<Artwork> artwork = getArtworkById(artworkId);
        return artwork.isPresent() && artwork.get().getUserId().equals(userId);
    }

    /**
     * Maps a ResultSet to an Artwork object.
     * 
     * @param rs the ResultSet to map
     * @return the mapped Artwork object
     * @throws SQLException if there is an error mapping the ResultSet
     */
    private Artwork mapResultSetToArtwork(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();

        artwork.setId(rs.getLong("id"));
        artwork.setTitle(rs.getString("title"));
        artwork.setDescription(rs.getString("description"));
        artwork.setUserId(rs.getLong("user_id"));
        artwork.setUsername(rs.getString("username"));
        artwork.setPixelData(rs.getString("pixel_data"));
        artwork.setWidth(rs.getInt("width"));
        artwork.setHeight(rs.getInt("height"));
        artwork.setPublic(rs.getBoolean("is_public"));
        artwork.setShareableLink(rs.getString("shareable_link"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            artwork.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            artwork.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return artwork;
    }

    /**
     * Generates a unique shareable link for an artwork.
     * 
     * @return a randomly generated shareable link
     */
    private String generateShareableLink() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
