package org.service_b.workflow.workflow.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.service_b.workflow.shared.utils.HashMapConverter;
import org.service_b.workflow.shared.utils.MapObject;
import org.service_b.workflow.shared.utils.SortedMapConverter;
import org.service_b.workflow.workflow.dto.CreateTaskRequest;
import org.service_b.workflow.workflow.dto.TaskDto;
import org.service_b.workflow.workflow.dto.TaskUpdateRequest;
import org.service_b.workflow.workflow.persistence.entity.TaskEntity;

import java.util.List;

@Mapper(uses = {HashMapConverter.class, SortedMapConverter.class}, componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TaskMapper {

//    HashMapConverter hashmapConverter = new HashMapConverter();
//    SortedMapConverter sortedMapConverter = new SortedMapConverter();

    @Mapping(target = "additionalInfo", source = "additionalInfo", qualifiedByName = "mapToPojo")
    @Mapping(target = "config", source = "config", qualifiedByName = "mapConfig")
    @Mapping(target = "configData", source = "configData", qualifiedByName = "mapConfig")
    TaskDto toDto(TaskEntity taskEntity);

    TaskEntity toEntity(CreateTaskRequest createTaskRequest);

    List<TaskDto> toDto(List<TaskEntity> taskEntities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "executionId", ignore = true)
    @Mapping(target = "processDefinitionId", ignore = true)
    @Mapping(target = "processInstanceId", ignore = true)
    @Mapping(target = "taskDefinitionKey", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateEntityFromDto(TaskUpdateRequest dto, @MappingTarget TaskEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "executionId", ignore = true)
    @Mapping(target = "processDefinitionId", ignore = true)
    @Mapping(target = "processInstanceId", ignore = true)
    @Mapping(target = "taskDefinitionKey", ignore = true)
    @Mapping(target = "formKey", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "taskState", ignore = true)
    void updateEntityFromEnrichedDto(TaskDto dto, @MappingTarget TaskEntity entity);

//    @Named("mapToPojo")
//    default SortedMap<String, HashMap<String, String>> mapToPojo(String additionalInfo) {
//        return sortedMapConverter.convertToEntityAttribute(additionalInfo);
//    }

//    @Named("mapConfig")
//    default Map<String, Object> mapConfig(String config) {
//        return hashmapConverter.convertToEntityAttribute(config);
//    }

}
