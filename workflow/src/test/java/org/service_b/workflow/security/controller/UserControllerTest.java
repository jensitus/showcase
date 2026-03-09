package org.service_b.workflow.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.service_b.workflow.security.dto.ChangePasswordRequest;
import org.service_b.workflow.security.dto.ForgotPasswordRequest;
import org.service_b.workflow.security.dto.ResetPasswordRequest;
import org.service_b.workflow.security.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private StubUserService userService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userService = new StubUserService();

        UserController controller = new UserController(
                userService,
                null,
                null,
                null,
                null
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("changePassword endpoint tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("should return 200 when password changed successfully")
        void changePassword_Success() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword123")
                    .newPassword("newPassword456")
                    .confirmPassword("newPassword456")
                    .build();

            mockMvc.perform(post("/api/auth/change-password")
                            .principal(createAuthentication("testuser"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("should return 400 when passwords do not match")
        void changePassword_PasswordMismatch() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword123")
                    .newPassword("newPassword456")
                    .confirmPassword("differentPassword")
                    .build();

            mockMvc.perform(post("/api/auth/change-password")
                            .principal(createAuthentication("testuser"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("New password and confirmation do not match"));
        }

        @Test
        @DisplayName("should return 400 when current password is incorrect")
        void changePassword_IncorrectCurrentPassword() throws Exception {
            userService.setChangePasswordException("Current password is incorrect");

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("wrongPassword")
                    .newPassword("newPassword456")
                    .confirmPassword("newPassword456")
                    .build();

            mockMvc.perform(post("/api/auth/change-password")
                            .principal(createAuthentication("testuser"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("should return 400 when user not found")
        void changePassword_UserNotFound() throws Exception {
            userService.setChangePasswordException("User not found");

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword123")
                    .newPassword("newPassword456")
                    .confirmPassword("newPassword456")
                    .build();

            mockMvc.perform(post("/api/auth/change-password")
                            .principal(createAuthentication("nonexistent"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    @Nested
    @DisplayName("forgotPassword endpoint tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should return 200 with generic message regardless of email existence")
        void forgotPassword_Success() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("user@example.com")
                    .build();

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));
        }

        @Test
        @DisplayName("should return 200 even when user does not exist")
        void forgotPassword_UserNotFound_StillReturnsSuccess() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("nonexistent@example.com")
                    .build();

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));
        }
    }

    @Nested
    @DisplayName("resetPassword endpoint tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("should return 200 when password reset successfully")
        void resetPassword_Success() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("newPassword456")
                    .confirmPassword("newPassword456")
                    .build();

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password has been reset successfully. You can now log in with your new password."));
        }

        @Test
        @DisplayName("should return 400 when passwords do not match")
        void resetPassword_PasswordMismatch() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("newPassword456")
                    .confirmPassword("differentPassword")
                    .build();

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("New password and confirmation do not match"));
        }

        @Test
        @DisplayName("should return 400 when token is invalid")
        void resetPassword_InvalidToken() throws Exception {
            userService.setResetPasswordException("Invalid or expired reset token");

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("invalid-token")
                    .newPassword("newPassword456")
                    .confirmPassword("newPassword456")
                    .build();

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid or expired reset token"));
        }

        @Test
        @DisplayName("should return 400 when token is expired")
        void resetPassword_TokenExpired() throws Exception {
            userService.setResetPasswordException("Reset token has expired");

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("expired-token")
                    .newPassword("newPassword456")
                    .confirmPassword("newPassword456")
                    .build();

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Reset token has expired"));
        }

        @Test
        @DisplayName("should return 400 when token is already used")
        void resetPassword_TokenAlreadyUsed() throws Exception {
            userService.setResetPasswordException("Reset token has already been used");

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("used-token")
                    .newPassword("newPassword456")
                    .confirmPassword("newPassword456")
                    .build();

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Reset token has already been used"));
        }
    }

    private Authentication createAuthentication(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private static class StubUserService extends UserService {
        private String changePasswordException;
        private String resetPasswordException;

        public StubUserService() {
            super(null, null, null, null, null);
        }

        public void setChangePasswordException(String message) {
            this.changePasswordException = message;
        }

        public void setResetPasswordException(String message) {
            this.resetPasswordException = message;
        }

        @Override
        public void changePassword(String username, String currentPassword, String newPassword) {
            if (changePasswordException != null) {
                throw new IllegalArgumentException(changePasswordException);
            }
        }

        @Override
        public void requestPasswordReset(String email) {
            // Always succeeds silently
        }

        @Override
        public void resetPassword(String token, String newPassword) {
            if (resetPasswordException != null) {
                throw new IllegalArgumentException(resetPasswordException);
            }
        }
    }
}
