package org.service_b.workflow.workflow.dto;

import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class TaskDto {
    private UUID id;
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
    private String additionalInfo;
    private String config;
    private String configData;
}
