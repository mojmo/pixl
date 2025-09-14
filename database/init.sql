-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS pixl;

-- Use the database
USE pixl;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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

-- replace `password` with your own
CREATE USER 'pixl_user'@'%' IDENTIFIED BY 'password';

FLUSH PRIVILEGES;

-- Only grant privileges if the user already exists (don't try to create user)
GRANT ALL PRIVILEGES ON pixl.* TO 'pixl_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;