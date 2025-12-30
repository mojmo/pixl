package com.mugtaba.pixl.integration;

import com.mugtaba.pixl.services.UserService;
import com.mugtaba.pixl.services.ArtworkService;
import com.mugtaba.pixl.util.CacheUtil;
import com.mugtaba.pixl.util.DatabaseUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Base class for H2 database integration tests.
 * Provides setup and teardown for H2 in-memory database.
 * Configures DatabaseUtil to use H2 and provides service instances.
 */
@DisplayName("H2 Database Integration Test Base")
public abstract class H2IntegrationTestBase {

    protected static HikariDataSource dataSource;
    protected static UserService userService;
    protected static ArtworkService artworkService;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        // configure H2 in-memory database
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTestQuery("SELECT 1");
        
        dataSource = new HikariDataSource(config);

        // configure DatabaseUtil to use our test datasource
        DatabaseUtil.setTestDataSource(dataSource);

        // initialize schema
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String schema = loadSchemaFromResource();
            
            // execute schema creation
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }

        // initialize services - they will now use our H2 database
        userService = new UserService();
        artworkService = new ArtworkService();
    }

    @AfterAll
    static void tearDownDatabase() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        // reset DatabaseUtil to default configuration
        DatabaseUtil.resetDataSource();
    }

    @BeforeEach
    void cleanDatabase() throws Exception {
        // clean all tables before each test
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("TRUNCATE TABLE artworks");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
        
        // clear all caches between tests to ensure test isolation
        CacheUtil.clear();
    }

    protected static Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }

    private static String loadSchemaFromResource() throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        H2IntegrationTestBase.class.getClassLoader()
                                .getResourceAsStream("test-schema.sql")))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Test
    @DisplayName("database_WhenSetUp_IsAccessible")
    void database_WhenSetUp_IsAccessible() throws Exception {
        // Act
        try (Connection conn = getConnection()) {
            // Assert
            assertThat(conn).isNotNull();
            assertThat(conn.isClosed()).isFalse();
        }
    }
}
