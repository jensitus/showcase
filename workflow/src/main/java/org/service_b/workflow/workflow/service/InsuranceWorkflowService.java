package org.service_b.workflow.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.customer.dto.CustomerDto;
import org.service_b.workflow.customer.service.CustomerService;
import org.service_b.workflow.insurance.persistence.Insurance;
import org.service_b.workflow.insurance.persistence.InsuranceState;
import org.service_b.workflow.insurance.service.InsuranceService;
import org.service_b.workflow.shared.entity.Gender;
import org.service_b.workflow.shared.utils.HashMapConverter;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.dto.ProcessInstanceWithVariableDto;
import org.service_b.workflow.workflow.dto.RequestedInsuranceDto;
import org.service_b.workflow.workflow.dto.TaskDto;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InsuranceWorkflowService {

    private static final String POSITION = "position";
    private static final String MONTHLY_INCOME = "monthlyIncome";
    private static final String MONTHLY_SALARY = "monthly salary";
    private static final String MANUAL_CREDIT_CHECK_OUTCOME = "manualCreditCheckOutcome";
    private static final String MANUAL_RISK_ASSESSMENT_OUTCOME = "manualRiskAssessment";
    private static final String TASK_DEFINITION_WORTHINESS_CHECK = "manualCreditworthinessCheck";
    private static final String TASK_DEFINITION_RISK_ASSESSMENT = "manualRiskAssessment";
    private static final String TASK_DEFINITION_LIABILITY_CHECK = "manualLiabilityCheck";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String[] APPROVAL_ARRAY = new String[]{APPROVED, REJECTED};

    @Value("${workflow-service.base-url}")
    private String workflowServiceUrl;

    private static final String COMPLETE_ENDPOINT = "/workflows/complete-task";

    private static final String MODULE_ID = "insurance";

    private static final String INSURANCE_PROCESS_DEFINITION_KEY = "insurance_showcase";
    private static final String INSURANCE_TENANT_ID = "insurance";

    private final FetchAndLockService fetchAndLockService;
    private final CibSevenRestClient cibSevenRestClient;
    private final CibSevenProperties cibSevenProperties;
    private final InsuranceService insuranceService;
    private final RestClientService restClientService;
    private final CustomerService customerService;
    private final HashMapConverter hashMapConverter;
    private final ProcessService processService;


    @Transactional
    public void startInsuranceWorkflow(RequestedInsuranceDto requestedInsuranceDto) throws Exception {

        final Insurance insurance = insuranceService.saveInsurance(requestedInsuranceDto.getCustomerId(),
                                                                   requestedInsuranceDto.getInsuranceType().toString(),
                                                                   requestedInsuranceDto.getInsuranceCoverage(),
                                                                   requestedInsuranceDto.getInsuranceSum(),
                                                                   requestedInsuranceDto.getPaymentSchedule(),
                                                                   requestedInsuranceDto.isFloodRisk(),
                                                                   requestedInsuranceDto.isMudslideRisk(),
                                                                   requestedInsuranceDto.getAmount(),
                                                                   InsuranceState.REQUESTED);
        StartProcessBody startProcessBody = getStartProcessBody(requestedInsuranceDto, insurance);
        ProcessInstanceWithVariableDto processInstanceWithVariableDto =
                restClientService.startCib7Process(INSURANCE_PROCESS_DEFINITION_KEY,
                                                   startProcessBody,
                                                   INSURANCE_TENANT_ID);

        log.info(processInstanceWithVariableDto.toString());
        processService.saveProcess(processInstanceWithVariableDto, insurance);
    }

    private StartProcessBody getStartProcessBody(RequestedInsuranceDto requestedInsuranceDto, Insurance insurance) {
        String initiator = SecurityContextHolder.getContext().getAuthentication().getName();

        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", Map.of("value", requestedInsuranceDto.getCustomerId(), "type", "String"));
        variables.put("insuranceType", Map.of("value", requestedInsuranceDto.getInsuranceType(), "type", "String"));
        variables.put("insuranceCoverage", Map.of("value", requestedInsuranceDto.getInsuranceCoverage(), "type", "String"));
        variables.put("insuranceSum", Map.of("value", requestedInsuranceDto.getInsuranceSum(), "type", "String"));
        variables.put("paymentSchedule", Map.of("value", requestedInsuranceDto.getPaymentSchedule(), "type", "String"));
        variables.put("floodRisk", Map.of("value", requestedInsuranceDto.isFloodRisk(), "type", "Boolean"));
        variables.put("mudslideRisk", Map.of("value", requestedInsuranceDto.isMudslideRisk(), "type", "Boolean"));
        variables.put("amount", Map.of("value", insurance.getAmount(), "type", "Integer"));
        variables.put("sufficientIncome", Map.of("value", requestedInsuranceDto.isSufficientIncome(), "type", "Boolean"));
        variables.put("insuranceId", Map.of("value", insurance.getId(), "type", "String"));
        variables.put("initiator", Map.of("value", initiator, "type", "String"));

        StartProcessBody startProcessBody = new StartProcessBody();
        startProcessBody.setVariables(variables);
        startProcessBody.setBusinessKey("insurance-" + requestedInsuranceDto.getCustomerId());
        return startProcessBody;
    }

    public TaskDto manualCreditworthinessCheck(UUID customerId,
                                               Boolean sufficientIncome,
                                               String taskId,
                                               String initiator,
                                               TaskDto taskDto) {

        Map<String, Object> additionalInfo = buildAdditionalInfo(customerId, sufficientIncome);
        Map<String, Object> config = buildConfig();
        Map<String, Object> configData = buildConfigData();

        log.info("task ID: {}", taskId);
        return populateTaskDto(taskDto, additionalInfo, config, configData, initiator);
    }

    public TaskDto manualRiskAssessment(UUID customerId,
                                        Boolean mudslideRisk,
                                        Boolean floodRisk,
                                        String initiator,
                                        TaskDto taskDto) {

        Map<String, Object> additionalInfo = buildRiskAssessmentAdditionalInfo(customerId, mudslideRisk, floodRisk);
        Map<String, Object> config = buildConfigWithFields("furtherInformation", MANUAL_RISK_ASSESSMENT_OUTCOME);
        Map<String, Object> configData = buildRiskAssessmentConfigData();

        return populateTaskDto(taskDto, additionalInfo, config, configData, initiator);
    }

    public TaskDto manualLiabilityCheck(UUID customerId,
                                        String insuranceCoverage,
                                        String insuranceSum,
                                        String initiator,
                                        TaskDto taskDto) {

        Map<String, Object> additionalInfo = buildLiabilityCheckAdditionalInfo(customerId, insuranceCoverage, insuranceSum);
        Map<String, Object> config = buildConfigWithFields("furtherInformation", "liabilityCheck");
        Map<String, Object> configData = buildLiabilityCheckConfigData();

        return populateTaskDto(taskDto, additionalInfo, config, configData, initiator);
    }

    private TaskDto populateTaskDto(TaskDto taskDto,
                                    Map<String, Object> additionalInfo,
                                    Map<String, Object> config,
                                    Map<String, Object> configData,
                                    String initiator) {
        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(additionalInfo));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(config));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(configData));
        return taskDto;
    }

    private Map<String, Object> buildConfigWithFields(String... fieldNames) {
        Map<String, Object> config = new HashMap<>();
        for (String fieldName : fieldNames) {
            config.put(fieldName, "??");
        }
        return config;
    }

    private Map<String, Object> buildFieldConfig(int position, String type, Object values) {
        Map<String, Object> fieldConfig = new HashMap<>();
        fieldConfig.put(POSITION, position);
        fieldConfig.put("type", type);
        if (values != null) {
            fieldConfig.put("values", values);
        }
        return fieldConfig;
    }

