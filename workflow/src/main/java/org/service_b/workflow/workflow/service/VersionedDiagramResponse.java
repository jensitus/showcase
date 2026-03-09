package org.service_b.workflow.workflow.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload containing the BPMN XML for a specific process definition version
 * and all running process instances that belong to that version.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionedDiagramResponse {
    /**
     * The BPMN 2.0 XML content of the process definition version.
     */
    private String text;

    /**
     * List of running process instance IDs that are using this process definition version.
     */
    private List<String> processInstanceIds;
}
