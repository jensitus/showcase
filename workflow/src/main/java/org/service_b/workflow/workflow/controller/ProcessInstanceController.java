package org.service_b.workflow.workflow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.submission.persistence.SubmissionState;
import org.service_b.workflow.submission.service.SubmissionService;
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
    private final SubmissionService submissionService;
    private final TaskService taskService;

    @GetMapping("/by-submission/{submissionId}")
    public ResponseEntity<Map<String, String>> getProcessInstanceIdBySubmissionId(@PathVariable UUID submissionId) {
        return processService.findProcessInstanceIdBySubmissionId(submissionId)
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
        }

        return ResponseEntity.ok().build();
    }
}
