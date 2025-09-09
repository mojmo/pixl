-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS pixl;

-- Use the database
USE pixl;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create artworks table
CREATE TABLE IF NOT EXISTS artworks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    pixels JSON NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Only grant privileges if the user already exists (don't try to create user)
GRANT ALL PRIVILEGES ON pixl.* TO 'pixl_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;