// Risk Assessment specific methods

    private Map<String, Object> buildRiskAssessmentAdditionalInfo(UUID customerId,
                                                                  Boolean mudslideRisk,
                                                                  Boolean floodRisk) {
        Map<String, Object> additionalInfo = getCustomerToAdditionalInfoMap(customerId);

        Map<String, String> riskAssessment = new HashMap<>();
        riskAssessment.put("Danger of mudslides", mudslideRisk ? "Yes" : "No");
        riskAssessment.put("Danger of flooding", floodRisk ? "Yes" : "No");

        additionalInfo.put("Risk Assessment", riskAssessment);
        return additionalInfo;
    }

    private Map<String, Object> buildRiskAssessmentConfigData() {
        Map<String, Object> configData = new HashMap<>();
        configData.put(MANUAL_RISK_ASSESSMENT_OUTCOME, buildFieldConfig(1, "select", APPROVAL_ARRAY));
        configData.put("furtherInformation", buildFieldConfig(2, "textarea", null));
        return configData;
    }

// Liability Check specific methods

    private Map<String, Object> buildLiabilityCheckAdditionalInfo(UUID customerId,
                                                                  String insuranceCoverage,
                                                                  String insuranceSum) {
        Map<String, Object> additionalInfo = getCustomerToAdditionalInfoMap(customerId);

        Map<String, String> liabilityCheck = new HashMap<>();
        liabilityCheck.put("insuranceCoverage", insuranceCoverage);
        liabilityCheck.put("insuranceSum", insuranceSum);

        additionalInfo.put("liabilityCheck", liabilityCheck);
        return additionalInfo;
    }

    private Map<String, Object> buildLiabilityCheckConfigData() {
        Map<String, Object> configData = new HashMap<>();
        configData.put("furtherInformation", buildFieldConfig(1, "textarea", null));
        configData.put("liabilityCheck", buildFieldConfig(2, "select", APPROVAL_ARRAY));
        return configData;
    }

    // helper:

    private Map<String, Object> buildAdditionalInfo(UUID customerId, Boolean sufficientIncome) {
        Map<String, Object> additionalInfo = getCustomerToAdditionalInfoMap(customerId);
        additionalInfo.put("Account", buildAccountInfo(sufficientIncome));
        return additionalInfo;
    }

    private Map<String, String> buildAccountInfo(Boolean sufficientIncome) {
        Map<String, String> account = new HashMap<>();
        String salaryRange = sufficientIncome ? "> € 5000.-" : "< € 5000.-";
        account.put(MONTHLY_SALARY, salaryRange);
        return account;
    }

    private Map<String, Object> buildConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(MONTHLY_INCOME, "");
        config.put(MANUAL_CREDIT_CHECK_OUTCOME, "??");
        return config;
    }

    private Map<String, Object> buildConfigData() {
        Map<String, Object> configData = new HashMap<>();
        configData.put(MANUAL_CREDIT_CHECK_OUTCOME, buildCreditCheckOutcomeConfig());
        configData.put(MONTHLY_INCOME, buildMonthlyIncomeConfig());
        return configData;
    }

    private Map<String, Object> buildCreditCheckOutcomeConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(POSITION, 1);
        config.put("type", "select");
        config.put("values", APPROVAL_ARRAY);
        return config;
    }

    private Map<String, Object> buildMonthlyIncomeConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(POSITION, 2);
        config.put("type", "number");
        config.put("required", true);
        return config;
    }

    private Map<String, Object> setTaskConfigData(int position, String type, String[] values) {
        Map<String, Object> firstMap = new HashMap<>();
        firstMap.put(POSITION, position);
        firstMap.put("type", type);
        firstMap.put("values", values);
        return firstMap;
    }

    private Map<String, Object> getCustomerToAdditionalInfoMap(UUID customerId) {
        final CustomerDto customerDto = customerService.getCustomer(customerId);
        Map<String, Object> addInfo = new HashMap<>();
        return addCustomerToAdditionalInfo(addInfo, customerDto);
    }

    public Map<String, Object> addCustomerToAdditionalInfo(Map<String, Object> addInfo, CustomerDto customerDto) {
        Map<String, String> customerMap = new HashMap<>();
        customerMap.put("Name", ((customerDto.getFirstname() != null ? customerDto.getFirstname() : "") + " " + (customerDto.getLastname() != null ? customerDto.getLastname() : "")).trim());
        customerMap.put("Email", customerDto.getEmail() != null ? customerDto.getEmail() : null);
        customerMap.put("Telephone", customerDto.getPhoneNumber() != null ? customerDto.getPhoneNumber() : null);
        customerMap.put("Date of birth", customerDto.getDateOfBirth() != null ? customerDto.getDateOfBirth().toString() : null);
        customerMap.put("Gender", customerDto.getGender() != null ? customerDto.getGender().toString() : null);
        addInfo.put("customer", customerMap);
        return addInfo;
    }

}
