package com.mugtaba.pixl.services;

import com.mugtaba.pixl.models.AdminStats;
import com.mugtaba.pixl.models.Artwork;
import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.models.UserActivity;
import com.mugtaba.pixl.util.CacheUtil;
import com.mugtaba.pixl.util.DatabaseUtil;
import com.mugtaba.pixl.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for admin operations including user management,
 * artwork management, and system statistics.
 */
public class AdminService {

    private static final String COMPONENT_NAME = "AdminService";

    // user queries
    private static final String SELECT_ALL_USERS = "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?";

    private static final String COUNT_ALL_USERS = "SELECT COUNT(*) FROM users";

    private static final String SELECT_USER_ACTIVITY = """
        SELECT u.id, u.username, u.email, u.is_admin,
        COUNT(a.id) as artwork_count,
        SUM(CASE WHEN a.is_public THEN 1 ELSE 0 END) as public_artwork_count
        FROM users u
        LEFT JOIN artworks a ON u.id = a.user_id
        GROUP BY u.id, u.username, u.email, u.is_admin
        ORDER BY artwork_count DESC LIMIT ? OFFSET ?
        """;

    private static final String DELETE_USER = "DELETE FROM users WHERE id = ?";

    private static final String UPDATE_USER_ADMIN_STATUS = "UPDATE users SET is_admin = ?, updated_at = NOW() WHERE id = ?";

    // artwork queries
    private static final String SELECT_ALL_ARTWORKS = """
        SELECT a.*, u.username FROM artworks a
        JOIN users u ON a.user_id = u.id
        ORDER BY a.created_at DESC LIMIT ? OFFSET ?
    """;

    private static final String COUNT_ALL_ARTWORKS = "SELECT COUNT(*) FROM artworks";

    private static final String DELETE_ARTWORK_ADMIN = "DELETE FROM artworks WHERE id = ?";

    // stats queries
    private static final String GET_USER_STATS = """
        SELECT
        COUNT(*) as total_users,
        SUM(CASE WHEN DATE(created_at) = CURDATE() THEN 1 ELSE 0 END) as new_today,
        SUM(CASE WHEN DATE(created_at) >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 ELSE 0 END) as new_week,
        SUM(CASE WHEN DATE(created_at) >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) as new_month
        FROM users
    """;

    private static final String GET_ARTWORK_STATS = """
        SELECT
        COUNT(*) as total_artworks,
        SUM(CASE WHEN is_public = TRUE THEN 1 ELSE 0 END) as public_artworks,
        SUM(CASE WHEN is_public = FALSE THEN 1 ELSE 0 END) as private_artworks,
        SUM(CASE WHEN DATE(created_at) = CURDATE() THEN 1 ELSE 0 END) as new_today,
        SUM(CASE WHEN DATE(created_at) >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 ELSE 0 END) as new_week,
        SUM(CASE WHEN DATE(created_at) >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) as new_month
        FROM artworks
    """;

