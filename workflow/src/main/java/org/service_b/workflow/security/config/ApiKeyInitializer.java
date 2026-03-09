package org.service_b.workflow.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.security.repository.ApiKeyRepository;
import org.service_b.workflow.security.service.ApiKeyService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Initializes a test API key for development purposes.
 * Only active when the 'dev' profile is enabled.
 *
 * To use: Add spring.profiles.active=dev to application.yaml
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInitializer implements CommandLineRunner {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;

    @Override
    public void run(String... args) {
        String serviceName = "camunda-service";

        if (!apiKeyRepository.existsByServiceName(serviceName)) {
            String apiKey = apiKeyService.generateApiKey(
                    serviceName,
                    "API key for Camunda service on port 7001",
                    null  // No expiration for dev
            );

            log.info("===========================================");
            log.info("DEVELOPMENT API KEY GENERATED");
            log.info("===========================================");
            log.info("Service: {}", serviceName);
            log.info("API Key: {}", apiKey);
            log.info("Use header: X-API-Key: {}", apiKey);
            log.info("===========================================");
        } else {
            log.info("API key for '{}' already exists", serviceName);
        }
    }
}
