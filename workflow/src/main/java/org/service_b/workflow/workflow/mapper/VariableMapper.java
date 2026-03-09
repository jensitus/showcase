package org.service_b.workflow.workflow.mapper;

import org.mapstruct.Mapper;
import org.service_b.workflow.workflow.dto.VariableDto;
import org.service_b.workflow.workflow.persistence.entity.VariableEntity;

@Mapper
public interface VariableMapper {
    VariableDto toDto(VariableEntity variable);
    VariableEntity toEntity(VariableDto variableDto);
}
