package com.example.clientservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for connecting to the Workflow API.
 *
 * Add to your application.yaml:
 *
 * workflow-api:
 *   base-url: http://localhost:8080
 *   api-key: ${WORKFLOW_API_KEY:your-dev-key-here}
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "workflow-api")
public class WorkflowApiConfig {

    /**
     * Base URL of the workflow service
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * API key for authentication
     */
    private String apiKey;
}
