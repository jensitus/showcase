package org.service_b.workflow.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.security.repository.ApiKeyRepository;
import org.service_b.workflow.security.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures an API key exists for the Camunda engine service on every startup.
 *
 * - If CAMUNDA_SERVICE_API_KEY env var is set: stores that key (idempotent).
 * - Otherwise: generates a random key and logs it (development fallback).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInitializer implements CommandLineRunner {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;

    @Value("${camunda.service.api-key:}")
    private String preconfiguredApiKey;

    @Override
    public void run(String... args) {
        String serviceName = "camunda-service";

        if (apiKeyRepository.existsByServiceName(serviceName)) {
            log.info("API key for '{}' already exists — skipping", serviceName);
            return;
        }

        if (preconfiguredApiKey != null && !preconfiguredApiKey.isBlank()) {
            apiKeyService.storeApiKey(serviceName, "API key for Camunda engine (pre-configured)", preconfiguredApiKey);
        } else {
            String apiKey = apiKeyService.generateApiKey(
                    serviceName,
                    "API key for Camunda service on port 7001",
                    null
            );
            log.info("===========================================");
            log.info("API KEY GENERATED — copy to CAMUNDA_SERVICE_API_KEY");
            log.info("===========================================");
            log.info("Service: {}", serviceName);
            log.info("API Key: {}", apiKey);
            log.info("Use header: X-API-Key: {}", apiKey);
            log.info("===========================================");
        }
    }
}
