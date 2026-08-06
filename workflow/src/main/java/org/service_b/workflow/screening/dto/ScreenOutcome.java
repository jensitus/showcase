package org.service_b.workflow.screening.dto;

/** Result of screening a whole batch. */
public record ScreenOutcome(int screened, int flagged) {
}
