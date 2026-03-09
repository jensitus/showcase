package com.example.clientservice.service;

import com.example.clientservice.client.WorkflowApiClient;
import com.example.clientservice.client.WorkflowApiClient.CreateTaskRequest;
import com.example.clientservice.client.WorkflowApiClient.TaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Example service showing how to use the WorkflowApiClient.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExampleUsage {

    private final WorkflowApiClient workflowApiClient;

    /**
     * Example: Create a new task in the workflow service.
     */
    public TaskDto createReviewTask(String processInstanceId, String assignee) {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTaskId("task-" + System.currentTimeMillis());
        request.setName("Review Application");
        request.setAssignee(assignee);
        request.setProcessInstanceId(processInstanceId);
        request.setTenantId("default");
        request.setTaskState("CREATED");

        Map<String, Object> variables = new HashMap<>();
        variables.put("priority", "HIGH");
        variables.put("dueDate", "2024-12-31");
        request.setVariables(variables);

        TaskDto createdTask = workflowApiClient.createTask(request);
        log.info("Created task with ID: {}", createdTask.getTaskId());

        return createdTask;
    }

    /**
     * Example: Complete a task.
     */
    public void approveTask(String taskId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("approvedBy", "system");

        workflowApiClient.completeTask(taskId, variables);
        log.info("Task {} approved", taskId);
    }
}
