package org.service_b.workflow.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Separate configuration for PasswordEncoder to avoid circular dependencies.
 * This is extracted from SecurityConfig because ApiKeyService needs PasswordEncoder,
 * and SecurityConfig needs ApiKeyAuthenticationFilter which depends on ApiKeyService.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
