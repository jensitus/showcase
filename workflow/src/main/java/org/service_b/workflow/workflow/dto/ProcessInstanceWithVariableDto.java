package org.service_b.workflow.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProcessInstanceWithVariableDto {
    private Map<String, VariableValueDto> variables;
    private String id;
    private String definitionId;
    private String definitionKey;
    private String businessKey;
    private String caseInstanceId;
    private Boolean ended;
    private Boolean suspended;
    private String tenantId;
    private Object links;
}
