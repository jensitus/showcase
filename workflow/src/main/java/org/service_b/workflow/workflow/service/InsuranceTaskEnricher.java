package org.service_b.workflow.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.workflow.dto.CreateTaskRequest;
import org.service_b.workflow.workflow.dto.TaskDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceTaskEnricher implements TaskEnricher {

    private final InsuranceWorkflowService insuranceWorkflowService;

    @Override
    public boolean supports(Map<String, Object> variables) {
        return variables.containsKey("customerId");
    }

    @Override
    public TaskDto enrich(String taskKey, CreateTaskRequest request, TaskDto taskDto) {
        UUID customerId = extractCustomerId(request.getVariables());
        if (customerId == null) {
            log.warn("customerId not parseable for task: {}", taskKey);
            return null;
        }

        return switch (taskKey) {
            case "ut_manual_creditworthiness_check" -> handleCreditworthinessCheck(customerId, request, taskDto);
            case "ut_manual_liability_check"        -> handleLiabilityCheck(customerId, request, taskDto);
            case "ut_manual_household_check"        -> handleRiskAssessment(customerId, request, taskDto);
            default -> {
                log.debug("No enrichment for insurance task: {}", taskKey);
                yield null;
            }
        };
    }

    private TaskDto handleCreditworthinessCheck(UUID customerId, CreateTaskRequest request, TaskDto taskDto) {
        Boolean sufficientIncome = parseBoolean(request.getVariables().get("sufficientIncome"));
        String initiator = getStringValue(request.getVariables(), "initiator");
        return insuranceWorkflowService.manualCreditworthinessCheck(
                customerId, sufficientIncome, request.getTaskId(), initiator, taskDto);
    }

    private TaskDto handleLiabilityCheck(UUID customerId, CreateTaskRequest request, TaskDto taskDto) {
        String insuranceCoverage = getStringValue(request.getVariables(), "insuranceCoverage");
        String insuranceSum      = getStringValue(request.getVariables(), "insuranceSum");
        String initiator         = getStringValue(request.getVariables(), "initiator");
        return insuranceWorkflowService.manualLiabilityCheck(
                customerId, insuranceCoverage, insuranceSum, initiator, taskDto);
    }

    private TaskDto handleRiskAssessment(UUID customerId, CreateTaskRequest request, TaskDto taskDto) {
        Boolean mudslideRisk = parseBoolean(request.getVariables().get("mudslideRisk"));
        Boolean floodRisk    = parseBoolean(request.getVariables().get("floodRisk"));
        String initiator     = getStringValue(request.getVariables(), "initiator");
        return insuranceWorkflowService.manualRiskAssessment(
                customerId, mudslideRisk, floodRisk, initiator, taskDto);
    }

    private UUID extractCustomerId(Map<String, Object> variables) {
        try {
            Object value = variables.get("customerId");
            return value != null ? UUID.fromString(value.toString()) : null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid customerId format", e);
            return null;
        }
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) return false;
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse boolean value: {}", value, e);
            return false;
        }
    }

    private String getStringValue(Map<String, Object> variables, String key) {
        Object value = variables.get(key);
        return value != null ? value.toString() : null;
    }
}
