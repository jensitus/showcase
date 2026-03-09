package org.service_b.workflow.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.insurance.persistence.InsuranceState;
import org.service_b.workflow.insurance.service.InsuranceService;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.config.ExternalTaskConfig;
import org.service_b.workflow.workflow.dto.FetchAndLock;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import org.service_b.workflow.workflow.dto.Topic;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
This service is responsible for handling external tasks from CIB Seven.
 */

@Service
@Slf4j
public class ExternalTaskService {
    private final FetchAndLockService fetchAndLockService;
    private final InsuranceService insuranceService;
    private final Map<String, ExternalTaskConfig.TaskDefinition> taskDefinitions;
    private final CibSevenProperties cibSevenProperties;
    private final CibSevenRestClient cibSevenRestClient;

    public ExternalTaskService(FetchAndLockService fetchAndLockService,
                               InsuranceService insuranceService, CibSevenProperties cibSevenProperties, CibSevenRestClient cibSevenRestClient) {
        this.fetchAndLockService = fetchAndLockService;
        this.insuranceService = insuranceService;
        this.cibSevenProperties = cibSevenProperties;
        this.cibSevenRestClient = cibSevenRestClient;
        this.taskDefinitions = initializeTaskDefinitions();
    }

    private Map<String, ExternalTaskConfig.TaskDefinition> initializeTaskDefinitions() {
        return Map.of(
                "creditworthiness", new ExternalTaskConfig.TaskDefinition(
                        "check_creditworthiness",
                        "insurance",
                        this::handleCreditWorthinessCheck
                ),
                "risk", new ExternalTaskConfig.TaskDefinition(
                        "risk_assessment",
                        "insurance",
                        this::handleRiskAssessment
                ),
                "insurance", new ExternalTaskConfig.TaskDefinition(
                        "take_out_insurance",
                        "insurance",
                        this::handleTakeOutInsurance
                )
        );
    }

    @Scheduled(fixedRate = 120000)
    public void getCreditWorthinessCheck() {
        processExternalTask("creditworthiness");
    }

    @Scheduled(fixedRate = 120000)
    public void riskAssessment() {
        processExternalTask("risk");
    }

    @Scheduled(fixedRate = 120000)
    public void takeOutInsurance() {
        processExternalTask("insurance");
    }

    private void processExternalTask(String taskKey) {
        ExternalTaskConfig.TaskDefinition taskDef = taskDefinitions.get(taskKey);

        Topic topic = setTopic(taskDef.topicName(), taskDef.businessKey());
        FetchAndLock fetchAndLock = setFetchAndLock(topic);

        log.info("Processing external task: {}", topic.getTopicName());

        FetchAndLockResponse response = fetchAndLockService.fetchAndLockExternalTask(fetchAndLock);
        Map<String, Object> result = taskDef.handler().processVariables(response.getVariables());

        processTask(response.getId(), result);
    }

    // Handler methods
    private Map<String, Object> handleCreditWorthinessCheck(Map<String, Map<String, Object>> variables) {
        Map<String, Object> result = new HashMap<>();
        if (variables.containsKey("sufficientIncome")) {
            result.put("creditCheckOutcome", variables.get("sufficientIncome").get("value"));
        }
        return result;
    }

    private Map<String, Object> handleRiskAssessment(Map<String, Map<String, Object>> variables) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : variables.entrySet()) {
            log.info("key: {} value: {}", entry.getKey(), entry.getValue());
            if ("floodRisk".equals(entry.getKey()) || "mudslideRisk".equals(entry.getKey())) {
                result.put("risk", entry.getValue().get("value"));
                break; // Assuming you want the first match
            }
        }
        return result;
    }

    private Map<String, Object> handleTakeOutInsurance(Map<String, Map<String, Object>> variables) {
        Map<String, Object> result = new HashMap<>();

        if (!variables.containsKey("insuranceId")) {
            throw new IllegalStateException("No insuranceId found in variables");
        }

        String insuranceId = (String) variables.get("insuranceId").get("value");
        if (insuranceId == null) {
            throw new IllegalStateException("No insuranceId found in variables");
        }

        insuranceService.updateInsurance(UUID.fromString(insuranceId), InsuranceState.APPROVED);
        result.put("proposalApproved", true);
        log.info("Proposal approved for insurance with id: {}", insuranceId);

        return result;
    }

    private void processTask(String taskId, Map<String, Object> variables) {
        try {
            log.info("Processing task: {}", taskId);

            // Execute your business logic
            Map<String, Object> result = executeBusinessLogic(variables);

            // Complete the task in CIB Seven with results
            cibSevenRestClient.completeExternalTask(taskId, result);

            log.info("Task {} processed and completed successfully", taskId);

        } catch (BusinessLogicException e) {
            log.error("Business logic failed for task {}: {}", taskId, e.getMessage());

            // Report failure to CIB Seven with retry
            cibSevenRestClient.handleTaskFailure(taskId,
                                                 "Business logic failed: " + e.getMessage(),
                                                 getStackTrace(e)
            );

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

    private Map<String, Object> executeBusinessLogic(Map<String, Object> input) throws BusinessLogicException {
        return new HashMap<>(input);
    }

    private String getStackTrace(Exception e) {
        return e.toString();
    }

    public static class BusinessLogicException extends Exception {
        public BusinessLogicException(String message) {
            super(message);
        }
    }

}
