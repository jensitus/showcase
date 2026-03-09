package org.service_b.workflow.workflow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MonthlyIncomeException extends RuntimeException {
    private final String message;
}
