package org.service_b.workflow.security.service;

import lombok.RequiredArgsConstructor;
import org.service_b.workflow.shared.service.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailService mailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public void sendVerificationEmail(String email, String username, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        mailService.send(email, "Verify your email address", buildVerificationContent(username, verificationUrl));
    }

    public void sendPasswordResetEmail(String email, String username, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        mailService.send(email, "Reset your password", buildPasswordResetContent(username, resetUrl));
    }

    private String buildVerificationContent(String username, String verificationUrl) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2>Welcome %s!</h2>
                <p>Thank you for registering. Please verify your email address by clicking the link below:</p>
                <p><a href="%s" style="background:#0d6efd;color:#fff;padding:10px 20px;text-decoration:none;border-radius:4px;">Verify Email</a></p>
                <p style="color:#666;font-size:0.9em;">This link will expire in 24 hours.</p>
                <p style="color:#666;font-size:0.9em;">If you didn't create this account, please ignore this email.</p>
            </body>
            </html>
            """, username, verificationUrl);
    }

    private String buildPasswordResetContent(String username, String resetUrl) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2>Password Reset</h2>
                <p>Hi %s,</p>
                <p>You requested a password reset. Click the link below to set a new password:</p>
                <p><a href="%s" style="background:#0d6efd;color:#fff;padding:10px 20px;text-decoration:none;border-radius:4px;">Reset Password</a></p>
                <p style="color:#666;font-size:0.9em;">This link will expire in 1 hour.</p>
                <p style="color:#666;font-size:0.9em;">If you didn't request this, please ignore this email.</p>
            </body>
            </html>
            """, username, resetUrl);
    }
}
