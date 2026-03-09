package org.service_b.workflow.security.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.service_b.workflow.security.dto.UserRegistrationRequest;
import org.service_b.workflow.security.dto.UserResponse;
import org.service_b.workflow.security.entity.EmailVerificationToken;
import org.service_b.workflow.security.entity.PasswordResetToken;
import org.service_b.workflow.security.entity.User;
import org.service_b.workflow.security.repository.EmailVerificationTokenRepository;
import org.service_b.workflow.security.repository.PasswordResetTokenRepository;
import org.service_b.workflow.security.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create user (disabled until email is verified)
        User user = User.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .enabled(false) // User is disabled until email is verified
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .role("USER")
                        .build();

        user = userRepository.save(user);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                                                                         .token(token)
                                                                         .user(user)
                                                                         .build();

        tokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);

        return mapToResponse(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                                                                  .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (verificationToken.isVerified()) {
            throw new IllegalArgumentException("Email already verified");
        }

        if (verificationToken.isExpired()) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        // Mark token as verified
        verificationToken.setVerifiedAt(java.time.LocalDateTime.now());
        tokenRepository.save(verificationToken);

        // Enable user account
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                                  .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("Email already verified");
        }

        // Delete old tokens
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        // Generate new token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                                                                         .token(token)
                                                                         .user(user)
                                                                         .build();

        tokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                                  .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                                  .orElse(null);

        // Don't reveal if user exists - always return success message
        if (user == null) {
            return;
        }

        // Delete any existing reset tokens for this user
        passwordResetTokenRepository.findByUser(user)
                                    .ifPresent(passwordResetTokenRepository::delete);

        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                                                          .token(token)
                                                          .user(user)
                                                          .build();

        passwordResetTokenRepository.save(resetToken);

        // Send password reset email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                                                                    .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        // Mark token as used
        resetToken.setUsedAt(java.time.LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                           .id(user.getId())
                           .username(user.getUsername())
                           .email(user.getEmail())
                           .enabled(user.isEnabled())
                           .role(user.getRole())
                           .createdAt(user.getCreatedAt())
                           .build();
    }
}
