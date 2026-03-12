package org.service_b.workflow.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.service_b.workflow.security.entity.PasswordResetToken;
import org.service_b.workflow.security.entity.User;
import org.service_b.workflow.security.repository.PasswordResetTokenRepository;
import org.service_b.workflow.security.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    private UserService userService;
    private StubUserRepository userRepository;
    private StubPasswordResetTokenRepository passwordResetTokenRepository;
    private StubEmailService emailService;
    private PasswordEncoder passwordEncoder;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository = new StubUserRepository();
        passwordResetTokenRepository = new StubPasswordResetTokenRepository();
        emailService = new StubEmailService();
        passwordEncoder = new BCryptPasswordEncoder();

        userService = new UserService(userRepository, null, passwordResetTokenRepository, passwordEncoder, emailService);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("oldPassword123"))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .role("USER")
                .build();

        userRepository.setUser(testUser);
    }

    @Nested
    @DisplayName("changePassword tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("should update password when current password is correct")
        void changePassword_Success() {
            userService.changePassword("testuser", "oldPassword123", "newPassword456");

            User savedUser = userRepository.getSavedUser();
            assertThat(savedUser).isNotNull();
            assertThat(passwordEncoder.matches("newPassword456", savedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void changePassword_UserNotFound() {
            userRepository.setUser(null);

            assertThatThrownBy(() -> userService.changePassword("nonexistent", "old", "new"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");

            assertThat(userRepository.getSavedUser()).isNull();
        }

        @Test
        @DisplayName("should throw exception when current password is incorrect")
        void changePassword_IncorrectCurrentPassword() {
            assertThatThrownBy(() -> userService.changePassword("testuser", "wrongPassword", "newPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Current password is incorrect");

            assertThat(userRepository.getSavedUser()).isNull();
        }

        @Test
        @DisplayName("should encode the new password before saving")
        void changePassword_EncodesNewPassword() {
            String newPassword = "myNewSecurePassword";

            userService.changePassword("testuser", "oldPassword123", newPassword);

            User savedUser = userRepository.getSavedUser();
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getPassword()).isNotEqualTo(newPassword);
            assertThat(passwordEncoder.matches(newPassword, savedUser.getPassword())).isTrue();
        }
    }

    @Nested
    @DisplayName("requestPasswordReset tests")
    class RequestPasswordResetTests {

        @Test
        @DisplayName("should create token and send email when user exists")
        void requestPasswordReset_Success() {
            userRepository.setUserByEmail(testUser);

            userService.requestPasswordReset("test@example.com");

            assertThat(passwordResetTokenRepository.getSavedToken()).isNotNull();
            assertThat(passwordResetTokenRepository.getSavedToken().getUser()).isEqualTo(testUser);
            assertThat(emailService.wasPasswordResetEmailSent()).isTrue();
        }

        @Test
        @DisplayName("should not throw exception when user does not exist")
        void requestPasswordReset_UserNotFound_NoException() {
            // This should not throw - we don't want to reveal if email exists
            userService.requestPasswordReset("nonexistent@example.com");

            assertThat(passwordResetTokenRepository.getSavedToken()).isNull();
            assertThat(emailService.wasPasswordResetEmailSent()).isFalse();
        }

        @Test
        @DisplayName("should delete existing token before creating new one")
        void requestPasswordReset_DeletesExistingToken() {
            userRepository.setUserByEmail(testUser);
            PasswordResetToken existingToken = PasswordResetToken.builder()
                    .token("existing-token")
                    .user(testUser)
                    .build();
            passwordResetTokenRepository.setExistingToken(existingToken);

            userService.requestPasswordReset("test@example.com");

            assertThat(passwordResetTokenRepository.wasTokenDeleted()).isTrue();
            assertThat(passwordResetTokenRepository.getSavedToken()).isNotNull();
        }
    }

    @Nested
    @DisplayName("resetPassword tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("should update password when token is valid")
        void resetPassword_Success() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .id(1L)
                    .token("valid-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .createdAt(LocalDateTime.now())
                    .build();
            passwordResetTokenRepository.setTokenByValue(token);

            userService.resetPassword("valid-token", "newPassword123");

            User savedUser = userRepository.getSavedUser();
            assertThat(savedUser).isNotNull();
            assertThat(passwordEncoder.matches("newPassword123", savedUser.getPassword())).isTrue();
            assertThat(passwordResetTokenRepository.getSavedToken().getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when token not found")
        void resetPassword_TokenNotFound() {
            assertThatThrownBy(() -> userService.resetPassword("invalid-token", "newPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid or expired reset token");
        }

        @Test
        @DisplayName("should throw exception when token is expired")
        void resetPassword_TokenExpired() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .id(1L)
                    .token("expired-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();
            passwordResetTokenRepository.setTokenByValue(token);

            assertThatThrownBy(() -> userService.resetPassword("expired-token", "newPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Reset token has expired");
        }

        @Test
        @DisplayName("should throw exception when token is already used")
        void resetPassword_TokenAlreadyUsed() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .id(1L)
                    .token("used-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .usedAt(LocalDateTime.now().minusMinutes(30))
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build();
            passwordResetTokenRepository.setTokenByValue(token);

            assertThatThrownBy(() -> userService.resetPassword("used-token", "newPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Reset token has already been used");
        }
    }

    // Stub implementations

    private static class StubUserRepository implements UserRepository {
        private User user;
        private User userByEmail;
        private User savedUser;

        public void setUser(User user) {
            this.user = user;
        }

        public void setUserByEmail(User user) {
            this.userByEmail = user;
        }

        public User getSavedUser() {
            return savedUser;
        }

        @Override
        public Optional<User> findByUsername(String username) {
            if (user != null && user.getUsername().equals(username)) {
                return Optional.of(user);
            }
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            if (userByEmail != null && userByEmail.getEmail().equals(email)) {
                return Optional.of(userByEmail);
            }
            return Optional.empty();
        }

        @Override
        public boolean existsByUsername(String username) {
            return user != null && user.getUsername().equals(username);
        }

        @Override
        public boolean existsByEmail(String email) {
            return false;
        }

        @Override
        public boolean existsByRole(String role) {
            return false;
        }

        @Override
        public <S extends User> S save(S entity) {
            this.savedUser = entity;
            return entity;
        }

        // Unused methods - minimal implementation
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(UUID id) { return null; }
        @Override public User getById(UUID id) { return null; }
        @Override public User getReferenceById(UUID id) { return null; }
        @Override public <S extends User> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override public <S extends User> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return null; }
        @Override public <S extends User> java.util.List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public java.util.List<User> findAll() { return null; }
        @Override public java.util.List<User> findAllById(Iterable<UUID> ids) { return null; }
        @Override public Optional<User> findById(UUID id) { return Optional.empty(); }
        @Override public boolean existsById(UUID id) { return false; }
        @Override public long count() { return 0; }
        @Override public void deleteById(UUID id) {}
        @Override public void delete(User entity) {}
        @Override public void deleteAllById(Iterable<? extends UUID> ids) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() {}
        @Override public java.util.List<User> findAll(org.springframework.data.domain.Sort sort) { return null; }
        @Override public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends User> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends User> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends User, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }

    private static class StubPasswordResetTokenRepository implements PasswordResetTokenRepository {
        private PasswordResetToken existingToken;
        private PasswordResetToken tokenByValue;
        private PasswordResetToken savedToken;
        private boolean tokenDeleted = false;

        public void setExistingToken(PasswordResetToken token) {
            this.existingToken = token;
        }

        public void setTokenByValue(PasswordResetToken token) {
            this.tokenByValue = token;
        }

        public PasswordResetToken getSavedToken() {
            return savedToken;
        }

        public boolean wasTokenDeleted() {
            return tokenDeleted;
        }

        @Override
        public Optional<PasswordResetToken> findByToken(String token) {
            if (tokenByValue != null && tokenByValue.getToken().equals(token)) {
                return Optional.of(tokenByValue);
            }
            return Optional.empty();
        }

        @Override
        public Optional<PasswordResetToken> findByUser(User user) {
            if (existingToken != null && existingToken.getUser().equals(user)) {
                return Optional.of(existingToken);
            }
            return Optional.empty();
        }

        @Override
        public void deleteByUser(User user) {
            tokenDeleted = true;
        }

        @Override
        public void delete(PasswordResetToken token) {
            tokenDeleted = true;
        }

        @Override
        public <S extends PasswordResetToken> S save(S entity) {
            this.savedToken = entity;
            return entity;
        }

        // Unused methods - minimal implementation
        @Override public void flush() {}
        @Override public <S extends PasswordResetToken> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends PasswordResetToken> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<PasswordResetToken> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public PasswordResetToken getOne(Long id) { return null; }
        @Override public PasswordResetToken getById(Long id) { return null; }
        @Override public PasswordResetToken getReferenceById(Long id) { return null; }
        @Override public <S extends PasswordResetToken> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override public <S extends PasswordResetToken> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return null; }
        @Override public <S extends PasswordResetToken> java.util.List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public java.util.List<PasswordResetToken> findAll() { return null; }
        @Override public java.util.List<PasswordResetToken> findAllById(Iterable<Long> ids) { return null; }
        @Override public Optional<PasswordResetToken> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends PasswordResetToken> entities) {}
        @Override public void deleteAll() {}
        @Override public java.util.List<PasswordResetToken> findAll(org.springframework.data.domain.Sort sort) { return null; }
        @Override public org.springframework.data.domain.Page<PasswordResetToken> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends PasswordResetToken> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends PasswordResetToken> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends PasswordResetToken> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends PasswordResetToken> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends PasswordResetToken, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }

    private static class StubEmailService extends EmailService {
        private boolean passwordResetEmailSent = false;

        public StubEmailService() {
            super(null);
        }

        public boolean wasPasswordResetEmailSent() {
            return passwordResetEmailSent;
        }

        @Override
        public void sendPasswordResetEmail(String email, String username, String token) {
            passwordResetEmailSent = true;
        }

        @Override
        public void sendVerificationEmail(String email, String username, String token) {
            // Not used in these tests
        }
    }
}
