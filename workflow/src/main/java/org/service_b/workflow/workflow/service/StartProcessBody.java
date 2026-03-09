package org.service_b.workflow.workflow.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class StartProcessBody {
    private Map<String, Object> variables;
    private String businessKey;
}