    /**
     * Gets all users with pagination 
     * @param limit the maximum number of user to retrieve
     * @param offset the offset to start retrieving users from
     * @return List of users
     * @throws SQLException if an error occurs while accessing the database
     */
    public List<User> getAllUsers(int limit, int offset) throws SQLException {
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_USERS)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }

            LogUtil.logInfo(
                COMPONENT_NAME, "getAllUsers",
                String.format("Retrieved %d users (limit: %d, offset: %d)", users.size(), limit, offset)
            );
        }

        return users;
    }

    /**
     * Gets total user count
     * @return total number of users
     * @throws SQLException if an error occurs while accessing the database
     */
    public long getUserCount() throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(COUNT_ALL_USERS)) {
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0;
    }

    /**
     * Gets user activity statistics for admin monitoring
     * @param limit the maximum number of user activities to retrieve
     * @param offset the offset to start retrieving user activities from
     * @return List of UserActivity objects
     * @throws SQLException if an error occurs while accessing the database
     */
    public List<UserActivity> getUserActivity(int limit, int offset) throws SQLException {

        List<UserActivity> activities = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_USER_ACTIVITY)) {
                stmt.setInt(1, limit);
                stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserActivity activity = new UserActivity();
                    activity.setUserId(rs.getLong("id"));
                    activity.setUsername(rs.getString("username"));
                    activity.setEmail(rs.getString("email"));
                    activity.setAdmin(rs.getBoolean("is_admin"));
                    activity.setArtworkCount(rs.getLong("artwork_count"));
                    activity.setPublicArtworkCount(rs.getLong("public_artwork_count"));

                    activities.add(activity);
                }
            }

            LogUtil.logInfo(COMPONENT_NAME, "getUserActivity", String.format("Retrieved %d user activities", activities.size()));

        }

        return activities;
    }

    /**
     * Deletes a user and all their associated artworks
     * @param userId the ID of the user to delete
     * @return true if the user was deleted, false otherwise
     * @throws SQLException if an error occurs while accessing the database
     */
    public boolean deleteUser(Long userId) throws SQLException {

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(DELETE_USER)) {

            stmt.setLong(1, userId);
            int deletedRows = stmt.executeUpdate();

            if (deletedRows > 0) {
                // invalidate user cache
                CacheUtil.remove(String.format("user_id_%d", userId));
                CacheUtil.removePattern("user_username_*");
                CacheUtil.removePattern("user_artworks_*");

                LogUtil.logInfo(COMPONENT_NAME, "deleteUser", String.format("Admin deleted user %d", userId));
                return true;
            }
        }

        return false;
    }

    /**
     * Updates a user's admin status
     * @param userId the ID of the user to update
     * @param isAdmin the new admin status
     * @return true if the update was successful, false otherwise
     * @throws SQLException if an error occurs while accessing the database
     */
    public boolean updateUserAdminStatus(Long userId, boolean isAdmin) throws SQLException {

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(UPDATE_USER_ADMIN_STATUS)) {

            stmt.setBoolean(1, isAdmin);
            stmt.setLong(2, userId);

            int updatedRows = stmt.executeUpdate();

            if (updatedRows > 0) {
                // invalidate user cache
                CacheUtil.remove(String.format("user_id_%d", userId));

                LogUtil.logInfo(
                    COMPONENT_NAME, "updateUserAdminStatus",
                    String.format("Updated user %d admin status to %s", userId, isAdmin)
                );
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all artworks with pagination (admin view)
     * @param limit the maximum number of artworks to retrieve
     * @param offset the starting point for retrieval
     * @return a list of artworks
     * @throws SQLException if an error occurs while accessing the database
     */
    public List<Artwork> getAllArtworks(int limit, int offset) throws SQLException {

        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_ARTWORKS)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }

            LogUtil.logInfo(
                COMPONENT_NAME, "getAllArtworks",
                String.format("Retrieved %d artworks (limit: %d, offset: %d)", artworks.size(), limit, offset)
            );
        }

        return artworks;
    }

    /**
     * Gets total artwork count
     * @return total number of artworks
     * @throws SQLException if an error occurs while accessing the database
     */
    public long getArtworkCount() throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(COUNT_ALL_ARTWORKS)) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0;
    }

    /**
     * Deletes an artwork (admin override - bypasses ownership check)
     * @param artworkId the ID of the artwork to delete
     * @return true if the deletion was successful, false otherwise
     * @throws SQLException if an error occurs while accessing the database
     */
    public boolean deleteArtwork(Long artworkId) throws SQLException {

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(DELETE_ARTWORK_ADMIN)) {

            stmt.setLong(1, artworkId);
            int deletedRows = stmt.executeUpdate();

            if (deletedRows > 0) {
                // invalidate artwork cache
                CacheUtil.remove(String.format("artwork_id_%d", artworkId));
                CacheUtil.removePattern("public_artworks_*");
                CacheUtil.remove("public_artworks_count");

                LogUtil.logInfo(
                COMPONENT_NAME, "deleteArtwork",
                String.format("Admin deleted artwork %d", artworkId)
                );
                return true;
            }
        }

        return false;
    }

    /**
     * Gets system statistics for admin dashboard
     * @return AdminStats object containing system statistics
     * @throws SQLException if an error occurs while accessing the database
     */
    public AdminStats getSystemStats() throws SQLException {

        AdminStats stats = new AdminStats();

        // get user stats
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(GET_USER_STATS)) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                stats.setTotalUsers(rs.getLong("total_users"));
                stats.setNewUsersToday(rs.getLong("new_today"));
                stats.setNewUsersThisWeek(rs.getLong("new_week"));
                stats.setNewUsersThisMonth(rs.getLong("new_month"));
            }
        }

        // get artwork stats
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(GET_ARTWORK_STATS)) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                stats.setTotalArtworks(rs.getLong("total_artworks"));
                stats.setPublicArtworks(rs.getLong("public_artworks"));
                stats.setPrivateArtworks(rs.getLong("private_artworks"));
                stats.setNewArtworksToday(rs.getLong("new_today"));
                stats.setNewArtworksThisWeek(rs.getLong("new_week"));
                stats.setNewArtworksThisMonth(rs.getLong("new_month"));
            }
        }

        // get cache stats
        stats.setCacheStats(CacheUtil.getStats());

        LogUtil.logInfo(
            COMPONENT_NAME, "getSystemStats",
            String.format(
                "Generated system stats: %d users, %d artworks",
                stats.getTotalUsers(), stats.getTotalArtworks()
            )
        );

        return stats;
    }

    /**
     * Maps ResultSet to User object
     * @param rs ResultSet to map
     * @return User object
     * @throws SQLException if an error occurs while accessing the database
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setIsAdmin(rs.getBoolean("is_admin"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return user;
    }

    /**
     * Maps ResultSet to artwork object
     * @param rs ResultSet to map
     * @return Artwork object
     * @throws SQLException if an error occurs while accessing the database
     */
    private Artwork mapResultSetToArtwork(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();

        artwork.setId(rs.getLong("id"));
        artwork.setUserId(rs.getLong("user_id"));
        artwork.setUsername(rs.getString("username"));
        artwork.setTitle(rs.getString("title"));
        artwork.setDescription(rs.getString("description"));
        artwork.setWidth(rs.getInt("width"));
        artwork.setHeight(rs.getInt("height"));
        artwork.setPixelData(rs.getString("pixel_data"));
        artwork.setPublic(rs.getBoolean("is_public"));
        artwork.setShareableLink(rs.getString("shareable_link"));
        artwork.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        artwork.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return artwork;
    }

}
