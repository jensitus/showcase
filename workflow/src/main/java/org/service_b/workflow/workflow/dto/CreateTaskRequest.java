package org.service_b.workflow.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class CreateTaskRequest {
    private String taskId;
    private String name;
    private String assignee;
    private LocalDateTime created;
    private String executionId;
    private String processDefinitionId;
    private String processInstanceId;
    private String taskDefinitionKey;
    private String formKey;
    private String tenantId;
    private String taskState;
    private Map<String, Object> variables;
}
