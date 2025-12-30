-- H2 compatible schema for testing
-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_email ON users(email);

-- Create artworks table
CREATE TABLE IF NOT EXISTS artworks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled',
    description VARCHAR(1000),
    user_id BIGINT NOT NULL,
    pixel_data CLOB NOT NULL,
    width INT NOT NULL DEFAULT 16,
    height INT NOT NULL DEFAULT 16,
    is_public BOOLEAN DEFAULT false,
    shareable_link VARCHAR(32) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_id ON artworks(user_id);
CREATE INDEX IF NOT EXISTS idx_public ON artworks(is_public);
CREATE INDEX IF NOT EXISTS idx_shareable_link ON artworks(shareable_link);
CREATE INDEX IF NOT EXISTS idx_created_at ON artworks(created_at);
