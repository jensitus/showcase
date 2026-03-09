package org.service_b.workflow.workflow.dto;

import java.util.UUID;

import org.service_b.workflow.insurance.persistence.InsuranceType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestedInsuranceDto {
    InsuranceType insuranceType;
    UUID customerId;
    boolean mudslideRisk;
    boolean floodRisk;
    boolean sufficientIncome;
    String insuranceCoverage;
    String insuranceSum;
    String paymentSchedule;
    int amount;
}
