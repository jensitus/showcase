package org.service_b.workflow.workflow.dto;

import lombok.Data;

@Data
public class FetchAndLock {
    private String workerId;
    private int maxTasks;
    private boolean usePriority;
    private Topic[] topics;
}
