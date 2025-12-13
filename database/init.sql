-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS pixl;

-- Use the database
USE pixl;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Create artworks table
CREATE TABLE IF NOT EXISTS artworks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled',
    description TEXT,
    user_id BIGINT NOT NULL,
    pixel_data LONGTEXT NOT NULL,
    width INT NOT NULL DEFAULT 16,
    height INT NOT NULL DEFAULT 16,
    is_public BOOLEAN DEFAULT false,
    shareable_link VARCHAR(32) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_public (is_public),
    INDEX idx_shareable_link (shareable_link),
    INDEX idx_created_at (created_at)
);

-- replace `password` with your own
CREATE USER 'pixl_user'@'%' IDENTIFIED BY 'password';

FLUSH PRIVILEGES;

-- Only grant privileges if the user already exists (don't try to create user)
GRANT ALL PRIVILEGES ON pixl.* TO 'pixl_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;