package org.service_b.workflow.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchAndLock {
    private String workerId;
    private int maxTasks;
    private boolean usePriority;
    /** Long polling: the engine waits up to this many ms for a task; null answers at once. */
    private Integer asyncResponseTimeout;
    private Topic[] topics;
}
