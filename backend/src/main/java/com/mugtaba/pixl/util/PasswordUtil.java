package com.mugtaba.pixl.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Utility class for secure password handling including hashing, salt generation,
 * and verification. Uses SHA-256 hashing algorithm with salt for enhanced security.
 */
public class PasswordUtil {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

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
    public static String hashPassword(String password, String salt) {

        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }

        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);

            // Add salt to password before hashing
            String saltedPassword = salt + password;

            // Hash the salted password
            byte[] hashedBytes = md.digest(saltedPassword.getBytes());

            // Convert the byte array to a Base64 encoded string
            return Base64.getEncoder().encodeToString(hashedBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    /**
     * Generates a cryptographically secure random salt value.
     * Uses SecureRandom to generate random bytes and returns
     * a Base64 encoded string representation of the salt.
     *
     * @return Base64 encoded string representation of the generated salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Verifies if a plain text password matches the stored hashed password
     * when using the same salt. This is done by hashing the provided password
     * with the given salt and comparing it to the stored hash in a constant
     * time comparison to prevent timing attacks
     *
     * @param password the plain text password to verify
     * @param hash the previously hashed password to compare against
     * @param salt the salt that was used to create the original hash
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String password, String hash, String salt) {
        if (password == null || hash == null || salt == null) {
            return false;
        }

        try {
            String hashedPassword = hashPassword(password, salt);

            // Use constant time comparison to prevent timing attacks
            return constantTimeEquals(hash, hashedPassword);
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Compares two strings in constant time to prevent timing attacks
     * during password verification
     * 
     * @param a the first string to compare
     * @param b the second string to compare
     * @return true if both strings are equal, false otherwise
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            //
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * Checks if a password is strong based on defined criteria:
     * - Minimum length of 8 characters
     * - Contains at least three of the following character types:
     *   - Uppercase letters
     *   - Lowercase letters
     *   - Digits
     *   - Special characters (e.g., !@#$%^&*()-+<>?)
     *
     * @param password the password to evaluate
     * @return true if the password is considered strong, false otherwise
     */
    public static boolean isWeakPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if ("!@#$%^&*()-+<>?".indexOf(c) != -1) {
                hasSpecialChar = true;
            }
        }

        // Require at least 3 of the  character types
        int typeCount = 0;
        if (hasUpperCase) typeCount++;
        if (hasLowerCase) typeCount++;
        if (hasDigit) typeCount++;
        if (hasSpecialChar) typeCount++;

        return typeCount < 3;
    }

    /**
     * Generates a random strong password of specified length.
     * The generated password will contain at least one uppercase letter,
     * one lowercase letter, one digit, and one special character.
     *
     * @param length the desired length of the password (minimum 8)
     * @return the generated strong password
     * @throws IllegalArgumentException if the specified length is less than 8
     */
    public static String generateRandomPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length should be at least 8 characters");
        }

        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        String allChars = upperCase + lowerCase + digits + specialChars;

        StringBuilder password = new StringBuilder();

        // Ensure the password contains at least one character from each category
        password.append(upperCase.charAt(secureRandom.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(secureRandom.nextInt(lowerCase.length())));
        password.append(digits.charAt(secureRandom.nextInt(digits.length())));
        password.append(specialChars.charAt(secureRandom.nextInt(specialChars.length())));

        // Fill the remaining positions with random characters from all categories
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }

        // Shuffle the password to ensure randomness
        return shuffleString(password.toString());
    }

    /**
     * Shuffles the characters in a string to ensure randomness.
     * 
     * @param input the string to shuffle
     * @return the shuffled string
     */
    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();

        for (int i = characters.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            // Swap characters at positions i and j
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }

        return new String(characters);
    }
}
