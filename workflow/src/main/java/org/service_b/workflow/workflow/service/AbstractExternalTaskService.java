package org.service_b.workflow.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.workflow.config.ExternalTaskConfig;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.dto.FetchAndLock;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import org.service_b.workflow.workflow.dto.Topic;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;

import java.util.Map;

@Slf4j
public abstract class AbstractExternalTaskService {

    protected final FetchAndLockService fetchAndLockService;
    protected final CibSevenProperties cibSevenProperties;
    protected final CibSevenRestClient cibSevenRestClient;

    protected AbstractExternalTaskService(FetchAndLockService fetchAndLockService,
                                          CibSevenProperties cibSevenProperties,
                                          CibSevenRestClient cibSevenRestClient) {
        this.fetchAndLockService = fetchAndLockService;
        this.cibSevenProperties = cibSevenProperties;
        this.cibSevenRestClient = cibSevenRestClient;
    }

    protected void processExternalTask(Map<String, ExternalTaskConfig.TaskDefinition> taskDefinitions, String taskKey) {
        ExternalTaskConfig.TaskDefinition taskDef = taskDefinitions.get(taskKey);

        Topic topic = setTopic(taskDef.topicName(), taskDef.businessKey());
        FetchAndLock fetchAndLock = setFetchAndLock(topic);

        log.info("Processing external task: {}", topic.getTopicName());

        FetchAndLockResponse response = fetchAndLockService.fetchAndLockExternalTask(fetchAndLock);
        Map<String, Object> result = taskDef.handler().processVariables(response.getVariables());

        completeTask(response.getId(), result);
    }

    private void completeTask(String taskId, Map<String, Object> variables) {
        try {
            log.info("Processing task: {}", taskId);
            cibSevenRestClient.completeExternalTask(taskId, variables);
            log.info("Task {} processed and completed successfully", taskId);
        } catch (Exception e) {
            log.error("Unexpected error processing task {}: {}", taskId, e.getMessage(), e);
            throw e;
        }
    }

    private Topic setTopic(String topicName, String tenantIdIn) {
        Topic topic = new Topic();
        topic.setLockDuration(100000);
        topic.setTopicName(topicName);
        topic.setTenantIdIn(new String[]{tenantIdIn});
        return topic;
    }

    private FetchAndLock setFetchAndLock(Topic topic) {
        FetchAndLock fetchAndLock = new FetchAndLock();
        fetchAndLock.setWorkerId(cibSevenProperties.getWorkerId());
        fetchAndLock.setMaxTasks(1);
        fetchAndLock.setTopics(new Topic[]{topic});
        return fetchAndLock;
    }

    protected String strVar(Map<String, Map<String, Object>> variables, String key) {
        return variables.containsKey(key) ? (String) variables.get(key).get("value") : null;
    }

    protected String strVarOr(Map<String, Map<String, Object>> variables, String key, String defaultValue) {
        String val = strVar(variables, key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

}
