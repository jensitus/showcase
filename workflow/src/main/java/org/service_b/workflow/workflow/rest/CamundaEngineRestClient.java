package org.service_b.workflow.workflow.rest;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minimal client for Camunda 7 engine REST API used by the frontend to render BPMN diagrams.
 */
@Component
@Slf4j
public class CamundaEngineRestClient {

    @Value("${cibseven.base-url}")
    private String engineBaseUrl; // e.g. http://localhost:7000/engine-rest

    private final RestClient restClient;

    /**
     * Spring will inject RestClient.Builder, but we want RestClient itself here.
     */
    public CamundaEngineRestClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * Fetch BPMN 2.0 XML by process definition identifier.
     * Note: In Camunda 7 the {id} is the process definition id (e.g., myProcess:1:abc123).
     * If you need to use a process definition key (BPMN id), call the /key/{key}/xml endpoint instead.
     */
    public String getProcessDefinitionXmlById(String definitionId) {
        BpmnXmlResponse body = get(engineBaseUrl + "/process-definition/" + definitionId + "/xml", BpmnXmlResponse.class);
        return body != null ? body.getBpmn20Xml() : null;
    }

    /**
     * Fetch BPMN 2.0 XML by process definition key (the BPMN process id).
     */
    public String getProcessDefinitionXmlByKey(String definitionKey, String tenantId) {
        BpmnXmlResponse body = get(engineBaseUrl + "/process-definition/key/" + definitionKey + "/tenant-id/" + tenantId + "/xml",
                BpmnXmlResponse.class);
        return body != null ? body.getBpmn20Xml() : null;
    }

    /**
     * Resolve a concrete process definition id by key + version (+ tenant).
     */
    public String findProcessDefinitionIdByKeyAndVersion(String definitionKey, int version, String tenantId) {
        String uri = engineBaseUrl + "/process-definition?key=" + definitionKey + "&version=" + version;
        ProcessDefinitionDto def = getProcessDefinitionByKeyAndVersion(definitionKey, version, tenantId, uri);
        return def.getId();
    }

    /**
     * Fetch BPMN XML by key and version (for a specific tenant if provided).
     */
    public String getProcessDefinitionXmlByKeyAndVersion(String definitionKey, int version, String tenantId) {
        String defId = findProcessDefinitionIdByKeyAndVersion(definitionKey, version, tenantId);
        if (defId == null) return null;
        return getProcessDefinitionXmlById(defId);
    }

    /**
     * Fetch latest deployed process definition for a given key (optionally restricted to tenant).
     */
    public ProcessDefinitionDto getLatestProcessDefinitionByKey(String definitionKey, String tenantId) {
        String uri = engineBaseUrl + "/process-definition?key=" + definitionKey + "&latestVersion=true";
        ProcessDefinitionDto def = getProcessDefinitionByKeyAndVersion(definitionKey, 0, tenantId, uri);
        return def;
    }

    /**
     * Convenience: return the latest deployed version number for a process definition key.
     */
    public Integer getLatestVersionByKey(String definitionKey, String tenantId) {
        ProcessDefinitionDto def = getLatestProcessDefinitionByKey(definitionKey, tenantId);
        return def != null ? def.getVersion() : null;
    }

    /**
     * List all process definitions for a given tenant.
     */
    public List<org.service_b.workflow.workflow.dto.ProcessDefinitionDto> getProcessDefinitionsByTenant(String tenantId) {
        String uri = engineBaseUrl + "/process-definition?tenantIdIn=" + tenantId;
        org.service_b.workflow.workflow.dto.ProcessDefinitionDto[] arr =
                get(uri, org.service_b.workflow.workflow.dto.ProcessDefinitionDto[].class);
        List<org.service_b.workflow.workflow.dto.ProcessDefinitionDto> result = new ArrayList<>();
        if (arr == null) return result;
        for (org.service_b.workflow.workflow.dto.ProcessDefinitionDto d : arr) {
            if (d != null) result.add(d);
        }
        return result;
    }

    /**
     * List running process instances for a given process definition id.
     */
    public List<ProcessInstanceRef> listRunningProcessInstancesByDefinitionId(String definitionId) {
        ProcessInstanceRef[] arr = get(engineBaseUrl + "/process-instance?processDefinitionId=" + definitionId,
                ProcessInstanceRef[].class);
        List<ProcessInstanceRef> result = new ArrayList<>();
        if (arr == null) return result;
        for (ProcessInstanceRef r : arr) {
            if (r != null) result.add(r);
        }
        return result;
    }

    /**
     * List running process instances for a given process definition key (BPMN id), optionally for a tenant.
     */
    public List<ProcessInstanceRef> listRunningProcessInstancesByDefinitionKey(String definitionKey, String tenantId) {
        String uri = engineBaseUrl + "/process-instance?processDefinitionKey=" + definitionKey;
        if (tenantId != null && !tenantId.isBlank()) {
            uri += "&tenantIdIn=" + tenantId;
        }
        ProcessInstanceRef[] arr = get(uri, ProcessInstanceRef[].class);
        List<ProcessInstanceRef> result = new ArrayList<>();
        if (arr == null) return result;
        for (ProcessInstanceRef r : arr) {
            if (r != null) result.add(r);
        }
        return result;
    }

