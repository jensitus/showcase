package org.service_b.workflow.shared.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.SortedMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

@Service
public class SortedMapConverter implements AttributeConverter<SortedMap<String, HashMap<String, String>>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final SortedMap<String, HashMap<String, String>> stringSortedMap) {
        try {
            return OBJECT_MAPPER.writeValueAsString(stringSortedMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Named("mapToPojo")
    public SortedMap<String, HashMap<String, String>> convertToEntityAttribute(final String s) {
        if (s == null) {
            return Collections.emptySortedMap();
        }
        TypeReference<SortedMap<String, HashMap<String, String>>> typeReference = new TypeReference<>() {};
        try {
            return OBJECT_MAPPER.readValue(s, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
