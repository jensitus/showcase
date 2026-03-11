package at.cibseven.cibdemo.service;

import at.cibseven.cibdemo.dto.TaskUpdateRequest;
import at.cibseven.cibdemo.dto.TaskDto;
import lombok.extern.slf4j.Slf4j;
import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;
import org.cibseven.bpm.engine.impl.persistence.entity.TaskEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
public class TaskNotificationService implements TaskListener {

    private static final String TASK_ENDPOINT = "/api/tasks";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final RestTemplate restTemplate;
    private final String workflowServiceUrl;
    private final String apiKey;

    public TaskNotificationService(RestTemplate restTemplate,
                                   @Value("${workflow-api.base-url:http://localhost:8080}") String workflowServiceUrl,
                                   @Value("${workflow-api.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.workflowServiceUrl = workflowServiceUrl;
        this.apiKey = apiKey;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            log.info("Task CREATE event detected for task: {}", delegateTask.getName());
           createTask(delegateTask);
        }
        if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            log.info("Task COMPLETE event detected for task: {}", delegateTask.getName());
            updateTask(delegateTask);
        }
    }

    private void createTask(DelegateTask delegateTask) {
        try {
            TaskDto task = new TaskDto();
            task.setTaskId(delegateTask.getId());
            task.setName(delegateTask.getName());
            task.setTaskDefinitionKey(delegateTask.getTaskDefinitionKey());
            task.setProcessInstanceId(delegateTask.getProcessInstanceId());
            task.setProcessDefinitionId(delegateTask.getProcessDefinitionId());
            task.setAssignee(delegateTask.getAssignee());
            task.setCreated(convertToLocalDateTime(delegateTask.getCreateTime()));
            task.setExecutionId(delegateTask.getExecutionId());
            task.setTenantId(delegateTask.getTenantId());
            task.setTaskState(TaskEntity.TaskState.STATE_CREATED.name());
            task.setVariables(delegateTask.getVariables());

            HttpHeaders headers = createHeaders();

            HttpEntity<TaskDto> request = new HttpEntity<>(task, headers);

            log.info("Sending task notification for task: {} to {} with headers: {}", delegateTask.getName(), workflowServiceUrl + TASK_ENDPOINT, headers);
            restTemplate.postForEntity(workflowServiceUrl + TASK_ENDPOINT, request, TaskDto.class);
            log.info("Task notification sent successfully for task: {}", delegateTask.getName());

        } catch (Exception e) {
            log.error("Failed to send task notification for task: {}", delegateTask.getName(), e);
        }
    }

    private void updateTask(DelegateTask delegateTask) {
        try {
            TaskUpdateRequest taskUpdateRequest = new TaskUpdateRequest();
            taskUpdateRequest.setTaskId(delegateTask.getId());
            taskUpdateRequest.setTaskState(TaskEntity.TaskState.STATE_COMPLETED.name());
            taskUpdateRequest.setName(delegateTask.getName());
            taskUpdateRequest.setAssignee(delegateTask.getAssignee());
            HttpHeaders headers = createHeaders();
            HttpEntity<TaskUpdateRequest> request = new HttpEntity<>(taskUpdateRequest, headers);
            log.info("Sending task update notification for task: {} to {}", delegateTask.getName(), workflowServiceUrl + TASK_ENDPOINT);
            restTemplate.put( workflowServiceUrl + TASK_ENDPOINT + "/" + taskUpdateRequest.getTaskId(), request);
            log.info("Task update notification sent successfully for task: {}", delegateTask.getName());
        } catch (Exception e) {
            log.error("Failed to send task update notification for task: {}", delegateTask.getName(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set(API_KEY_HEADER, apiKey);
        }
        return headers;
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
