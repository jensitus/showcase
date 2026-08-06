package org.service_b.workflow.screening.dto;

/** One flagged submission for the dashboard (mirrors a flagged.csv row). */
public record FlaggedRow(
        String submissionId,
        String verdict,
        String noveltyScore,
        String title,
        String matchedPriorTitle,
        String matchedYear,
        String similarity,
        String rationale) {
}
