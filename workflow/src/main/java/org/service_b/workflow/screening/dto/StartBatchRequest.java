package org.service_b.workflow.screening.dto;

import lombok.Data;

/** Body for POST /api/screening/batch — where the submissions export lives. */
@Data
public class StartBatchRequest {
    /** Path to the submissions export (.jsonl / .json / .csv) the runner ingests. */
    private String exportPath;
}
