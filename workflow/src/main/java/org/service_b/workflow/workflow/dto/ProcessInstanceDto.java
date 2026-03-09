package org.service_b.workflow.workflow.dto;

import lombok.Data;

import java.util.Collection;

@Data
public class ProcessInstanceDto {
    private String id;
    private String definitionId;
    private String definitionKey;
    private String businessKey;
    private String caseInstanceId;
    private boolean ended;
    private boolean suspended;
    private String tenantId;
    private Collection<Object> links;
}
