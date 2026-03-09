package org.service_b.workflow.shared.utils;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import org.springframework.stereotype.Service;

@Service
public class DataItemConverter implements AttributeConverter<List<DataItemGroup>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final List<DataItemGroup> dataItemGroups) {
        try {
            return OBJECT_MAPPER.writeValueAsString(dataItemGroups);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DataItemGroup> convertToEntityAttribute(final String s) {
        TypeReference<List<DataItemGroup>> typeReference = new TypeReference<List<DataItemGroup>>() {};
        try {
            return OBJECT_MAPPER.readValue(s, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
