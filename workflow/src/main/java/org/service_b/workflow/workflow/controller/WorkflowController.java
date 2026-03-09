package org.service_b.workflow.workflow.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.service_b.workflow.insurance.dto.RequestedContractDto;
import org.service_b.workflow.insurance.service.InsuranceService;
import org.service_b.workflow.workflow.MonthlyIncomeException;
import org.service_b.workflow.workflow.dto.CompleteTaskDto;
import org.service_b.workflow.workflow.dto.ProcessInstanceDto;
import org.service_b.workflow.workflow.dto.ProcessDefinitionDto;
import org.service_b.workflow.workflow.dto.RequestedInsuranceDto;
import org.service_b.workflow.workflow.service.InsuranceWorkflowService;
import org.service_b.workflow.workflow.service.Message;

import org.service_b.workflow.workflow.service.RestClientService;
import org.service_b.workflow.workflow.rest.CamundaEngineRestClient;
import org.service_b.workflow.workflow.rest.CamundaEngineRestClient.FlowNodeLike;
import org.service_b.workflow.workflow.service.StartProcessBody;
import org.service_b.workflow.workflow.service.VersionedDiagramResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final InsuranceWorkflowService insuranceWorkflowService;
    private final InsuranceService insuranceService;
    private final RestClientService restClient;
    private final CamundaEngineRestClient camundaEngineRestClient;

    @PostMapping
    public ResponseEntity<Message> requestInsurance(@RequestBody RequestedInsuranceDto requestedInsuranceDto) throws Exception {
        insuranceWorkflowService.startInsuranceWorkflow(requestedInsuranceDto);
        return ResponseEntity.accepted().body(new Message("Insurance successfully requested"));
    }

    /**
     * Returns the latest deployed version number for the given process definition key.
     * Uses tenant "insurance" by default to align with current deployment setup.
     */
    @GetMapping("process-definition/{key}/latest-version/tenant-id/{tenant-id}")
    public ResponseEntity<Message> getLatestDeployedVersion(@PathVariable("key") String key, @PathVariable("tenant-id") String tenantId) {
        Integer version = camundaEngineRestClient.getLatestVersionByKey(key, tenantId);
        String text = version != null ? String.valueOf(version) : "";
        return ResponseEntity.ok(new Message(text));
    }

    @PostMapping("complete-task")
    public ResponseEntity<Void> completeTask(@RequestBody CompleteTaskDto completeTaskDto) {
        //insuranceWorkflowService.updateAggregateForTaskCompletion(completeTaskDto);
        for (Map.Entry<String, Object> entry : completeTaskDto.getCompleteVars().entrySet()) {
            log.info(entry.getKey() + ":" + entry.getValue());
        }
        return ResponseEntity.ok()
                             .build();
    }

    @GetMapping("by-definition-key/{key}")
    public ResponseEntity<List<ProcessInstanceDto>> getProcessInstances(@PathVariable("key") String key) {
        // Default tenant aligned with current deployments
        String tenantId = "insurance";
        List<CamundaEngineRestClient.ProcessInstanceRef> refs =
                camundaEngineRestClient.listRunningProcessInstancesByDefinitionKey(key, tenantId);

        List<ProcessInstanceDto> result = refs.stream().map(ref -> {
            ProcessInstanceDto dto = new ProcessInstanceDto();
            dto.setId(ref.getId());
            dto.setDefinitionId(ref.getDefinitionId());
            dto.setDefinitionKey(key);
            // businessKey is a string, sanitize to keep digits only as done elsewhere
            if (ref.getBusinessKey() != null) {
                try {
                    dto.setBusinessKey(ref.getBusinessKey().replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {
                    dto.setBusinessKey(null);
                }
            }
            dto.setTenantId(ref.getTenantId());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/definitions/{tenantId}")
    public ResponseEntity<List<ProcessDefinitionDto>> getProcessDefinitions(@PathVariable("tenantId") String tenantId) {
        List<ProcessDefinitionDto> defs = camundaEngineRestClient.getProcessDefinitionsByTenant(tenantId);
        // Enrich each definition with the number of running instances similar to getDiagramAndInstancesByVersion
        for (ProcessDefinitionDto def : defs) {
            try {
                int running = camundaEngineRestClient.listRunningProcessInstancesByDefinitionId(def.getId())
                                                     .size();
                def.setRunningInstances(running);
            } catch (Exception e) {
                // Be resilient: if counting fails for any reason, default to 0
                def.setRunningInstances(0);
            }
        }
        return ResponseEntity.ok(defs);
    }

    @GetMapping("workflow/{key}")
    public ResponseEntity<ProcessInstanceDto> getSingleProcessInstance(@PathVariable("key") String key) {
        CamundaEngineRestClient.ProcessInstanceDetails details = camundaEngineRestClient.getProcessInstanceById(key);
        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        ProcessInstanceDto dto = new ProcessInstanceDto();
        dto.setId(details.getId());
        dto.setDefinitionId(details.getDefinitionId());
        // Resolve definition key (BPMN id) from definition id
        CamundaEngineRestClient.ProcessDefinitionDto def = null;
        if (details.getDefinitionId() != null) {
            def = camundaEngineRestClient.getProcessDefinitionById(details.getDefinitionId());
        }
        if (def != null) {
            dto.setDefinitionKey(def.getKey());
        }
        // Business key is modeled as Long in DTO; try to parse
        try {
            if (details.getBusinessKey() != null) {
                dto.setBusinessKey(details.getBusinessKey().replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ex) {
            dto.setBusinessKey(null);
        }
        dto.setCaseInstanceId(details.getCaseInstanceId());
        dto.setEnded(details.isEnded());
        dto.setSuspended(details.isSuspended());
        dto.setTenantId(details.getTenantId());
        dto.setLinks(details.getLinks());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("variables/{key}")
    public ResponseEntity<Void> getVariables(@PathVariable("key") Long key) {
        // return ResponseEntity.ok(insuranceWorkflowService.getVariables(key));
        return null;
    }

    @GetMapping("diagram/{key}/tenant-id/{tenant-id}")
    public ResponseEntity<Message> getBpmnXML(@PathVariable("key") String key, @PathVariable("tenant-id") String tenantId) {
        // In Camunda 7, {key} here is actually the BPMN process definition key (string). We accept Long for backward compatibility
        String xml = camundaEngineRestClient.getProcessDefinitionXmlByKey(key, tenantId);
        return ResponseEntity.ok(new Message(xml));
    }

    @GetMapping("/diagram/sequence-flows/{key}")
    public ResponseEntity<List<String>> getSequenceFlows(@PathVariable("key") Long key) {
        // For now, return an empty list. Could be enhanced by parsing BPMN and history transitions.
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/diagram/flow-node-instance/{key}")
    public ResponseEntity<List<FlowNodeLike>> getFlowNodeInstance(@PathVariable("key") String key) {
        List<FlowNodeLike> list = camundaEngineRestClient.getHistoryActivityInstances(key);
        return ResponseEntity.ok(list);
    }

    /**
     * Returns running process instances filtered by processDefinitionKey, tenant and version.
     * Example: /workflows/process-instances/insurance_showcase/tenant-id/insurance/version/3
     */
    @GetMapping("process-instances/{processDefinitionKey}/tenant-id/{tenant-id}/version/{version}")
    public ResponseEntity<List<ProcessInstanceDto>> getProcessInstancesByKeyTenantAndVersion(
            @PathVariable("processDefinitionKey") String processDefinitionKey,
            @PathVariable("tenant-id") String tenantId,
            @PathVariable("version") int version) {

        // Resolve concrete process definition id for given key+version(+tenant)
        String defId = camundaEngineRestClient.findProcessDefinitionIdByKeyAndVersion(processDefinitionKey, version, tenantId);
        if (defId == null || defId.isBlank()) {
            // Definition not found; return empty list (404 would also be acceptable, but keep API lenient)
            return ResponseEntity.ok(List.of());
        }

        List<CamundaEngineRestClient.ProcessInstanceRef> refs =
                camundaEngineRestClient.listRunningProcessInstancesByDefinitionId(defId);

        List<ProcessInstanceDto> result = refs.stream().map(ref -> {
            ProcessInstanceDto dto = new ProcessInstanceDto();
            dto.setId(ref.getId());
            dto.setDefinitionId(ref.getDefinitionId());
            dto.setDefinitionKey(processDefinitionKey);
            if (ref.getBusinessKey() != null) {
                try {
                    dto.setBusinessKey(ref.getBusinessKey().replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {
                    dto.setBusinessKey(null);
                }
            }
            dto.setTenantId(ref.getTenantId());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Combined endpoint to fetch a specific version of a BPMN diagram and all running process instances on that version.
     * Uses tenant "insurance" by default.
     */
    @GetMapping("diagram/{key}/version/{version}")
    public ResponseEntity<VersionedDiagramResponse> getDiagramAndInstancesByVersion(@PathVariable("key") String key,
                                                                                    @PathVariable("version") int version) {
        String tenantId = "insurance";
        String xml = camundaEngineRestClient.getProcessDefinitionXmlByKeyAndVersion(key, version, tenantId);
        List<String> running = camundaEngineRestClient.listRunningProcessInstanceIdsByKeyAndVersion(key, version, tenantId);
        return ResponseEntity.ok(new VersionedDiagramResponse(xml, running));
    }

//    @GetMapping("requested-contract/{aggregateId}")
//    public ResponseEntity<RequestedContractDto> getRequestedContracts(@PathVariable("aggregateId") UUID aggregateId) {
//        return ResponseEntity.ok(insuranceService.getRequestedContractsDto(aggregateId));
//    }

    @PostMapping("start-cib/{variable}")
    public ResponseEntity<Void> startCIB(@PathVariable("variable") String variable, @RequestBody StartProcessBody body) {
        restClient.startCib7Process("insurance_showcase", body, "insurance");
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(MonthlyIncomeException.class)
    public ResponseEntity<Message> handleMonthlyIncomeException(MonthlyIncomeException monthlyIncomeException) {
        log.info(monthlyIncomeException.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Message(monthlyIncomeException.getMessage()));
    }

}
