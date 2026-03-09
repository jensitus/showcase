package org.service_b.workflow.workflow.rest;

import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.dto.CompleteExternalTask;
import org.service_b.workflow.workflow.service.RestClientService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CibSevenRestClient {

    @Value("${cibseven.base-url}")
    private String cibsevenBaseUrl;

    private final RestTemplate restTemplate;
    private final CibSevenProperties cibSevenProperties;
    private final RestClientService restClientService;
    private final RestClient restClient;

    public CibSevenRestClient(RestTemplate restTemplate, CibSevenProperties cibSevenProperties, RestClientService restClientService, RestClient.Builder restClientBuilder) {
        this.restTemplate = restTemplate;
        this.cibSevenProperties = cibSevenProperties;
        this.restClientService = restClientService;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Complete an external task with variables
     */
    public void completeExternalTask(String taskId, Map<String, Object> variables) {
        completeExternalTask(taskId, cibSevenProperties.getWorkerId(), variables);
    }


    /**
     * Complete an external task with custom worker ID
     */
    public void completeExternalTask(String taskId, String workerId, Map<String, Object> variables) {
        String url = cibSevenProperties.getBaseUrl() + "/external-task/" + taskId + "/complete";

        try {
            CompleteTaskRequest request = new CompleteTaskRequest();
            request.setWorkerId(workerId);

            if (variables != null && !variables.isEmpty()) {
                Map<String, CamundaVariable> camundaVariables = new HashMap<>();
                variables.forEach((key, value) ->
                                          camundaVariables.put(key, createCamundaVariable(value))
                );
                request.setVariables(camundaVariables);
            }

            ResponseEntity<Void> response = completeExternalTask(taskId, request);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Task {} completed successfully", taskId);
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.error("Task not found: {}", taskId);
            throw new CamundaApiException("Task not found: " + taskId, e);
        } catch (HttpServerErrorException e) {
            log.error("Camunda server error while completing task {}: {}", taskId, e.getMessage());
            throw new CamundaApiException("Server error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to complete task {}: {}", taskId, e.getMessage());
            throw new CamundaApiException("Failed to complete task: " + e.getMessage(), e);
        }
    }

    /**
     * Report a failure for an external task
     */
    public void handleTaskFailure(String taskId, String errorMessage, String errorDetails) {
        handleTaskFailure(taskId,
                          cibSevenProperties.getWorkerId(),
                          errorMessage,
                          errorDetails,
                          cibSevenProperties.getDefaultRetries(),
                          cibSevenProperties.getDefaultRetryTimeout()
        );
    }

    /**
     * Report a failure with custom retry configuration
     */
    public void handleTaskFailure(String taskId, String workerId, String errorMessage,
                                  String errorDetails, int retries, long retryTimeout) {
        String url = cibSevenProperties.getBaseUrl() + "/external-task/" + taskId + "/failure";

        try {
            TaskFailureRequest request = new TaskFailureRequest();
            request.setWorkerId(workerId);
            request.setErrorMessage(errorMessage);
            request.setErrorDetails(errorDetails);
            request.setRetries(retries);
            request.setRetryTimeout(retryTimeout);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<TaskFailureRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            log.warn("Reported failure for task {}: {}", taskId, errorMessage);

        } catch (Exception e) {
            log.error("Failed to report task failure for {}: {}", taskId, e.getMessage());
            throw new CamundaApiException("Failed to report task failure: " + e.getMessage(), e);
        }
    }

    /**
     * Create a Camunda variable with proper type detection
     */
    private CamundaVariable createCamundaVariable(Object value) {
        CamundaVariable variable = new CamundaVariable();
        variable.setValue(value);
        variable.setType(detectType(value));
        return variable;
    }

    /**
     * Detect Camunda variable type from Java object
     */
    private String detectType(Object value) {
        if (value == null) return "Null";
        if (value instanceof String) return "String";
        if (value instanceof Integer) return "Integer";
        if (value instanceof Long) return "Long";
        if (value instanceof Double || value instanceof Float) return "Double";
        if (value instanceof Boolean) return "Boolean";
        if (value instanceof java.util.Date) return "Date";
        return "Json";
    }

    // ============== Request/Response DTOs ==============

    @Data
    private static class CompleteTaskRequest {
        private String workerId;
        private Map<String, CamundaVariable> variables;
        private Map<String, CamundaVariable> localVariables;
    }

    @Data
    private static class TaskFailureRequest {
        private String workerId;
        private String errorMessage;
        private String errorDetails;
        private Integer retries;
        private Long retryTimeout;
    }

    @Data
    private static class CamundaVariable {
        private Object value;
        private String type;
    }

    // ============== Custom Exception ==============

    public static class CamundaApiException extends RuntimeException {
        public CamundaApiException(String message) {
            super(message);
        }

        public CamundaApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private ResponseEntity<Void> completeExternalTask(String taskId, CompleteTaskRequest completeTaskRequest) {
        ResponseEntity<Void> response = restClient.post()
                                                  .uri(cibsevenBaseUrl + "/external-task/" + taskId + "/complete")
                                                  .body(completeTaskRequest)
                                                  .retrieve()
                                                  .toBodilessEntity();
        log.info(response.toString());
        return response;
    }

}
