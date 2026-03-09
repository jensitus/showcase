package org.service_b.workflow.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.service_b.workflow.insurance.persistence.Insurance;
import org.service_b.workflow.shared.entity.Gender;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerDto {
    UUID id;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Gender gender;
    String firstname;
    String lastname;
    LocalDate dateOfBirth;
    String phoneNumber;
    String email;
    String street;
    String zipCode;
    String city;
    String country;
    int customerNumber;
    Set<Insurance> insurances;

}
