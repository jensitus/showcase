package org.service_b.workflow.security.controller;

import lombok.RequiredArgsConstructor;
import org.service_b.workflow.security.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Generate a new API key for a service.
     * Only accessible by users with ADMIN role.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> generateApiKey(
            @RequestParam String serviceName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer expiresInDays) {

        LocalDateTime expiresAt = expiresInDays != null
                ? LocalDateTime.now().plusDays(expiresInDays)
                : null;

        String apiKey = apiKeyService.generateApiKey(serviceName, description, expiresAt);

        return ResponseEntity.ok(Map.of(
                "serviceName", serviceName,
                "apiKey", apiKey,
                "message", "Store this API key securely. It will not be shown again."
        ));
    }

    /**
     * Regenerate an API key for a service.
     */
    @PostMapping("/{serviceName}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> regenerateApiKey(@PathVariable String serviceName) {
        String apiKey = apiKeyService.regenerateApiKey(serviceName);

        return ResponseEntity.ok(Map.of(
                "serviceName", serviceName,
                "apiKey", apiKey,
                "message", "Store this API key securely. It will not be shown again."
        ));
    }

    /**
     * Revoke an API key for a service.
     */
    @DeleteMapping("/{serviceName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> revokeApiKey(@PathVariable String serviceName) {
        apiKeyService.revokeApiKey(serviceName);

        return ResponseEntity.ok(Map.of(
                "message", "API key revoked for service: " + serviceName
        ));
    }
}
