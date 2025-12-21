package com.mugtaba.pixl.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database utility class with connection pooling and proper resource management.
 * Uses HikariCP for efficient connection pooling.
 */
public class DatabaseUtil {

    private static volatile HikariDataSource dataSource;
    private static final Object lock = new Object();

    // Default configuration values
    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/pixl";
    private static final String DEFAULT_DB_USER = "pixl_user";
    private static final String DEFAULT_DB_PASSWORD = "password";
    private static final int DEFAULT_MAX_POOL_SIZE = 20; // Max 20 concurrent connections
    private static final int DEFAULT_MIN_IDLE = 5; // Always keep 5 connections ready
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30s to get connection from pool
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // Close unused connections after 10min
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // Recycle connections every 30min

    static {
        initializeDataSource();
    }

    /**
     * Initializes the HikariCP data source with configuration
     */
    private static void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();

            // Database connection settings
            String dbUrl = getEnvOrDefault("DB_URL", DEFAULT_DB_URL);
            String dbUser = getEnvOrDefault("DB_USER", DEFAULT_DB_USER);
            String dbPassword = getEnvOrDefault("DB_PASSWORD", DEFAULT_DB_PASSWORD);

            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Connection pool settings
            config.setMaximumPoolSize(getEnvAsInt("DB_MAX_POOL_SIZE", DEFAULT_MAX_POOL_SIZE));
            config.setMinimumIdle(getEnvAsInt("DB_MIN_IDLE", DEFAULT_MIN_IDLE));
            config.setConnectionTimeout(getEnvAsLong("DB_CONNECTION_TIMEOUT", DEFAULT_CONNECTION_TIMEOUT));
            config.setIdleTimeout(getEnvAsLong("DB_IDLE_TIMEOUT", DEFAULT_IDLE_TIMEOUT));
            config.setMaxLifetime(getEnvAsLong("DB_MAX_LIFETIME", DEFAULT_MAX_LIFETIME));

            // Performance and reliability settings
            config.setLeakDetectionThreshold(60000); // 1 minute
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000); // 5 seconds

            // MySQL specific optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            // Set pool name for monitoring
            config.setPoolName("PixlCP");

            dataSource = new HikariDataSource(config);

            // Test the connection
            testConnection();

            System.out.println("Database connection pool initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize database connection pool: " + e.getMessage());
            throw new RuntimeException("Database initialization error", e);
        }
    }

    /**
     * Gets a connection from the pool
     * 
     * @return a valid database connection
     * @throws SQLException if unable to get a connection
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            synchronized (lock) {
                if (dataSource == null) {
                    initializeDataSource();
                }
            }
        }

        try {
            Connection conn = dataSource.getConnection();
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Failed to obtain a valid database connection");
            }
            return conn;
        } catch (SQLException e) {
            System.err.println("Error obtaining database connection: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Tests the database connection
     * @throws SQLException if unable to connect to the database
     */
    private static void testConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (conn == null || !conn.isValid(5)) {
                throw new SQLException("Database connection test failed");
            }
        }
    }

    /**
     * Utility method to get environment variable with default
     * @param key the environment variable key
     * @param defaultValue the default value if the environment key is not set or invalid
     * @return the environment variable value or default
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    /**
     * Utility method to get environment variable as integer
     * @param key the environment variable key
     * @param defaultValue the default value if the environment key is not set or invalid
     * @return the environment variable value as integer or default
     */
    private static int getEnvAsInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Utility method to get environment variable as long
     * @param key the environment variable key
     * @param defaultValue the default value if the environment key is not set or invalid
     * @return the environment variable value as long or default
     */
    private static long getEnvAsLong(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid long value for " + key + ": " + value + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
}