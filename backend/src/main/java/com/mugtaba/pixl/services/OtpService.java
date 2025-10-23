package com.mugtaba.pixl.services;

import com.mugtaba.pixl.util.CacheUtil;
import com.mugtaba.pixl.util.LogUtil;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for generating and verifying OTPs.
 */
public class OtpService {
    
    private static final String COMPONENT_NAME = "OtpService";

    // OTP Configuration
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final String OTP_CHARACTERS = "0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Cache keys
    private static final String CACHE_OTP_KEY = "otp_%s_%s"; // Format: otp_{username}_{type}
    private static final String CACHE_ATTEMPT_KEY = "otp_attempts_%s_%s"; // Format: otp_attempt_{email}_{type}

    // Rate limiting
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int MAX_OTP_GENERATION_PER_HOUR = 10;

    /**
     * Generates an OTP for the given email and type.
     * @param email The email address to generate the OTP for.
     * @param type the type of OTP to generate (see OtpType enum).
     * @return The generated OTP code.
     */
    public String generateOtp(String email, OtpType type) {

        // Check rate limiting
        if (isRateLimited(email, type)) {
            LogUtil.logWarning(
                COMPONENT_NAME, "generateOtp",
                String.format("Rate limit exceeded for email: %s, type: %s", email, type.getValue())
            );

            return null;
        }

        // Generate OTP
        String otp = generateSecureOtp();

        // Store OTP in cache
        String otpCacheKey = String.format(CACHE_OTP_KEY, email, type.getValue());
        OtpData otpData = new OtpData(otp, LocalDateTime.now(), 0);
        CacheUtil.put(otpCacheKey, otpData, OTP_EXPIRY_MINUTES);

        // Update generation count for rate limiting
        updateGenerationCount(email, type);

        LogUtil.logInfo(
            COMPONENT_NAME, "generateOtp",
            String.format("OTP generated for email: %s, type: %s", email, type.getValue())
        );

        return otp;
    }

    /**
     * Validates the provided OTP for the specified email and type.
     * @param email user's email address
     * @param type OTP type (see OtpType enum)
     * @param providedOtp OTP code provided by the user
     * @return OtpValidationResult indicating the result of the validation
     *         (VALID, INVALID, NOT_FOUND, EXPIRED, MAX_ATTEMPTS_EXCEEDED)
     */
    public OtpValidationResult validateOtp(String email, OtpType type, String providedOtp) {
        String otpCacheKey = String.format(CACHE_OTP_KEY, email, type.getValue());

        // get OTP data from cache
        OtpData otpData = CacheUtil.get(otpCacheKey, OtpData.class);

        if (otpData == null) {
            LogUtil.logInfo(
                COMPONENT_NAME, "validateOtp",
                String.format("No OTP found for email: %s, type: %s", email, type.getValue())
            );

            return OtpValidationResult.NOT_FOUND;
        }

        // Check if too many attempts have been made
        if (otpData.getAttempts() >= MAX_OTP_ATTEMPTS) {
            LogUtil.logWarning(
                COMPONENT_NAME, "validateOtp",
                String.format("Max attempts exceeded for email: %s, type: %s", email, type.getValue())
            );

            CacheUtil.remove(otpCacheKey); // remove to prevent further attempts
            return OtpValidationResult.MAX_ATTEMPTS_EXCEEDED;
        }

        // Increment attempt count
        otpData.incrementAttempts();
        CacheUtil.put(otpCacheKey, otpData, OTP_EXPIRY_MINUTES);

        // Validate OTP
        if (providedOtp != null && providedOtp.equals(otpData.getCode())) {

            LogUtil.logInfo(
                COMPONENT_NAME, "validateOtp",
                String.format("OTP validated successfully for email: %s, type: %s", email, type.getValue())
            );

            return OtpValidationResult.VALID;
        } else {
            LogUtil.logWarning(
                COMPONENT_NAME, "validateOtp",
                String.format(
                    "Invalid OTP provided for email: %s, type: %s (attempt %d/%d)",
                    email, type.getValue(), otpData.getAttempts(), MAX_OTP_ATTEMPTS
                )
            );

            return OtpValidationResult.INVALID;
        }
    }

    /**
     * Revokes (deletes) OTP for the given email and type
     * @param email user's email address
     * @param type OTP type
     */
    public void revokeOtp(String email, OtpType type) {
        String otpCacheKey = String.format(CACHE_OTP_KEY, email, type.getValue());

        CacheUtil.remove(otpCacheKey);

        LogUtil.logInfo(
            COMPONENT_NAME, "revokeOtp",
            String.format("OTP revoked for email: %s, type: %s", email, type.getValue()
        ));
    }

    /**
     * Gets the remaining time in minutes before the OTP expires.
     * @param email user's email address
     * @param type the OTP type (see OtpType enum)
     * @return remaining minutes, or -1 if no OTP found
     */
    public int getOtpRemainingMinutes(String email, OtpType type) {
        String otpCacheKey = String.format(CACHE_OTP_KEY, email, type.getValue());

        OtpData otpData = CacheUtil.get(otpCacheKey, OtpData.class);
        if (otpData == null) {
            return -1; // No OTP found
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = otpData.getGeneratedAt().plusMinutes(OTP_EXPIRY_MINUTES);

        if (now.isAfter(expiry)) {
            return 0; // OTP expired
        }

        return (int) Duration.between(now, expiry).toMinutes();
    }

    /**
     * Generates a secure random OTP code.
     * @return the OTP code
     */
    private String generateSecureOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);

        for (int i = 0; i < OTP_LENGTH; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(OTP_CHARACTERS.length());
            otp.append(OTP_CHARACTERS.charAt(randomIndex));
        }

        return otp.toString();
    }

    /**
     * Checks if the email is rate limited for OTP generation.
     * @param email the email address
     * @param type the OTP type (see OtpType enum)
     * @return true if rate limited, false otherwise
     */
    private boolean isRateLimited(String email, OtpType type) {
        String attemptCacheKey = String.format(CACHE_ATTEMPT_KEY, email, type.getValue());
        Integer generationCount = CacheUtil.get(attemptCacheKey, Integer.class);

        return generationCount != null && generationCount >= MAX_OTP_GENERATION_PER_HOUR;
    }

    /**
     * Updates the OTP generation count for the specified email and type for rate limiting.
     * @param email the email address
     * @param type the OTP type (see OtpType enum)
     */
    private void updateGenerationCount(String email, OtpType type) {
        String attemptCacheKey = String.format(CACHE_ATTEMPT_KEY, email, type.getValue());
        Integer currentCount = CacheUtil.get(attemptCacheKey, Integer.class);
        int newCount = (currentCount != null) ? currentCount + 1 : 1;
        CacheUtil.put(attemptCacheKey, newCount, 60); // 1 hour TTL
    }

    /**
     * OTP types for different purposes.
     */
    public enum OtpType {
        PASSWORD_RESET("password_reset"),
        ACCOUNT_VERIFICATION("account_verification");

        private final String value;

        OtpType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * OTP validation results.
     */
    public enum OtpValidationResult {
        VALID, INVALID, NOT_FOUND, EXPIRED, MAX_ATTEMPTS_EXCEEDED
    }

    /**
     * OTP data class for cache storage.
     */
    public static class OtpData {

        private String code;
        private LocalDateTime generatedAt;
        private int attempts;

        public OtpData() {} // Default constructor for JSON deserialization

        public OtpData(String code, LocalDateTime generatedAt, int attempts) {
            this.code = code;
            this.generatedAt = generatedAt;
            this.attempts = attempts;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }

        public void incrementAttempts() { this.attempts++; }
    }
}
