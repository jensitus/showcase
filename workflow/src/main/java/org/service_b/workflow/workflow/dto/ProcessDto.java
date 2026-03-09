package org.service_b.workflow.workflow.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProcessDto {
    private UUID id;
    private String name;
    private String processInstanceId;
    private String processDefinitionId;
    private String tenantId;
}
