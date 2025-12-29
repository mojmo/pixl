package com.mugtaba.pixl.services;

import com.mugtaba.pixl.models.User;
import com.mugtaba.pixl.util.CacheUtil;
import com.mugtaba.pixl.util.DatabaseUtil;
import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.PasswordUtil;
import com.mugtaba.pixl.services.OtpService.OtpType;
import com.mugtaba.pixl.services.OtpService.OtpValidationResult;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service class for user-related operations including registration and authentication.
 * Handles database interactions for user management with secure password handling.
 */
public class UserService {

    private static final String COMPONENT_NAME = "UserService";

    // Cache keys
    private static final String CACHE_USER_BY_ID = "user_id_%d";
    private static final String CACHE_USER_BY_USERNAME = "user_username_%s";
    private static final String CACHE_OTP_KEY = "otp_%s_%s";

    // Cache TTL in minutes
    private static final int USER_CACHE_TTL = 30;

    private static final String INSERT_USER =
    "INSERT INTO users (username, email, password_hash, salt, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_USER_BY_ID = "SELECT * FROM users WHERE id = ?";

    private static final String SELECT_USER_BY_USERNAME = "SELECT * FROM users WHERE username = ?";

    private static final String SELECT_USER_BY_EMAIL = "SELECT * FROM users WHERE email = ? LIMIT 1";

    private static final String SELECT_USER_BY_USERNAME_OR_EMAIL = "SELECT * FROM users WHERE username = ? OR email = ?";

    private static final String UPDATE_USER_PASSWORD = "UPDATE users SET password_hash = ?, salt = ?, updated_at = ? WHERE id = ?";

    private static final String UPDATE_USER_PROFILE = "UPDATE users SET username = ?, email = ?, updated_at = ? WHERE id = ?";

    private static final String CHECK_USERNAME_EXISTS = "SELECT COUNT(*) FROM users WHERE username = ?";

    private static final String CHECK_EMAIL_EXISTS = "SELECT COUNT(*) FROM users WHERE email = ?";

    private static final String CHECK_USERNAME_TAKEN = "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?";

    private static final String CHECK_EMAIL_TAKEN = "SELECT COUNT(*) FROM users WHERE email = ? AND id != ?";

    private final EmailService emailService;
    private final OtpService otpService;

    public UserService() {
        this.emailService = new EmailService();
        this.otpService = new OtpService();
    }

    /**
     * Registers a new user with the system.
     * Generates a salt, hashes the password, and stores user credentials in the database.
     *
     * @param username the username for the new account
     * @param email the email address for the new account
     * @param password the plain text password to be hashed and stored
     * @return the registered User object
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws SQLException if a database access error occurs
     */
    public User registerUser(String username, String email, String password) throws SQLException {

        // Validate input
        User tempUser = new User(username, email);

        if (tempUser.validatedForRegistration()) {
            throw new IllegalArgumentException("Invalid user data provided");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and include uppercase, lowercase, number, and special character");
        }

        // Check if username or email already exists
        if (isUsernameExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (isEmailExists(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Generate salt and hash password
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        User newUser = new User(username, email, passwordHash, salt);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, newUser.getUsername());
                stmt.setString(2, newUser.getEmail());
                stmt.setString(3, newUser.getPasswordHash());
                stmt.setString(4, newUser.getSalt());
                stmt.setTimestamp(5, Timestamp.valueOf(newUser.getCreatedAt()));
                stmt.setTimestamp(6, Timestamp.valueOf(newUser.getUpdatedAt()));

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newUser.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
        }

        return newUser;
    }

    /**
     * Authenticates a user by username or email and password.
     * @param usernameOrEmail the username or email of the user to authenticate
     * @param password the plain text password to be verified
     * @return an Optional containing the authenticated User object if successful, empty otherwise
     * @throws SQLException if a database access error occurs
     */
    public Optional<User> authenticateUser(String usernameOrEmail, String password) throws SQLException {

        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
                return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_USER_BY_USERNAME_OR_EMAIL)) {

            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);

                    // Verify password
                    if (PasswordUtil.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
                        return Optional.of(user);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieves a user by their ID.
     * @param userId the ID of the user to retrieve
     * @return an Optional containing the User object if found, empty otherwise
     * @throws SQLException if a database access error occurs
     */
    public Optional<User> getUserById(Long userId) throws SQLException {
        String cacheKey = String.format(CACHE_USER_BY_ID, userId);

        // Try cache first
        User cachedUser = CacheUtil.get(cacheKey, User.class);
        if (cachedUser != null) {
            LogUtil.logInfo(
                COMPONENT_NAME, "getUserById",
                String.format("Cache hit for user ID: %d", userId)
            );
        }

        // Cache miss - fetch from database
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_USER_BY_ID)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);

