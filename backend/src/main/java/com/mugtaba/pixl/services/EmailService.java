package com.mugtaba.pixl.services;

import com.mugtaba.pixl.util.LogUtil;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Service for sending emails, such as password reset OTPs.
 */
public class EmailService {
    private static final String COMPONENT_NAME = "EmailService";

    // SMTP Configurations
    private static final String SMTP_HOST = System.getenv("SMTP_HOST");
    private static final String SMTP_PORT = System.getenv("SMTP_PORT");
    private static final String SMTP_USERNAME = System.getenv("SMTP_USERNAME");
    private static final String SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");
    private static final String FROM_EMAIL = System.getenv("FROM_EMAIL");
    private static final String FROM_NAME = System.getenv("FROM_NAME");

    private Session session;

    public EmailService() {
        initializeSession();
    }

    /**
     * Initializes the SMTP session for sending emails.
     */
    private void initializeSession() {
        Properties props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        LogUtil.logInfo(
            COMPONENT_NAME, "initializeSession",
            String.format("SMTP session initialized for host: %s", SMTP_HOST)
        );
    }

    /**
     * Sends a password reset OTP email to the specified email address.
     * @param toEmail email address to send the OTP
     * @param username username to send the OTP
     * @param otp OTP code to send
     * @param expiryMinutes number of minutes before the OTP expires
     * @return true if the email was sent successfully, false otherwise
     */
    public boolean sendPasswordResetOTP(String toEmail, String username, String otp, int expiryMinutes) {
        if (isConfiguredEmail()) {
            LogUtil.logError(
                COMPONENT_NAME, "sendPasswordResetOTP",
                "Email service not configured - missing SMTP credentials", null
            );
            return false;
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Pixl - Password Reset Code");

            String emailBody = buildPasswordResetEmailBody(username, otp, expiryMinutes);

            message.setContent(emailBody, "text/html; charset=utf-8");

            Transport.send(message);

            LogUtil.logInfo(
                COMPONENT_NAME, "sendPasswordResetOTP",
                String.format("Password reset OTP sent to: %s", username)
            );

            return true;
        } catch (Exception e) {
            LogUtil.logError(
                COMPONENT_NAME, "sendPasswordResetOTP",
                String.format("Failed to send OTP email to: %s", toEmail), e
            );
            return false;
        }
    }

    /**
     * Checks if email service is configured
     * @return true if email service is configured, false otherwise
     */
    public boolean isConfiguredEmail() {
        return SMTP_USERNAME == null || SMTP_USERNAME.trim().isEmpty() ||
                SMTP_PASSWORD == null || SMTP_PASSWORD.trim().isEmpty() ||
                SMTP_HOST == null || SMTP_HOST.trim().isEmpty();
    }

    /**
     * Builds the email body for a password reset email.
     * @param username username of the user
     * @param otp OTP code for password reset
     * @param expiryMinutes number of minutes before the OTP expires
     */
    private String buildPasswordResetEmailBody(String username, String otp, int expiryMinutes) {
        // TODO: Consider adjusting the template
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Pixl - Password Reset</title>
                <style>
                    .container {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background-color: #4A90E2;
                        color: white;
                        padding: 10px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }
                    .content {
                        background-color: #f9f9f9;
                        padding: 20px;
                        border-radius: 0 0 5px 5px;
                    }
                    .otp-code {
                        background-color: #fff;
                        border: 2px solid #4A90E2;
                        padding: 15px;
                        text-align: center;
                        font-size: 24px;
                        font-weight: bold;
                        color: #4A90E2;
                        margin: 20px 0;
                        border-radius: 5px;
                    }
                    .warning {
                        color: #e74c3c;
                        font-weight: bold;
                    }
                    .footer {
                        margin-top: 20px;
                        font-size: 12px;
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎨 Pixl Password Reset</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>We received a request to reset your password. Use the verification code below to reset your password:</p>
                        <div class="otp-code">%s</div>
                        <p><strong>This code will expire in %d minutes.</strong></p>
                        <p>If you didn't request a password reset, please ignore this email. Your password will not be changed.</p>
                        <div class="warning">
                            <p>⚠️ For your security:</p>
                            <ul>
                                <li>Never share this code with anyone.</li>
                                <li>Pixl will never ask for this code via phone or email.</li>
                                <li>This code can only be used once.</li>
                            </ul>
                        </div>

                        <div class="footer">
                            <p>Best regards,<br/>The Pixl Team</p>
                            <p>This is an automated message, please do not reply to this email.</p>
                        </div>
                    </div>
                </div>
            </body>
            """, username, otp, expiryMinutes);
    }
}
