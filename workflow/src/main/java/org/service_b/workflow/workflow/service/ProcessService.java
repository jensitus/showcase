package org.service_b.workflow.workflow.service;

import lombok.RequiredArgsConstructor;
import java.util.Optional;
import java.util.UUID;

import org.service_b.workflow.insurance.persistence.Insurance;
import org.service_b.workflow.insurance.persistence.InsuranceRepository;
import org.service_b.workflow.workflow.domain.ResponsibleForType;
import org.service_b.workflow.workflow.dto.ProcessDto;
import org.service_b.workflow.workflow.dto.ProcessInstanceWithVariableDto;
import org.service_b.workflow.workflow.mapper.ProcessMapper;
import org.service_b.workflow.workflow.persistence.entity.ProcessEntity;
import org.service_b.workflow.workflow.persistence.entity.Processable;
import org.service_b.workflow.workflow.persistence.entity.VariableEntity;
import org.service_b.workflow.workflow.persistence.repository.ProcessRepository;
import org.service_b.workflow.workflow.persistence.repository.VariableRepository;
import org.service_b.workflow.workflow.persistence.validation.ProcessableEntityValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessRepository processRepository;
    private final InsuranceRepository insuranceRepository;
    private final VariableRepository variableRepository;
    private final ProcessMapper processMapper;
    private final ProcessableEntityValidator validator;

    public ProcessDto saveProcess(ProcessInstanceWithVariableDto processInstanceWithVariableDto, Processable processable) {
        if (processable.getId() == null) {
            throw new IllegalArgumentException("Entity must have an ID");
        }

        validator.validateExists(processable.getType(), processable.getId());

        ProcessEntity processEntity = getProcessEntity(processInstanceWithVariableDto, processable);

        ProcessEntity saved = processRepository.save(processEntity);
        return processMapper.toDto(saved);
    }

    public ProcessDto getProcessByProcessInstanceId(String processInstanceId) {
        ProcessEntity byProcessInstanceId = processRepository.findByProcessInstanceId(processInstanceId);
        return processMapper.toDto(byProcessInstanceId);
    }

    public Optional<String> findProcessInstanceIdByInsuranceId(UUID insuranceId) {
        return processRepository.findByResponsibleForId(insuranceId)
                                .map(ProcessEntity::getProcessInstanceId);
    }

    public Optional<Insurance> findInsuranceByProcessInstanceId(String processInstanceId) {
        return Optional.ofNullable(processRepository.findByProcessInstanceId(processInstanceId))
                       .filter(e -> e.getResponsibleForType() == ResponsibleForType.INSURANCE)
                       .flatMap(e -> insuranceRepository.findById(e.getResponsibleForId()));
    }

    public Processable getResponsibleEntity(ProcessEntity processEntity) {
        return switch (processEntity.getResponsibleForType()) {
            case INSURANCE -> insuranceRepository.findById(processEntity.getResponsibleForId()).orElseThrow();
            case TODO, CLAIM, CONTRACT -> null;
        };
    }

    private static ProcessEntity getProcessEntity(ProcessInstanceWithVariableDto processInstanceWithVariableDto, Processable processable) {
        ProcessEntity processEntity = new ProcessEntity();
        processEntity.setProcessDefinitionId(processInstanceWithVariableDto.getDefinitionId());
        processEntity.setProcessInstanceId(processInstanceWithVariableDto.getId());
        processEntity.setTenantId(processInstanceWithVariableDto.getTenantId());
        // Link process to a single Insurance (one process -> one insurance)
        processEntity.setResponsibleForType(processable.getType());
        processEntity.setResponsibleForId(processable.getId());
        return processEntity;
    }

}
