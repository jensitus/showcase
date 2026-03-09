package at.cibseven.cibdemo.service;

import lombok.extern.slf4j.Slf4j;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/*
This service is responsible for notifying the workflow service about the state of a process instance.
 */

@Slf4j
@Service
public class ProcessStateNotificationService implements ExecutionListener {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final RestTemplate restTemplate;
    private final String workflowServiceUrl;
    private final String apiKey;

    public ProcessStateNotificationService(
            RestTemplate restTemplate,
            @Value("${workflow-api.base-url:http://localhost:8080}") String workflowServiceUrl,
            @Value("${workflow-api.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.workflowServiceUrl = workflowServiceUrl;
        this.apiKey = apiKey;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        // only handle the root process instance execution (no parent = root)
        if (execution.getParentId() != null) {
            return;
        }

        if (execution.isCanceled()) {
            String processInstanceId = execution.getProcessInstanceId();
            log.info("Process instance {} cancelled", processInstanceId);
            notifyWorkflowService(processInstanceId, "CANCELLED");
        }
    }

    private void notifyWorkflowService(String processInstanceId, String state) {
        try {
            String url = workflowServiceUrl + "/api/process-instances/" + processInstanceId + "/state";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set(API_KEY_HEADER, apiKey);
            }
            restTemplate.postForEntity(url, new HttpEntity<>(Map.of("state", state), headers), Void.class);
            log.info("Notified workflow service: process {} → {}", processInstanceId, state);
        } catch (Exception e) {
            log.error("Failed to notify workflow service of process {} state change", processInstanceId, e);
        }
    }
}
