package org.service_b.workflow.workflow.persistence.validation;

import jakarta.persistence.EntityNotFoundException;
import org.service_b.workflow.submission.persistence.SubmissionRepository;
import org.service_b.workflow.workflow.domain.ResponsibleForType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProcessableEntityValidator {
    private final SubmissionRepository submissionRepository;

    public ProcessableEntityValidator(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
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
            case SUBMISSION -> submissionRepository.existsById(id);
            case INSURANCE, TODO, CLAIM, CONTRACT -> false;
        };
    }
}
