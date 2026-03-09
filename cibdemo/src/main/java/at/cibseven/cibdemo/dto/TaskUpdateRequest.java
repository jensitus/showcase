package at.cibseven.cibdemo.dto;

import lombok.Data;

@Data
public class TaskUpdateRequest {
    private String taskId;
    private String name;
    private String assignee;
    private String taskState;
    private String formKey;
}
