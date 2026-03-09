package org.service_b.workflow.workflow.mapper;

import org.mapstruct.Mapper;
import org.service_b.workflow.workflow.dto.ProcessDto;
import org.service_b.workflow.workflow.persistence.entity.ProcessEntity;

@Mapper
public interface ProcessMapper {
    ProcessDto toDto(ProcessEntity processEntity);
    ProcessEntity toEntity(ProcessDto processDto);
}
