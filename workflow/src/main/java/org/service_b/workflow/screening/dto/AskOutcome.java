package org.service_b.workflow.screening.dto;

/**
 * Result of the 35B "adds anything new?" step over a whole batch: how many abstracts it
 * answered (the ones screening let through), how many it called "nothing new" / "only
 * partly new" (both count as flagged), and how many failed even after the pipeline's retry.
 */
public record AskOutcome(int asked, int nothingNew, int partlyNew, int errors) {
}
