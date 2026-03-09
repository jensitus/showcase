package org.service_b.workflow.workflow.dto;

import java.util.Map;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteTaskDto {
    String taskId;
    String taskDefinition;
    Map<String, Object> completeVars;
}
