package org.service_b.workflow.security.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.from:info@service-b.org}")
    private String fromAddress;

    public void sendVerificationEmail(String email, String username, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verify your email address";
        String content = buildVerificationContent(username, verificationUrl);
        send(email, subject, content);
    }

    public void sendPasswordResetEmail(String email, String username, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your password";
        String content = buildPasswordResetContent(username, resetUrl);
        send(email, subject, content);
    }

    private void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
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
