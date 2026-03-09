package org.service_b.workflow.workflow.dto;

import lombok.Data;

@Data
public class VariableDto {
    private String id;
    private String processId;
    private String variableName;
    private String variableValue;
    private String variableType;
}
