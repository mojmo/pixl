package com.mugtaba.pixl.services;

import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.util.CacheUtil;
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

    private static final String COMPONENT_NAME = "ArtworkService";

    // Cache keys
    private static final String CACHE_PUBLIC_ARTWORKS = "public_artworks_page_%d_limit_%d";
    private static final String CACHE_PUBLIC_ARTWORKS_COUNT = "public_artworks_count";
    private static final String CACHE_ARTWORK_BY_ID = "artwork_id_%d";
    private static final String CACHE_ARTWORK_BY_LINK = "artwork_link_%s";
    private static final String CACHE_USER_ARTWORKS = "user_artworks_%d";

    // Cache TTL in minutes
    private static final int PUBLIC_ARTWORK_TTL = 10;
    private static final int ARTWORK_DETAILS_TTL = 30;
    private static final int USER_ARTWORKS_TTL = 15; 

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
     * Creates a new artwork record in the database and invalidates relevant caches.
     * 
     * @param artwork the Artwork object containing the artwork details
     * @return the created Artwork object
     * @throws SQLException if an error occurs while accessing the database
     */
    public Artwork createArtwork(Artwork artwork) throws SQLException {

        if (artwork.validatedData()) {
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

            // Invalidate relevant caches
            invalidateArtworkCaches(artwork.getUserId(), artwork.isPublic());

            LogUtil.logInfo(
                COMPONENT_NAME, "createArtwork",
                String.format("Created artwork %d and invalidated caches", artwork.getId())
            );
        }

        return artwork;
    }

    /**
     * Updates an existing artwork record in the database and invalidates relevant caches.
     * 
     * @param artwork the Artwork object containing the updated artwork details
     * @return true if the artwork was successfully updated, false if the update failed
     * @throws SQLException if an error occurs while accessing the database
     */
    public boolean updateArtwork(Artwork artwork) throws SQLException {

        if (artwork.validatedData() || artwork.getId() == null) {
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

                    // Invalidate specific artwork cache
                    CacheUtil.remove(String.format(CACHE_ARTWORK_BY_ID, artwork.getId()));

                    // Invalidate user and public caches
                    invalidateArtworkCaches(artwork.getUserId(), artwork.isPublic());

                    LogUtil.logInfo(
                        COMPONENT_NAME, "updateArtwork",
                        String.format("Updated artwork %d and invalidated caches", artwork.getId())
                    );
                    return true;
                }
            }
            
            return false;
        }
    }

    /**
     * Retrieves an artwork by its ID from the database with caching support.
     * 
     * @param id the ID of the artwork
     * @return an Optional containing the Artwork object if found, or an empty Optional if not found
     * @throws SQLException if an error occurs while accessing the database
     */
    public Optional<Artwork> getArtworkById(long id) throws SQLException {
        String cacheKey = String.format(CACHE_ARTWORK_BY_ID, id);

        // Try cach first
        Artwork cachedArtwork = CacheUtil.get(cacheKey, Artwork.class);
        if (cachedArtwork != null) {
            LogUtil.logInfo(
                COMPONENT_NAME, "getArtworkById",
                String.format("Cache hit for artwork ID: %d", id)
            );
            return Optional.of(cachedArtwork);
        }

        // Cache miss - fetch from database
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ARTWORK_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Artwork artwork = mapResultSetToArtwork(rs);

                    // Cache the result
                    CacheUtil.put(cacheKey, artwork, ARTWORK_DETAILS_TTL);
                    LogUtil.logInfo(
                        COMPONENT_NAME, "getArtworkById",
                        String.format("Database fetch and cached artwork ID: %d", id)
                    );
                    return Optional.of(artwork);
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
        String cacheKey = String.format(CACHE_ARTWORK_BY_LINK, shareableLink);

        // Try cache first
        Artwork cachedArtwork = CacheUtil.get(cacheKey, Artwork.class);
        if (cachedArtwork != null) {
            LogUtil.logInfo(
                COMPONENT_NAME, "getArtworkByLink",
                String.format("Cache hit for shareable link: %s", shareableLink)
            );
            return Optional.of(cachedArtwork);
        }

        // Cache miss - fetch from database
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ARTWORK_BY_LINK)) {

            stmt.setString(1, shareableLink);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Artwork artwork = mapResultSetToArtwork(rs);

                    // Cache the result
                    CacheUtil.put(cacheKey, artwork, ARTWORK_DETAILS_TTL);
                    LogUtil.logInfo(
                        COMPONENT_NAME, "getArtworkByLink",
                        String.format("Database fetch and cached artwork link: %s", shareableLink)
                    );
                    return Optional.of(artwork);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieves a list of artworks for a specific user from the database with caching support.
     * @param userId the ID of the user
     * @return a list of artworks
     * @throws SQLException if there is an error accessing the database
     */
    public List<Artwork> getArtworksByUser(Long userId) throws SQLException {
        String cacheKey = String.format(CACHE_USER_ARTWORKS, userId);

        // Try cache first
        @SuppressWarnings("unchecked")
        List<Artwork> cachedArtworks = CacheUtil.get(cacheKey, List.class);
        if (cachedArtworks != null) {
            LogUtil.logInfo(
                COMPONENT_NAME, "getArtworksByUser",
                String.format("Cache hit for user artworks: %d", userId)
            );
            return cachedArtworks;
        }

        // Cache miss - fetch from database
        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ARTWORKS_BY_USER)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }

            // Cache the results
            CacheUtil.put(cacheKey, artworks, USER_ARTWORKS_TTL);
            LogUtil.logInfo(
                COMPONENT_NAME, "getArtworksByUser",
                String.format("Database fetch and cached user artworks: %d (count: %d)", userId, artworks.size())
            );
        }

        return artworks;
    }

    /**
     * Retrieves a list of public artworks from the database with caching support.
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

        int page = (offset / limit) + 1;
        String cacheKey = String.format(CACHE_PUBLIC_ARTWORKS, page, limit);
        
        // Try cache first
        @SuppressWarnings("unchecked")
        List<Artwork> cachedArtworks = CacheUtil.get(cacheKey, List.class);
        if (cachedArtworks != null) {
            LogUtil.logInfo(
                COMPONENT_NAME, "getPublicArtwork",
                String.format("Cache hit for public artworks (page=%d, limit=%d)", page, limit)
            );
            return cachedArtworks;
        }

        // Cache miss - fetch from database
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

            // Cache the results
            CacheUtil.put(cacheKey, artworks, PUBLIC_ARTWORK_TTL);
            LogUtil.logInfo(
                COMPONENT_NAME, "getPublicArtwork",
                String.format(
                    "Database fetch and cached public artworks (page=%d, limit=%d, count=%d)",
                    page, limit, artworks.size()
                )
            );
        } catch (SQLException e) {
            LogUtil.logError(
                COMPONENT_NAME, "getPublicArtwork",
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
     * Counts the number of public artworks in the database with caching support.
     * @return the count of public artworks
     * @throws SQLException if there is an error accessing the database
     */
    public int getPublicArtworkCount() throws SQLException {
        // Try cache first
        Integer cachedCount = CacheUtil.get(CACHE_PUBLIC_ARTWORKS_COUNT, Integer.class);
        if (cachedCount != null) {
            LogUtil.logInfo(COMPONENT_NAME, "getPublicArtworkCount", "Cache hit for public count");
            return cachedCount;
        }

        // Cache miss - fetch from database
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(COUNT_PUBLIC_ARTWORKS);
            ResultSet rs = stmt.executeQuery()) {

            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }

            // Cache the result
            CacheUtil.put(CACHE_PUBLIC_ARTWORKS_COUNT, count, PUBLIC_ARTWORK_TTL);
            LogUtil.logInfo(
                COMPONENT_NAME, "getPublicArtworkCount",
                String.format("Database fetch and cached public count: %d", count)
            );

            return count;
        }
    }

    /**
     * Deletes an artwork from the database and invalidates relevant caches.
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

            boolean deleted = stmt.executeUpdate() > 0;

            if (deleted) {

                Artwork deletedArtwork = getArtworkById(artworkId).orElse(null);
                boolean wasPublic = deletedArtwork != null && deletedArtwork.isPublic();

                // Invalidate specific artwork cache
                CacheUtil.remove(String.format(CACHE_ARTWORK_BY_ID, artworkId));
                
                // Invalidate user and public caches if needed
                invalidateArtworkCaches(userId, wasPublic);
                
                LogUtil.logInfo(
                    COMPONENT_NAME, "deleteArtwork",
                    String.format("Deleted artwork %d and invalidated caches", artworkId)
                );
            }

            return deleted;
        }
    }

    /**
     * Invalidates artwork-related caches.
     * @param userId the ID of the user whose artwork caches should be invalidated
     * @param affectsPublic true if the artwork affects public listings, false otherwise
     */
    private void invalidateArtworkCaches(Long userId, boolean affectsPublic) {
        // Invalidates user's artwork cache
        CacheUtil.remove(String.format(CACHE_USER_ARTWORKS, userId));

        // Invalidate public caches if this artwork affects public listings
        if (affectsPublic) {
            CacheUtil.removePattern("public_artworks_*");
            CacheUtil.remove(CACHE_PUBLIC_ARTWORKS_COUNT);
        }

        LogUtil.logInfo(
            COMPONENT_NAME, "invalidateArtworkCaches",
            String.format("Invalidated caches for user: %d (public affected: %s)", userId, affectsPublic)
        );
    }

    /**
     * Checks if the user owns the artwork
     * 
     * @param artworkId the ID of the artwork
     * @param userId the ID of the user
     * @return true if the user owns the artwork, false otherwise
     * @throws SQLException if there is an error accessing the database
     */
    public boolean isOwner(Long artworkId, Long userId) throws SQLException {
        Optional<Artwork> artwork = getArtworkById(artworkId);
        return artwork.isEmpty() || !artwork.get().getUserId().equals(userId);
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
