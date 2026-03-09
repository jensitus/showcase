package org.service_b.workflow.workflow.config;

import java.util.Map;

public class ExternalTaskConfig {
    @FunctionalInterface
    public interface TaskHandler {
        Map<String, Object> processVariables(Map<String, Map<String, Object>> variables);
    }

    public record TaskDefinition(
            String topicName,
            String businessKey,
            TaskHandler handler
    ) {}
}
