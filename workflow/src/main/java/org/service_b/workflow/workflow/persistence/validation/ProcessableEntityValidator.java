package org.service_b.workflow.workflow.persistence.validation;

import jakarta.persistence.EntityNotFoundException;
import org.service_b.workflow.insurance.persistence.InsuranceRepository;
import org.service_b.workflow.workflow.domain.ResponsibleForType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProcessableEntityValidator {
    private final InsuranceRepository insuranceRepository;

    public ProcessableEntityValidator(InsuranceRepository insuranceRepository) {
        this.insuranceRepository = insuranceRepository;
    }

    public void validateExists(ResponsibleForType type, UUID id) {
        if (!exists(type, id)) {
            throw new EntityNotFoundException(
                    String.format("%s with id %s does not exist", type, id)
            );
        }
    }

    public boolean exists(ResponsibleForType type, UUID id) {
        return switch (type) {
            case INSURANCE -> insuranceRepository.existsById(id);
            case TODO, CLAIM, CONTRACT -> false;
        };
    }
}
