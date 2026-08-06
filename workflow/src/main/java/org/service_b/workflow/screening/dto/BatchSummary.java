package org.service_b.workflow.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/** Outcome of building the committee report for one batch. */
@Data
@AllArgsConstructor
public class BatchSummary {
    private int total;
    private int flagged;
    private Map<String, Integer> verdictCounts;
}
