package org.service_b.workflow.screening.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.BatchDetail;
import org.service_b.workflow.screening.dto.BatchListItem;
import org.service_b.workflow.screening.dto.StartBatchRequest;
import org.service_b.workflow.screening.service.ScreeningResultsService;
import org.service_b.workflow.workflow.dto.ProcessInstanceWithVariableDto;
import org.service_b.workflow.workflow.service.RestClientService;
import org.service_b.workflow.workflow.service.StartProcessBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Starts a batch novelty-screening run. Kicks off the {@code novelty_batch} process
 * with the export path; the external-task workers then ingest -> screen -> report ->
 * notify. (Secured by the app's SecurityConfig like other /api endpoints.)
 */
@RestController
@RequestMapping("/api/screening")
@RequiredArgsConstructor
@Slf4j
public class ScreeningController {

    private final RestClientService restClientService;
    private final NoveltyProperties noveltyProperties;
    private final ScreeningResultsService screeningResultsService;

    /** Finished batches (for the dashboard list). */
    @GetMapping("/batches")
    public ResponseEntity<List<BatchListItem>> listBatches() {
        return ResponseEntity.ok(screeningResultsService.listBatches());
    }

    /** One batch's full results (summary + flagged rows). */
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<BatchDetail> getBatch(@PathVariable String batchId) throws IOException {
        return ResponseEntity.ok(screeningResultsService.getBatch(batchId));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, String>> startBatch(@RequestBody StartBatchRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("exportPath", Map.of("value", request.getExportPath(), "type", "String"));

        StartProcessBody body = new StartProcessBody();
        body.setVariables(variables);
        body.setBusinessKey("novelty-batch-" + Instant.now().toEpochMilli());

        ProcessInstanceWithVariableDto process =
                restClientService.startCib7Process("novelty_batch", body, noveltyProperties.getTenant());
        log.info("[novelty_batch] started process {} for export {}", process.getId(), request.getExportPath());

        return ResponseEntity.ok(Map.of("processInstanceId", process.getId()));
    }
}