    /**
     * Fetch a single process instance by runtime id.
     */
    public ProcessInstanceDetails getProcessInstanceById(String processInstanceId) {
        return get(engineBaseUrl + "/process-instance/" + processInstanceId, ProcessInstanceDetails.class);
    }

    /**
     * Fetch process definition details by id (to resolve definition key/name).
     */
    public ProcessDefinitionDto getProcessDefinitionById(String definitionId) {
        return get(engineBaseUrl + "/process-definition/" + definitionId, ProcessDefinitionDto.class);
    }

    /**
     * Convenience: List running process instance ids for key+version (+tenant).
     */
    public List<String> listRunningProcessInstanceIdsByKeyAndVersion(String definitionKey, int version, String tenantId) {
        String defId = findProcessDefinitionIdByKeyAndVersion(definitionKey, version, tenantId);
        List<String> ids = new ArrayList<>();
        if (defId == null) return ids;
        for (ProcessInstanceRef ref : listRunningProcessInstancesByDefinitionId(defId)) {
            if (ref.getId() != null) ids.add(ref.getId());
        }
        return ids;
    }

    /**
     * Fetch historic activity instances for a given runtime process instance id.
     * This is used to highlight nodes in the BPMN diagram.
     */
    public List<FlowNodeLike> getHistoryActivityInstances(String processInstanceId) {
        HistoryActivityInstance[] items = get(engineBaseUrl + "/history/activity-instance?processInstanceId=" + processInstanceId,
                HistoryActivityInstance[].class);
        List<FlowNodeLike> result = new ArrayList<>();
        if (items == null) return result;
        for (HistoryActivityInstance hai : items) {
            FlowNodeLike fn = new FlowNodeLike();
            fn.setKey(0L);
            try {
                fn.setProcessInstanceKey(Long.parseLong(processInstanceId));
            } catch (NumberFormatException e) {
                fn.setProcessInstanceKey(0L);
            }
            fn.setProcessDefinitionKey(0L);
            fn.setStartDate(hai.getStartTime());
            fn.setEndDate(hai.getEndTime());
            fn.setFlowNodeId(hai.getActivityId());
            fn.setFlowNodeName(hai.getActivityName());
            fn.setIncidentKey(0L);
            fn.setType(hai.getActivityType());
            fn.setState(hai.getEndTime() == null ? "ACTIVE" : "COMPLETED");
            fn.setIncident(false);
            fn.setTenantId(hai.getTenantId());
            result.add(fn);
        }
        return result;
    }

    private ProcessDefinitionDto getProcessDefinitionByKeyAndVersion(String definitionKey, int version, String tenantId, String uri) {
        if (tenantId != null && !tenantId.isBlank()) {
            uri += "&tenantIdIn=" + tenantId;
        }
        ProcessDefinitionDto[] defs = get(uri, ProcessDefinitionDto[].class);
        if (defs == null || defs.length == 0) return null;
        return defs[0];
    }

    // --- Generic GET helpers ---
    private <T> T get(String uri, Class<T> responseType) {
        try {
            ResponseEntity<T> entity = restClient.get()
                                                 .uri(uri)
                                                 .retrieve()
                                                 .toEntity(responseType);
            return entity.getBody();
        } catch (Exception ex) {
            log.warn("REST GET failed for {}: {}", uri, ex.toString());
            return null;
        }
    }

    @Data
    public static class BpmnXmlResponse {
        private String id;
        private String bpmn20Xml;
    }

    @Data
    public static class HistoryActivityInstance {
        private String id;
        private String parentActivityInstanceId;
        private String activityId;
        private String activityName;
        private String activityType;
        private String processDefinitionId;
        private String processInstanceId;
        private String executionId;
        private String taskId;
        private String calledProcessInstanceId;
        private String calledCaseInstanceId;
        private String assignee;
        private String tenantId;
        private java.util.Date startTime;
        private java.util.Date endTime;
    }

    /**
     * DTO shaped similarly to the frontend FlowNodeInstance interface.
     */
    @Data
    public static class FlowNodeLike {
        private Long key;
        private Long processInstanceKey;
        private Long processDefinitionKey;
        private java.util.Date startDate;
        private java.util.Date endDate;
        private String flowNodeId;
        private String flowNodeName;
        private Long incidentKey;
        private String type;
        private String state;
        private boolean incident;
        private String tenantId;
    }

    @Data
    public static class ProcessDefinitionDto {
        private String id;
        private String key;
        private Integer version;
        private String tenantId;
        private String name;
    }

    @Data
    public static class ProcessInstanceRef {
        private String id;
        private String definitionId;
        private String businessKey;
        private String tenantId;
    }

    @Data
    public static class ProcessInstanceDetails {
        private String id;
        private String definitionId;
        private String businessKey;
        private String caseInstanceId;
        private boolean ended;
        private boolean suspended;
        private String tenantId;
        private java.util.Collection<Object> links;
    }
}
