package com.example.clientservice.client;

import com.example.clientservice.config.WorkflowApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Client for communicating with the Workflow API.
 * Automatically includes the API key in all requests.
 */
@Component
@Slf4j
public class WorkflowApiClient {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final RestTemplate restTemplate;
    private final WorkflowApiConfig config;

    public WorkflowApiClient(WorkflowApiConfig config, RestTemplateBuilder restTemplateBuilder) {
        this.config = config;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Creates a new task in the workflow service.
     */
    public TaskDto createTask(CreateTaskRequest request) {
        String url = config.getBaseUrl() + "/api/tasks";

        HttpEntity<CreateTaskRequest> entity = new HttpEntity<>(request, createHeaders());

        try {
            ResponseEntity<TaskDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    TaskDto.class
            );

            log.info("Task created successfully: {}", response.getBody());
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to create task: {}", e.getMessage());
            throw new WorkflowApiException("Failed to create task", e);
        }
    }

    /**
     * Gets a task by ID.
     */
    public TaskDto getTask(String taskId) {
        String url = config.getBaseUrl() + "/api/tasks/" + taskId;

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<TaskDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    TaskDto.class
            );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to get task {}: {}", taskId, e.getMessage());
            throw new WorkflowApiException("Failed to get task: " + taskId, e);
        }
    }

    /**
     * Completes a task.
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        String url = config.getBaseUrl() + "/api/tasks/" + taskId + "/complete";

        CompleteTaskDto completeRequest = new CompleteTaskDto();
        completeRequest.setVariables(variables);

        HttpEntity<CompleteTaskDto> entity = new HttpEntity<>(completeRequest, createHeaders());

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            log.info("Task {} completed successfully", taskId);

        } catch (RestClientException e) {
            log.error("Failed to complete task {}: {}", taskId, e.getMessage());
            throw new WorkflowApiException("Failed to complete task: " + taskId, e);
        }
    }

    /**
     * Creates HTTP headers with API key authentication.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(API_KEY_HEADER, config.getApiKey());
        return headers;
    }

    // --- DTOs (copy from workflow service or create shared library) ---

    @lombok.Data
    public static class CreateTaskRequest {
        private String taskId;
        private String name;
        private String assignee;
        private String executionId;
        private String processDefinitionId;
        private String processInstanceId;
        private String taskDefinitionKey;
        private String formKey;
        private String tenantId;
        private String taskState;
        private Map<String, Object> variables;
    }

    @lombok.Data
    public static class TaskDto {
        private Long id;
        private String taskId;
        private String name;
        private String assignee;
        private String tenantId;
        private String taskState;
        // ... other fields
    }

    @lombok.Data
    public static class CompleteTaskDto {
        private Map<String, Object> variables;
    }

    public static class WorkflowApiException extends RuntimeException {
        public WorkflowApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
