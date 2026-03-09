package org.service_b.workflow.workflow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.insurance.persistence.InsuranceState;
import org.service_b.workflow.insurance.service.InsuranceService;
import org.service_b.workflow.workflow.service.ProcessService;
import org.service_b.workflow.workflow.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/process-instances")
@RequiredArgsConstructor
@Slf4j
public class ProcessInstanceController {

    private final ProcessService processService;
    private final InsuranceService insuranceService;
    private final TaskService taskService;

    @GetMapping("/by-insurance/{insuranceId}")
    public ResponseEntity<Map<String, String>> getProcessInstanceIdByInsuranceId(@PathVariable UUID insuranceId) {
        return processService.findProcessInstanceIdByInsuranceId(insuranceId)
                             .map(id -> ResponseEntity.ok(Map.of("processInstanceId", id)))
                             .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{processInstanceId}/state")
    public ResponseEntity<Void> updateProcessState(@PathVariable String processInstanceId,
                                                   @RequestBody Map<String, String> body) {
        String state = body.get("state");
        log.info("Received state update for process {}: {}", processInstanceId, state);

        if ("CANCELLED".equals(state)) {
            taskService.cancelTasksByProcessInstanceId(processInstanceId);
            processService.findInsuranceByProcessInstanceId(processInstanceId)
                          .ifPresentOrElse(
                                  insurance -> {
                                      log.info("Cancelling insurance {} for process {}", insurance.getId(), processInstanceId);
                                      insuranceService.updateInsurance(insurance.getId(), InsuranceState.CANCELLED);
                                  },
                                  () -> log.warn("No insurance found for process instance {}", processInstanceId)
                          );
        }

        return ResponseEntity.ok().build();
    }
}
