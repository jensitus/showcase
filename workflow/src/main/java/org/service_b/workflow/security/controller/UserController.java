package org.service_b.workflow.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.service_b.workflow.security.dto.ChangePasswordRequest;
import org.service_b.workflow.security.dto.ForgotPasswordRequest;
import org.service_b.workflow.security.dto.LoginRequest;
import org.service_b.workflow.security.dto.LoginResponse;
import org.service_b.workflow.security.dto.ResetPasswordRequest;
import org.service_b.workflow.security.dto.UserRegistrationRequest;
import org.service_b.workflow.security.dto.UserResponse;
import org.service_b.workflow.security.entity.User;
import org.service_b.workflow.security.jwt.JwtUtil;
import org.service_b.workflow.security.repository.UserRepository;
import org.service_b.workflow.security.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Load user details
            final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());

            // Get user entity for additional info
            final User user = userRepository.findByUsername(loginRequest.getUsername())
                                            .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate JWT token
            final String jwt = jwtUtil.generateToken(userDetails);

            // Build response
            LoginResponse response = LoginResponse.builder()
                                                  .token(jwt)
                                                  .type("Bearer")
                                                  .id(user.getId())
                                                  .username(user.getUsername())
                                                  .email(user.getEmail())
                                                  .role(user.getRole())
                                                  .build();

            return ResponseEntity.ok(response);

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(Map.of("error", "Account is disabled. Please verify your email."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "An error occurred during login"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            UserResponse response = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        try {
            userService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                    "message", "Email verified successfully. You can now log in."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam String email) {
        try {
            userService.resendVerificationEmail(email);
            return ResponseEntity.ok(Map.of(
                    "message", "Verification email sent successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "New password and confirmation do not match"
                ));
            }

            String username = authentication.getName();
            userService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());

            return ResponseEntity.ok(Map.of(
                    "message", "Password changed successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a password reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "New password and confirmation do not match"
                ));
            }

            userService.resetPassword(request.getToken(), request.getNewPassword());

            return ResponseEntity.ok(Map.of(
                    "message", "Password has been reset successfully. You can now log in with your new password."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    //  suggestion from frontend AI:


    // Upload avatar
    @PostMapping("/api/users/{userId}/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("avatar") MultipartFile file
    ) {
        // Save file and return URL
        return null;
    }

    // Get avatar
    @GetMapping("/api/users/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        // Return image bytes
        return null;
    }

    // Delete avatar
    @DeleteMapping("/api/users/{userId}/avatar")
    public ResponseEntity<Void> deleteAvatar(@PathVariable Long userId) {
        // Delete avatar file
        return null;
    }


}
