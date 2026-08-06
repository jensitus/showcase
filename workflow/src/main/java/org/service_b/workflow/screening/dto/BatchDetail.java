package org.service_b.workflow.screening.dto;

import java.util.List;
import java.util.Map;

/** Full results of one batch for the dashboard. */
public record BatchDetail(
        String batchId,
        int total,
        int flagged,
        Map<String, Integer> verdictCounts,
        List<FlaggedRow> rows) {
}
