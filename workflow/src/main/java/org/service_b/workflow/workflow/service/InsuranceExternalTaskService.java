package org.service_b.workflow.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.insurance.persistence.InsuranceState;
import org.service_b.workflow.insurance.service.InsuranceService;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.config.ExternalTaskConfig;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class InsuranceExternalTaskService extends AbstractExternalTaskService {

    private final InsuranceService insuranceService;
    private final Map<String, ExternalTaskConfig.TaskDefinition> taskDefinitions;

    public InsuranceExternalTaskService(FetchAndLockService fetchAndLockService,
                                        CibSevenProperties cibSevenProperties,
                                        CibSevenRestClient cibSevenRestClient,
                                        InsuranceService insuranceService) {
        super(fetchAndLockService, cibSevenProperties, cibSevenRestClient);
        this.insuranceService = insuranceService;
        this.taskDefinitions = initializeTaskDefinitions();
    }

    private Map<String, ExternalTaskConfig.TaskDefinition> initializeTaskDefinitions() {
        Map<String, ExternalTaskConfig.TaskDefinition> defs = new HashMap<>();
        defs.put("creditworthiness", new ExternalTaskConfig.TaskDefinition(
                "check_creditworthiness", "insurance", this::handleCreditWorthinessCheck));
        defs.put("risk", new ExternalTaskConfig.TaskDefinition(
                "risk_assessment", "insurance", this::handleRiskAssessment));
        defs.put("insurance", new ExternalTaskConfig.TaskDefinition(
                "take_out_insurance", "insurance", this::handleTakeOutInsurance));
        return defs;
    }

    @Scheduled(fixedRate = 120000)
    public void getCreditWorthinessCheck() {
        processExternalTask(taskDefinitions, "creditworthiness");
    }

    @Scheduled(fixedRate = 120000)
    public void riskAssessment() {
        processExternalTask(taskDefinitions, "risk");
    }

    @Scheduled(fixedRate = 120000)
    public void takeOutInsurance() {
        processExternalTask(taskDefinitions, "insurance");
    }

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
                break;
            }
        }
        return result;
    }

    private Map<String, Object> handleTakeOutInsurance(Map<String, Map<String, Object>> variables) {
        if (!variables.containsKey("insuranceId")) {
            throw new IllegalStateException("No insuranceId found in variables");
        }

        String insuranceId = (String) variables.get("insuranceId").get("value");
        if (insuranceId == null) {
            throw new IllegalStateException("No insuranceId found in variables");
        }

        insuranceService.updateInsurance(UUID.fromString(insuranceId), InsuranceState.APPROVED);
        log.info("Proposal approved for insurance with id: {}", insuranceId);

        Map<String, Object> result = new HashMap<>();
        result.put("proposalApproved", true);
        return result;
    }
}
