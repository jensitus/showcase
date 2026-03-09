package org.service_b.workflow.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

@Service
public class MapObject {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SortedMap<String, HashMap<String, String>> convertSortedMapToEntityAttribute(final String s) {
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

    @Named("mapConfig")
    public Map<String, Object> convertHashMapToEntityAttribute(final String s) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        try {
            return OBJECT_MAPPER.readValue(s, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
