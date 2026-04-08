package org.service_b.workflow.workflow.service;

import org.service_b.workflow.workflow.dto.CreateTaskRequest;
import org.service_b.workflow.workflow.dto.TaskDto;

import java.util.Map;

public interface TaskEnricher {

    /** Returns true if this enricher handles the given task variables. */
    boolean supports(Map<String, Object> variables);

    /** Enrich the task DTO with domain-specific config and context. */
    TaskDto enrich(String taskKey, CreateTaskRequest request, TaskDto taskDto);

    /** Apply domain-specific variable coercions before completing a task. */
    default Map<String, Object> coerceCompleteVars(String taskDefinitionKey, Map<String, Object> vars) {
        return vars;
    }
}
