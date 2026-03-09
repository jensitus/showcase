package org.service_b.workflow.workflow.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.workflow.dto.*;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RestClientService {

    @Value("${tasklist.base-url}")
    private String taskListUrl;

    @Value("${cibseven.base-url}")
    private String cibsevenBaseUrl;

    private final RestClient restClient;

    public RestClientService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void sendTaskToList(TaskDto taskDto) {
        ResponseEntity<Void> response = restClient.post()
                                                  .uri(taskListUrl + "/tasks")
                                                  .body(taskDto)
                                                  .retrieve()
                                                  .toBodilessEntity();
        log.info(response.toString());
    }

    public void informTaskListAboutCompletedUserTask(CompleteTaskEvent completeTaskEvent) {
        ResponseEntity<Void> response = restClient.post()
                                                  .uri(taskListUrl + "/tasks/complete")
                                                  .body(completeTaskEvent)
                                                  .retrieve()
                                                  .toBodilessEntity();
        log.info(response.toString());
    }

    public ProcessInstanceWithVariableDto startCib7Process(String processDefinitionKey, StartProcessBody body, String tenantId) {
        ResponseEntity<ProcessInstanceWithVariableDto> entity = restClient.post()
                                                                          .uri(cibsevenBaseUrl + "/process-definition/key/" + processDefinitionKey + "/tenant-id/" + tenantId + "/start")
                                                                          .body(body)
                                                                          .retrieve()
                                                                          .toEntity(ProcessInstanceWithVariableDto.class);
        return entity.getBody();
    }

    public FetchAndLockResponse fetchAndLock(FetchAndLock fetchAndLock) {
        ResponseEntity<FetchAndLockResponse[]> entity = restClient.post()
                                                                  .uri(cibsevenBaseUrl + "/external-task/fetchAndLock")
                                                                  .body(fetchAndLock)
                                                                  .retrieve()
                                                                  .toEntity(FetchAndLockResponse[].class);
        log.info(entity.toString());
        FetchAndLockResponse[] body = entity.getBody();
        if (body != null) return body[0];
        return null;
    }

    public ResponseEntity<Void> completeExternalTask(String taskId, CompleteExternalTaskRequest completeExternalTaskRequest) {
        ResponseEntity<Void> response = restClient.post()
                                                  .uri(cibsevenBaseUrl + "/external-task/" + taskId + "/complete")
                                                  .body(completeExternalTaskRequest)
                                                  .retrieve()
                                                  .toBodilessEntity();
        log.info(response.toString());
        return response;
    }

    public ResponseEntity<Void> completeUserTask(String taskId, Map<String, Object> variables) {
        CompleteUserTaskRequest request = new CompleteUserTaskRequest();
        if (variables != null && !variables.isEmpty()) {
            Map<String, CamundaVariable> camundaVariables = new HashMap<>();
            variables.forEach((key, value) ->
                                      camundaVariables.put(key, createCamundaVariable(value))
            );
            request.setVariables(camundaVariables);
        }
        ResponseEntity<Void> response = restClient.post()
                                                  .uri(taskListUrl + "/task/" + taskId + "/complete")
                                                  .body(request)
                                                  .retrieve()
                                                  .toBodilessEntity();
        return response;
    }

    @Data
    public static class CompleteExternalTaskRequest {
        private String workerId;
        private Map<String, CamundaVariable> variables;
        private Map<String, CamundaVariable> localVariables;
    }

    @Data
    public static class CompleteUserTaskRequest {
        private Map<String, CamundaVariable> variables;
        private boolean withVariablesInReturn;
    }

    @Data
    private static class CamundaVariable {
        private Object value;
        private String type;
    }

    private CamundaVariable createCamundaVariable(Object value) {
        CamundaVariable variable = new CamundaVariable();
        variable.setValue(value);
        variable.setType(detectType(value));
        return variable;
    }

    private String detectType(Object value) {
        if (value == null) return "Null";
        if (value instanceof String) return "String";
        if (value instanceof Integer) return "Integer";
        if (value instanceof Long) return "Long";
        if (value instanceof Double || value instanceof Float) return "Double";
        if (value instanceof Boolean) return "Boolean";
        if (value instanceof java.util.Date) return "Date";
        return "Json";
    }

}
