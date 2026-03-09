package org.service_b.workflow.workflow.dto;

import lombok.Data;

@Data
public class Topic {
    private String topicName;
    private int lockDuration;
    private String[] tenantIdIn;
}
