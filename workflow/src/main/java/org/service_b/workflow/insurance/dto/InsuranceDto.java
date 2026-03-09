package org.service_b.workflow.insurance.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.service_b.workflow.customer.persistence.Customer;
import org.service_b.workflow.insurance.persistence.InsuranceType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InsuranceDto {
    UUID id;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String name;
    String simpleName;
    InsuranceType insuranceType;
    Set<Customer> customers;
}
