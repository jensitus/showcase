package org.service_b.workflow.shared.utils;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

@Service
public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final Map<String, Object> stringObjectMap) {

        try {
            return OBJECT_MAPPER.writeValueAsString(stringObjectMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Named("mapConfig")
    public Map<String, Object> convertToEntityAttribute(final String s) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        try {
            return OBJECT_MAPPER.readValue(s, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
