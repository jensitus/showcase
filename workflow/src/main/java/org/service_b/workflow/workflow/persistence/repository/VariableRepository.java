package org.service_b.workflow.workflow.persistence.repository;

import org.service_b.workflow.workflow.persistence.entity.VariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VariableRepository extends JpaRepository<VariableEntity, UUID> {
    VariableEntity findByProcessInstanceIdAndVariableNameAndProcessId(
            String processInstanceId,
            String variableName,
            UUID processId);
}
