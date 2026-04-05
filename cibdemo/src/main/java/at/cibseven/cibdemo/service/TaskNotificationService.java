package at.cibseven.cibdemo.service;

import at.cibseven.cibdemo.config.ApiKeyProvider;
import at.cibseven.cibdemo.dto.TaskUpdateRequest;
import at.cibseven.cibdemo.dto.TaskDto;
import lombok.extern.slf4j.Slf4j;
import org.cibseven.bpm.engine.impl.persistence.entity.TaskEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TaskNotificationService {

    private static final String TASK_ENDPOINT = "/api/tasks";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final RestTemplate restTemplate;
    private final String workflowServiceUrl;
    private final ApiKeyProvider apiKeyProvider;

    public TaskNotificationService(RestTemplate restTemplate,
                                   @Value("${workflow-api.base-url:http://localhost:8080}") String workflowServiceUrl,
                                   ApiKeyProvider apiKeyProvider) {
        this.restTemplate = restTemplate;
        this.workflowServiceUrl = workflowServiceUrl;
        this.apiKeyProvider = apiKeyProvider;
    }

    public void notifyFromSnapshot(TaskDto snapshot) {
        if (TaskEntity.TaskState.STATE_CREATED.name().equals(snapshot.getTaskState())) {
            log.info("Task CREATE event detected for task: {}", snapshot.getName());
            createTask(snapshot);
        } else if (TaskEntity.TaskState.STATE_COMPLETED.name().equals(snapshot.getTaskState())) {
            log.info("Task COMPLETE event detected for task: {}", snapshot.getName());
            updateTask(snapshot);
        }
    }

    private void createTask(TaskDto task) {
        try {
            HttpEntity<TaskDto> request = new HttpEntity<>(task, createHeaders());
            log.info("Sending task notification for task: {} to {}", task.getName(), workflowServiceUrl + TASK_ENDPOINT);
            restTemplate.postForEntity(workflowServiceUrl + TASK_ENDPOINT, request, TaskDto.class);
            log.info("Task notification sent successfully for task: {}", task.getName());
        } catch (Exception e) {
            log.error("Failed to send task notification for task: {}", task.getName(), e);
        }
    }

    private void updateTask(TaskDto snapshot) {
        try {
            TaskUpdateRequest taskUpdateRequest = new TaskUpdateRequest();
            taskUpdateRequest.setTaskId(snapshot.getTaskId());
            taskUpdateRequest.setTaskState(snapshot.getTaskState());
            taskUpdateRequest.setName(snapshot.getName());
            taskUpdateRequest.setAssignee(snapshot.getAssignee());
            HttpEntity<TaskUpdateRequest> request = new HttpEntity<>(taskUpdateRequest, createHeaders());
            log.info("Sending task update notification for task: {} to {}", snapshot.getName(), workflowServiceUrl + TASK_ENDPOINT);
            restTemplate.put(workflowServiceUrl + TASK_ENDPOINT + "/" + taskUpdateRequest.getTaskId(), request);
            log.info("Task update notification sent successfully for task: {}", snapshot.getName());
        } catch (Exception e) {
            log.error("Failed to send task update notification for task: {}", snapshot.getName(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = apiKeyProvider.getApiKey();
        if (!apiKey.isBlank()) {
            headers.set(API_KEY_HEADER, apiKey);
        }
        return headers;
    }
}
