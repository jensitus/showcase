package at.cibseven.cibdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Provides the API key used to authenticate calls to the workflow service backend.
 *
 * Resolution order:
 *   1. WORKFLOW_API_API_KEY env var (explicit override)
 *   2. Derived deterministically from JWT_SECRET (default, self-healing)
 *
 * The derivation is identical to ApiKeyInitializer on the backend side,
 * so both services always share the same key as long as JWT_SECRET matches.
 */
@Component
@Slf4j
public class ApiKeyProvider {

    private final String apiKey;

    public ApiKeyProvider(
            @Value("${workflow-api.api-key:}") String configuredKey,
            @Value("${jwt.secret:}") String jwtSecret) {

        if (configuredKey != null && !configuredKey.isBlank()) {
            log.info("ApiKeyProvider: using pre-configured workflow API key");
            this.apiKey = configuredKey;
        } else if (jwtSecret != null && !jwtSecret.isBlank()) {
            log.info("ApiKeyProvider: deriving workflow API key from JWT secret");
            this.apiKey = deriveKey(jwtSecret);
        } else {
            log.warn("ApiKeyProvider: no API key available — outgoing calls to workflow service will be unauthenticated");
            this.apiKey = "";
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    private static String deriveKey(String jwtSecret) {
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
