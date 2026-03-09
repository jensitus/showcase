package org.service_b.workflow.insurance.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class RequestedContractDto {
    String paymentSchedule;
    int amount;
    String insuranceSum;
    String insuranceCoverage;
    int insuranceNumber;
    String name;
}
