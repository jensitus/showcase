package org.service_b.workflow.workflow.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessDefinitionDto {
    String id;
    String key;
    String category;
    String description;
    String name;
    int version;
    String resource;
    String deploymentId;
    String diagram;
    boolean suspended;
    String tenantId;
    String versionTag;
    int historyTimeToLive;
    boolean startableInTasklist;
    // Computed field: number of currently running process instances for this definition
    int runningInstances;
}
