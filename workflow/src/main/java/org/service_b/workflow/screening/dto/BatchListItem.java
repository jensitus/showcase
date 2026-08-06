package org.service_b.workflow.screening.dto;

/** One row in the batch list. */
public record BatchListItem(String batchId, int total, int flagged) {
}
