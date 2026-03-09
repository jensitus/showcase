package org.service_b.workflow.workflow.persistence.repository;

import org.service_b.workflow.workflow.persistence.entity.ProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessRepository extends JpaRepository<ProcessEntity, UUID> {
    ProcessEntity findByProcessInstanceId(String processInstanceId);

    Optional<ProcessEntity> findByResponsibleForId(UUID responsibleForId);
}
