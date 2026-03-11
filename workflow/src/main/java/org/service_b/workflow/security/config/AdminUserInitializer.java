package org.service_b.workflow.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.security.entity.User;
import org.service_b.workflow.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the first admin user on startup if ADMIN_USERNAME and ADMIN_PASSWORD are set
 * and no admin user exists yet. Idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.debug("ADMIN_USERNAME/ADMIN_PASSWORD not set — skipping admin seeding");
            return;
        }

        if (userRepository.existsByRole("ADMIN")) {
            log.info("Admin user already exists — skipping admin seeding");
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .email(adminUsername + "@admin.local")
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .role("ADMIN")
                .build();

        userRepository.save(admin);
        log.info("===========================================");
        log.info("Admin user '{}' created successfully", adminUsername);
        log.info("===========================================");
    }
}
