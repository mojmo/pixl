package com.mugtaba.pixl.servlets;

import com.mugtaba.pixl.exceptions.*;
import com.mugtaba.pixl.services.UserService;
import com.mugtaba.pixl.services.OtpService.OtpValidationResult;
import com.mugtaba.pixl.util.LogUtil;
import com.mugtaba.pixl.util.PasswordUtil;
import com.mugtaba.pixl.util.ValidationUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/password-reset/*")
public class PasswordResetServlet extends BaseServlet {

    private static final String COMPONENT_NAME = "PasswordResetServlet";
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
        LogUtil.logInfo(
            COMPONENT_NAME, "init",
            "Password servlet initialized successfully"
        );
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        try {
            switch (pathInfo) {
                case "/initiate" -> handleInitiateReset(request, response);
                case "/verify-otp" -> handleVerifyOtp(request, response);
                case "/complete" -> handleCompleteReset(request, response);
                default -> throw new ResourceNotFoundException("Password reset endpoint");
            }
        } catch (PixlException e) {
            LogUtil.logError(COMPONENT_NAME, "doPost", e.getLogMessage(), e);
            sendErrorResponse(response, e.getStatusCode(), e.getUserMessage());
        } catch (Exception e) {
            LogUtil.logError(
                COMPONENT_NAME, "doPost",
                "Unexpected error in password reset", e
            );
            sendErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to process password reset request. Please try again later."
            );
        }
    }

    /**
     * Handles password reset initiation requests.
     * Initiates a password reset process for the specified email or username.
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws PixlException if an error occurs during request processing
     * @throws SQLException if a database error occurs
     * @throws IOException if an I/O error occurs
     */
    private void handleInitiateReset(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, SQLException, IOException {

        Map<String, String> requestData = extractRequestData(request);
        String emailOrUsername = requestData.get("emailOrUsername").trim();

        ValidationUtil.validateStringNotEmpty(emailOrUsername, "Email or username");
        ValidationUtil.validateStringLength(emailOrUsername, "Email or username", 3, 100);

        boolean initiated = userService.initiatePasswordReset(emailOrUsername);

        if (initiated) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put(
                "message",
                "If an account with that email exists, you will receive a password reset code shortly"
            );

            LogUtil.logInfo(
                COMPONENT_NAME, "handleInitiateReset",
                String.format("Password reset initiated for: %s", emailOrUsername)
            );

            sendSuccessResponse(response, "Password reset initiated", responseData);
        } else {
            throw new DatabaseException(
                "Password reset initiation",
                new Exception("Failed to initiate password reset")
            );
        }
    }

    /**
     * Handles OTP verification requests.
     * Verifies the provided OTP code for the specified email.
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws PixlException if an error occurs during request processing
     * @throws IOException if an I/O error occurs
     */
    private void handleVerifyOtp(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, IOException {

        Map<String, String> requestData = extractRequestData(request);
        String email = requestData.get("email").trim().toLowerCase();
        String otp = requestData.get("otp").trim();
        String emailRegex = "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";

        ValidationUtil.validateStringNotEmpty(email, "Email");
        ValidationUtil.validateStringNotEmpty(otp, "OTP code");

        // Validate email format
        if (!email.matches(emailRegex)) {
            throw new ValidationException("Invalid email format");
        }

        // Validate OTP format
        if (!otp.matches("^\\d{6}$")) {
            throw new ValidationException("OTP must be a 6-digit number");
        }

        OtpValidationResult result = userService.verifyPasswordResetOtp(email, otp);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("valid", result == OtpValidationResult.VALID);

        switch (result) {
            case VALID -> {
                requestData.put("message", "OTP verified successfully. You can now reset your password.");
                LogUtil.logInfo(
                    COMPONENT_NAME, "handleVerifyOtp",
                    String.format("OTP verified successfully for: %s", email)
                );
                sendSuccessResponse(response, "OTP verified", responseData);
            }

            case INVALID -> throw new ValidationException("Invalid OTP code. Please check and try again.");

            case NOT_FOUND -> throw new ValidationException("OTP not found or expired. Please request a new one.");

            case MAX_ATTEMPTS_EXCEEDED -> throw new ValidationException("Too many incorrect attempts. Please request a new OTP.");
        
            default -> throw new ValidationException("OTP verification failed. Please try again.");
        }
    }

    /**
     * Handles password reset completion requests.
     * Resets the user's password using the provided email, OTP, and new password.
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws PixlException if an error occurs during request processing
     * @throws SQLException if a database error occurs
     * @throws IOException if an I/O error occurs
     */
    private void handleCompleteReset(HttpServletRequest request, HttpServletResponse response)
        throws PixlException, SQLException, IOException {

        Map<String, String> requestData = extractRequestData(request);
        String email = requestData.get("email").trim().toLowerCase();
        String otp = requestData.get("otp").trim();
        String newPassword = requestData.get("newPassword");
        String emailRegex = "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";

        ValidationUtil.validateStringNotEmpty(email, "Email");
        ValidationUtil.validateStringNotEmpty(otp, "OTP code");
        ValidationUtil.validateStringNotEmpty(newPassword, "New password");

        if (!email.matches(emailRegex)) {
            throw new ValidationException("Invalid email format");
        }

        if (!otp.matches("^\\d{6}$")) {
            throw new ValidationException("OTP must be a 6-digit number");
        }

        ValidationUtil.validateStringLength(newPassword, "New password", 8, 128);
        if (PasswordUtil.isWeakPassword(newPassword)) {
            throw new ValidationException(
                "Password must contain at least 3 of: uppercase letter, lowercase letter, number, special character"
            );
        }

        boolean completed = userService.completePasswordReset(email, otp, newPassword);

        if (completed) {
            LogUtil.logInfo(
                COMPONENT_NAME, "handleCompleteReset",
                String.format("Password reset completed for: %s", email)
            );
            sendSuccessResponse(
                response,
                "Password reset successful. You can now login with your new password.",
                null
            );
        } else {
            throw new ValidationException(
                "Failed to reset password. Please verify your OTP and try again."
            );
        }
    }

}