                    // Cache the result
                    CacheUtil.put(cacheKey, user, USER_CACHE_TTL);
                    CacheUtil.put(String.format(CACHE_USER_BY_USERNAME, user.getUsername()), user, USER_CACHE_TTL);

                    LogUtil.logInfo(
                        COMPONENT_NAME, "getUserById",
                        String.format("Database fetch and cached user: %d", userId)
                    );

                    return Optional.of(user);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieves a user by their username.
     * @param username the username of the user to retrieve
     * @return an Optional containing the User object if found, empty otherwise
     * @throws SQLException if a database access error occurs
     */
    public Optional<User> getUserByUsername(String username) throws SQLException {
        String cacheKey = String.format(CACHE_USER_BY_USERNAME, username);

        // Try cache first
        User cachedUser = CacheUtil.get(cacheKey, User.class);
        if (cachedUser != null) {
            LogUtil.logInfo(
                COMPONENT_NAME, "getUserByUsername",
                String.format("Cache hit for user username: %s", username)
            );
            return Optional.of(cachedUser);
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_USER_BY_USERNAME)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);

                    // Cache the result
                    CacheUtil.put(cacheKey, user, USER_CACHE_TTL);

                    LogUtil.logInfo(
                        COMPONENT_NAME, "getUserByUsername",
                        String.format("Database fetch and cached user: %s", username)
                    );
                    return Optional.of(user);
                }
            }
        }

        return Optional.empty();
    }


    /**
     * Updates a user's profile information (username and email).
     * @param userId the ID of the user to update
     * @param newUsername the new username
     * @param newEmail the new email
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateProfile(Long userId, String newUsername, String newEmail) throws SQLException {
        User tempUser = new User(newUsername, newEmail);
        if (tempUser.validatedForRegistration()) {
            throw new IllegalArgumentException("Invalid user data provided");
        }

        // Check if new username/email already exits (excluding current user)
        if (isUsernameExistsForOtherUser(newUsername, userId)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (isEmailExistsForOtherUser(newEmail, userId)) {
            throw new IllegalArgumentException("Email already exists");
        }

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(UPDATE_USER_PROFILE)) {
                stmt.setString(1, newUsername);
                stmt.setString(2, newEmail);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setLong(4, userId);

                boolean updated = stmt.executeUpdate() > 0;

                if (updated) {
                    // Invalidate user caches
                    CacheUtil.remove(String.format(CACHE_USER_BY_ID, userId));
                    CacheUtil.removePattern("user_username_*");

                    LogUtil.logInfo(
                        COMPONENT_NAME, "updateProfile",
                        String.format("Updated user %d profile and invalidated cache", userId)
                    );
                }

                return updated;
            }
    }

    /**
     * Initiates the password reset process by generating an OTP and sending it via email.
     * @param emailOrUsername email address or username of the user requesting password reset
     * @return true if password reset initiation was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean initiatePasswordReset(String emailOrUsername) throws SQLException {
        String normalizedInput = emailOrUsername.trim().toLowerCase();

        Optional<User> userOpt = getUserByUsernameOrEmail(normalizedInput);

        if (userOpt.isEmpty()) {
            LogUtil.logWarning(
                COMPONENT_NAME, "initiatePasswordReset",
                String.format("Password reset requested for non-existent user: %s", normalizedInput)
            );

            return true; // Return true to not reveal user existence
        }

        User user = userOpt.get();

        if (emailService.isConfiguredEmail()) {
            LogUtil.logError(
                COMPONENT_NAME, "initiatePasswordReset",
                "Email service not configured - can not send password reset email", null
            );

            return false;
        }

        // Generate OTP
        String otp = otpService.generateOtp(user.getEmail(), OtpType.PASSWORD_RESET);

        if (otp == null) {
            LogUtil.logWarning(
                COMPONENT_NAME, "initiatePassword",
                String.format("OTP generation failed (rate limited) for user: %s", user.getEmail())
            );

            return false; // rate limited
        }

        // send email
        boolean emailSent = emailService.sendPasswordResetOTP(
            user.getEmail(),
            user.getUsername(),
            otp,
            15 // 15 minutes expiration
        );

        if (emailSent) {
            LogUtil.logInfo(
                COMPONENT_NAME, "initiatePasswordReset",
                String.format("Password reset OTP sent to user: %s", user.getEmail())
            );
        } else {
            LogUtil.logError(
                COMPONENT_NAME, "initiatePasswordReset",
                String.format(
                    "Failed to send password reset email to: %s",
                    user.getEmail()
                ),
                null
            );
        }

        return emailSent;
    }

    /**
     * Verifies the OTP for password reset.
     * @param email user's email address
     * @param otp OTP code provided by the user
     * @return verification result
     */
    public OtpValidationResult verifyPasswordResetOtp(String email, String otp) {
        OtpValidationResult result = otpService.validateOtp(email, OtpType.PASSWORD_RESET, otp);

        LogUtil.logInfo(
            COMPONENT_NAME, "verifyPasswordResetOtp",
            String.format("Password reset OTP verification for %s: %s", email, result)
        );

        return result;
    }

    /**
     * Completes password reset with new password after OTP verification
     * @param email user's email address
     * @param otp OTP code for verification
     * @param newPassword new password
     * @return true if password reset successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean completePasswordReset(String email, String otp, String newPassword)
        throws SQLException {

        // Verify OTP
        OtpValidationResult otpResult = otpService.validateOtp(
            email, OtpType.PASSWORD_RESET, otp
        );

        if (otpResult != OtpValidationResult.VALID) {
            LogUtil.logWarning(
                COMPONENT_NAME, "completePasswordReset",
                String.format("Invalid OTP for password reset: %s (result: %s)", email, otpResult)
            );
            return false;
        }

        // Find user by email
        Optional<User> userOpt = getUserByEmail(email);
        if (userOpt.isEmpty()) {
            LogUtil.logWarning(
                COMPONENT_NAME, "completePasswordReset",
                String.format("User not found for password reset: %s", email)
            );

            return false;
        }

        User user = userOpt.get();

        // Update password
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(newPassword, salt);

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(UPDATE_USER_PASSWORD)) {
                stmt.setString(1, hashedPassword);
                stmt.setString(2, salt);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setLong(4, user.getId());

                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    // Invalidate user caches
                    CacheUtil.remove(String.format(CACHE_USER_BY_ID, user.getId()));
                    CacheUtil.remove(String.format(CACHE_USER_BY_USERNAME, user.getUsername()));
                    CacheUtil.remove(String.format(CACHE_OTP_KEY, email, OtpType.PASSWORD_RESET));

                    LogUtil.logInfo(
                        COMPONENT_NAME, "completePasswordReset",
                        String.format("Password reset completed successfully for user: %s", user.getEmail())
                    );

                    return true;
                }
        } catch (SQLException e) {
            LogUtil.logError(
                "UserService", "completePasswordReset",
                String.format("Database error updating password for user: %s", email), e
            );
            throw e;
        }

        return false;
    }

    /**
     * Retrieves a user by their email.
     * @param email the email of the user to retrieve
     * @return an Optional containing the User object if found, empty otherwise
     * @throws SQLException if a database access error occurs
     */
    public Optional<User> getUserByEmail(String email) throws SQLException {
        
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_USER_BY_EMAIL)) {
                stmt.setString(1, email);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        User user = mapResultSetToUser(rs);
                        return Optional.of(user);
                    }
                }
        }

        return Optional.empty();
    }

    public Optional<User> getUserByUsernameOrEmail(String usernameOrEmail) throws SQLException {

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(SELECT_USER_BY_USERNAME_OR_EMAIL)) {

            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);

                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a username already exists in the database.
     * @param username the username to check
     * @return true if the username exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private boolean isUsernameExists(String username) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(CHECK_USERNAME_EXISTS)) {
                stmt.setString(1, username);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
        }
    }

    /**
     * Checks if an email already exists in the database.
     * @param email the email to check
     * @return true if the email exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private boolean isEmailExists(String email) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(CHECK_EMAIL_EXISTS)) {
                stmt.setString(1, email);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
        }
    }

    /**
     * Checks if a username already exists in the database for a different user.
     * @param username the username to check
     * @param excludeUserId the ID of the user to exclude
     * @return true if the username exists for another user, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private boolean isUsernameExistsForOtherUser(String username, Long excludeUserId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(CHECK_USERNAME_TAKEN)) {
                stmt.setString(1, username);
                stmt.setLong(2, excludeUserId);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
        }
    }

    /**
     * Checks if an email already exists in the database for a different user.
     * @param email the email to check
     * @param excludeUserId the ID of the user to exclude
     * @return true if the email exists for another user, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private boolean isEmailExistsForOtherUser(String email, Long excludeUserId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(CHECK_EMAIL_TAKEN)) {
                stmt.setString(1, email);
                stmt.setLong(2, excludeUserId);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setAdmin(rs.getBoolean("is_admin"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return user;
    }
}
