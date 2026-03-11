package org.service_b.workflow.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.security.entity.ApiKey;
import org.service_b.workflow.security.repository.ApiKeyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int API_KEY_LENGTH = 32;

    /**
     * Generates a new API key for a service.
     * Returns the plain text key (only shown once).
     */
    @Transactional
    public String generateApiKey(String serviceName, String description, LocalDateTime expiresAt) {
        if (apiKeyRepository.existsByServiceName(serviceName)) {
            throw new IllegalArgumentException("API key already exists for service: " + serviceName);
        }

        String plainTextKey = generateSecureKey();
        String keyHash = passwordEncoder.encode(plainTextKey);

        ApiKey apiKey = ApiKey.builder()
                              .keyHash(keyHash)
                              .serviceName(serviceName)
                              .description(description)
                              .enabled(true)
                              .expiresAt(expiresAt)
                              .build();

        apiKeyRepository.save(apiKey);
        log.info("Generated new API key for service: {}", serviceName);

        return plainTextKey;
    }

    /**
     * Validates an API key and returns the associated service name if valid.
     */
    @Transactional
    public Optional<String> validateApiKey(String plainTextKey) {
        // We need to check against all keys since we can't reverse the hash
        for (ApiKey apiKey : apiKeyRepository.findAll()) {
            if (passwordEncoder.matches(plainTextKey, apiKey.getKeyHash())) {
                if (!apiKey.isValid()) {
                    log.warn("API key for service {} is disabled or expired", apiKey.getServiceName());
                    return Optional.empty();
                }

                // Update last used timestamp
                apiKey.setLastUsedAt(LocalDateTime.now());
                apiKeyRepository.save(apiKey);

                log.debug("API key validated for service: {}", apiKey.getServiceName());
                return Optional.of(apiKey.getServiceName());
            }
        }

        log.warn("Invalid API key attempted");
        return Optional.empty();
    }

    /**
     * Revokes an API key for a service.
     */
    @Transactional
    public void revokeApiKey(String serviceName) {
        ApiKey apiKey = apiKeyRepository.findByServiceName(serviceName)
                                        .orElseThrow(() -> new IllegalArgumentException("No API key found for service: " + serviceName));

        apiKey.setEnabled(false);
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key for service: {}", serviceName);
    }

    /**
     * Regenerates an API key for a service.
     * Returns the new plain text key.
     */
    @Transactional
    public String regenerateApiKey(String serviceName) {
        ApiKey apiKey = apiKeyRepository.findByServiceName(serviceName)
                                        .orElseThrow(() -> new IllegalArgumentException("No API key found for service: " + serviceName));

        String plainTextKey = generateSecureKey();
        apiKey.setKeyHash(passwordEncoder.encode(plainTextKey));
        apiKey.setEnabled(true);
        apiKeyRepository.save(apiKey);

        log.info("Regenerated API key for service: {}", serviceName);
        return plainTextKey;
    }

    /**
     * Stores a pre-configured API key for a service (e.g. from an environment variable).
     * The plaintext key is hashed before storage.
     */
    @Transactional
    public void storeApiKey(String serviceName, String description, String plainTextKey) {
        if (apiKeyRepository.existsByServiceName(serviceName)) {
            return;
        }
        ApiKey apiKey = ApiKey.builder()
                              .keyHash(passwordEncoder.encode(plainTextKey))
                              .serviceName(serviceName)
                              .description(description)
                              .enabled(true)
                              .build();
        apiKeyRepository.save(apiKey);
        log.info("Stored pre-configured API key for service: {}", serviceName);
    }

    private String generateSecureKey() {
        byte[] keyBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }
}
