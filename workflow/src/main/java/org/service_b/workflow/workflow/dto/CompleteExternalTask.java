package org.service_b.workflow.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CompleteExternalTask {
    private String workerId;
    private Map<String, VariableValueDto> variables;
    private Map<String, VariableValueDto> localVariables;
}
