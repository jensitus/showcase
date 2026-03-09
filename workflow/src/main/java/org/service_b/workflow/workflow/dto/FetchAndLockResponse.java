package org.service_b.workflow.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FetchAndLockResponse {
    private String activityId;
    private String activityInstanceId;
    private String errorMessage;
    private String errorDetails;
    private String executionId;
    private String id;
    private String lockExpirationTime;
    private String createTime;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionVersionTag;
    private String processInstanceId;
    private String retries;
    private String suspended;
    private String workerId;
    private String topicName;
    private String tenantId;
    private Map<String, Map<String, Object>> variables;
    private String priority;
    private String businessKey;
    private Object extensionProperties;
}
