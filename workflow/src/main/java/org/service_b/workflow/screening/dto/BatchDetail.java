package org.service_b.workflow.screening.dto;

import java.util.List;
import java.util.Map;

/** Full results of one batch for the dashboard. */
public record BatchDetail(
        String batchId,
        int total,
        int flagged,
        Map<String, Integer> verdictCounts,
        List<FlaggedRow> rows,
        /**
         * The year cutoff every report in this batch was produced under, or null if the
         * batch was screened without one.
         *
         * <p>Shown on the dashboard because it changes what the numbers mean: the same
         * abstracts screened without a cutoff match themselves and come back ~100%
         * duplicates. A reader seeing "22% flagged" needs to know it was measured
         * against prior years only, and the verdicts themselves are the only honest
         * source for that — it is stamped per report by the pipeline.
         */
        Integer beforeYear) {
}
