package at.cibseven.cibdemo.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class TaskDto {
    String taskId;
    String name;
    String assignee;
    LocalDateTime created;
    String executionId;
    String processDefinitionId;
    String processInstanceId;
    String taskDefinitionKey;
    String formKey;
    String tenantId;
    String taskState;
    Map<String, Object> variables;
}
