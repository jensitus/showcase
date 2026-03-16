package org.service_b.workflow.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.security.repository.ApiKeyRepository;
import org.service_b.workflow.security.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Ensures an API key exists for the Camunda engine service on every startup.
 *
 * Key resolution order:
 *   1. CAMUNDA_SERVICE_API_KEY env var (explicit override)
 *   2. Derived deterministically from the JWT secret (default, self-healing)
 *
 * If the DB already contains a key that no longer matches (e.g. leftover from
 * a previous random-generation run), it is updated automatically so the system
 * stays consistent without manual intervention.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInitializer implements CommandLineRunner {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${camunda.service.api-key:}")
    private String preconfiguredApiKey;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final String SERVICE_NAME = "camunda-service";

    @Override
    public void run(String... args) {
        String key = resolveKey();

        apiKeyRepository.findByServiceName(SERVICE_NAME).ifPresentOrElse(
            existing -> {
                if (passwordEncoder.matches(key, existing.getKeyHash())) {
                    log.info("API key for '{}' is up to date — skipping", SERVICE_NAME);
                } else {
                    existing.setKeyHash(passwordEncoder.encode(key));
                    apiKeyRepository.save(existing);
                    log.info("API key for '{}' updated (re-derived from JWT secret)", SERVICE_NAME);
                }
            },
            () -> {
                apiKeyService.storeApiKey(SERVICE_NAME, "API key for Camunda engine (derived from JWT secret)", key);
                log.info("API key for '{}' stored", SERVICE_NAME);
            }
        );
    }

    private String resolveKey() {
        if (preconfiguredApiKey != null && !preconfiguredApiKey.isBlank()) {
            log.info("Using pre-configured API key for '{}'", SERVICE_NAME);
            return preconfiguredApiKey;
        }
        log.info("Deriving API key for '{}' from JWT secret", SERVICE_NAME);
        return deriveKey(jwtSecret);
    }

    /**
     * Derives a stable API key from the JWT secret.
     * Both this service and the engine use the same derivation — as long as
     * JWT_SECRET is the same on both sides, the key is always reproducible.
     */
    public static String deriveKey(String jwtSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                (jwtSecret + ":camunda-service").getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
