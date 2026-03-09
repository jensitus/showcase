package org.service_b.workflow.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {
    private String taskId;
    private String name;
    private String assignee;
    private String taskState;
    private String formKey;
}
