package com.mugtaba.pixl.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for secure password handling including hashing, salt generation,
 * and verification. Uses SHA-256 hashing algorithm with salt for enhanced security.
 */
public class PasswordUtil {

    /**
     * Hashes a password using SHA-256 algorithm with a provided salt.
     * The method combines the salt and password, computes the hash, and returns
     * a Base64 encoded string representation of the hashed result.
     *
     * @param password the plain text password to be hashed
     * @param salt the salt value to be used in the hashing process
     * @return Base64 encoded string representation of the hashed password
     * @throws RuntimeException if the SHA-256 algorithm is not available
     */
    public static String hashedPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Generates a cryptographically secure random salt value.
     * Uses SecureRandom to generate 16 random bytes and returns
     * a Base64 encoded string representation of the salt.
     *
     * @return Base64 encoded string representation of the generated salt
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Verifies if a plain text password matches the stored hashed password
     * when using the same salt. This is done by hashing the provided password
     * with the given salt and comparing it to the stored hash.
     *
     * @param password the plain text password to verify
     * @param hashedPassword the previously hashed password to compare against
     * @param salt the salt that was used to create the original hash
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String password, String hashedPassword, String salt) {
        return hashedPassword(password, salt).equals(hashedPassword);
    }
}
