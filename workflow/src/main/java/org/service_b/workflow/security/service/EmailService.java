package org.service_b.workflow.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Sends verification email to user
     * Note: This is a placeholder implementation.
     * In production, integrate with actual email service (JavaMailSender, SendGrid, AWS SES, etc.)
     */
    public void sendVerificationEmail(String email, String username, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        
        log.info("===========================================");
        log.info("EMAIL VERIFICATION");
        log.info("===========================================");
        log.info("To: {}", email);
        log.info("Subject: Verify your email address");
        log.info("Username: {}", username);
        log.info("Verification Link: {}", verificationUrl);
        log.info("Token expires in 24 hours");
        log.info("===========================================");
        
        // TODO: Integrate with actual email service
        // Example with JavaMailSender:
        // MimeMessage message = mailSender.createMimeMessage();
        // MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // helper.setTo(email);
        // helper.setSubject("Verify your email address");
        // helper.setText(buildEmailContent(username, verificationUrl), true);
        // mailSender.send(message);
    }
    
    /**
     * Sends password reset email to user
     * Note: This is a placeholder implementation.
     * In production, integrate with actual email service (JavaMailSender, SendGrid, AWS SES, etc.)
     */
    public void sendPasswordResetEmail(String email, String username, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        log.info("===========================================");
        log.info("PASSWORD RESET");
        log.info("===========================================");
        log.info("To: {}", email);
        log.info("Subject: Reset your password");
        log.info("Username: {}", username);
        log.info("Reset Link: {}", resetUrl);
        log.info("Token expires in 1 hour");
        log.info("===========================================");

        // TODO: Integrate with actual email service
    }

    private String buildEmailContent(String username, String verificationUrl) {
        return String.format("""
            <html>
            <body>
                <h2>Welcome %s!</h2>
                <p>Thank you for registering. Please verify your email address by clicking the link below:</p>
                <p><a href="%s">Verify Email</a></p>
                <p>This link will expire in 24 hours.</p>
                <p>If you didn't create this account, please ignore this email.</p>
            </body>
            </html>
            """, username, verificationUrl);
    }
}
