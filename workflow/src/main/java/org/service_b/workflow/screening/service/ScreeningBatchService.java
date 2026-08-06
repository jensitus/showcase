package org.service_b.workflow.screening.service;

import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.BatchSummary;
import org.service_b.workflow.screening.dto.ScreenOutcome;
import org.service_b.workflow.screening.dto.Submission;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.config.ExternalTaskConfig;
import org.service_b.workflow.workflow.dto.FetchAndLock;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import org.service_b.workflow.workflow.dto.Topic;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.service_b.workflow.workflow.service.AbstractExternalTaskService;
import org.service_b.workflow.workflow.service.FetchAndLockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * External-task worker for the {@code novelty_batch} process. It orchestrates the
 * novelty pipeline over a batch of submissions in four steps, mirroring the CFP
 * external-task pattern (one @Scheduled poller per topic, tenant "screening"):
 *
 *   novelty_ingest  -> load the export, normalise to {id,title,abstract}, count
 *   novelty_screen  -> screen in chunks via NoveltyApiClient, checkpointing progress
 *   novelty_report  -> aggregate into results.json + a committee-facing flagged.csv
 *   novelty_notify  -> notify the committee (summary + report location)
 *
 * SKELETON: handler bodies are stubbed. The chunked/checkpointed screening loop and
 * the report writer land in the next step — this commit is for reviewing the shape.
 */
@Service
@Slf4j
public class ScreeningBatchService extends AbstractExternalTaskService {

    private final NoveltyApiClient noveltyApiClient;
    private final NoveltyProperties noveltyProperties;
    private final IngestService ingestService;
    private final ReportService reportService;
    private final ChunkScreeningService chunkScreeningService;
    private final RestTemplate restTemplate;
    private final Map<String, ExternalTaskConfig.TaskDefinition> taskDefinitions;

    public ScreeningBatchService(FetchAndLockService fetchAndLockService,
                                 CibSevenProperties cibSevenProperties,
                                 CibSevenRestClient cibSevenRestClient,
                                 NoveltyApiClient noveltyApiClient,
                                 NoveltyProperties noveltyProperties,
                                 IngestService ingestService,
                                 ReportService reportService,
                                 ChunkScreeningService chunkScreeningService,
                                 RestTemplate restTemplate) {
        super(fetchAndLockService, cibSevenProperties, cibSevenRestClient);
        this.noveltyApiClient = noveltyApiClient;
        this.noveltyProperties = noveltyProperties;
        this.ingestService = ingestService;
        this.reportService = reportService;
        this.chunkScreeningService = chunkScreeningService;
        this.restTemplate = restTemplate;
        this.taskDefinitions = initializeTaskDefinitions();
    }

    private Map<String, ExternalTaskConfig.TaskDefinition> initializeTaskDefinitions() {
        String tenant = noveltyProperties.getTenant();
        Map<String, ExternalTaskConfig.TaskDefinition> defs = new HashMap<>();
        defs.put("ingest", new ExternalTaskConfig.TaskDefinition("novelty_ingest", tenant, this::handleIngest));
        defs.put("report", new ExternalTaskConfig.TaskDefinition("novelty_report", tenant, this::handleReport));
        defs.put("notify", new ExternalTaskConfig.TaskDefinition("novelty_notify", tenant, this::handleNotify));
        return defs;
    }

    @Scheduled(fixedRate = 30000)
    public void ingest() { processExternalTask(taskDefinitions, "ingest"); }

    @Scheduled(fixedRate = 30000)
    public void report() { processExternalTask(taskDefinitions, "report"); }

    @Scheduled(fixedRate = 30000)
    public void notifyCommittee() { processExternalTask(taskDefinitions, "notify"); }

    /**
     * The screen step is handled with its own fetch-lock (not the shared 100 s pattern):
     * a chunk of submissions takes minutes, so we take a longer lock and extend it after
     * each chunk. The chunk loop checkpoints, so if the lock ever expires (worker crash),
     * the task is re-fetched and resumes from the last unfinished chunk.
     */
    @Scheduled(fixedRate = 30000)
    public void screen() {
        Topic topic = new Topic();
        topic.setTopicName("novelty_screen");
        topic.setTenantIdIn(new String[]{noveltyProperties.getTenant()});
        topic.setLockDuration(noveltyProperties.getScreenLockMs());
        FetchAndLock fetchAndLock = new FetchAndLock();
        fetchAndLock.setWorkerId(cibSevenProperties.getWorkerId());
        fetchAndLock.setMaxTasks(1);
        fetchAndLock.setTopics(new Topic[]{topic});

        FetchAndLockResponse response = fetchAndLockService.fetchAndLockExternalTask(fetchAndLock);
        if (response == null || response.getId() == null) {
            return;  // no screen task waiting
        }
        String taskId = response.getId();
        String batchId = strVar(response.getVariables(), "batchId");
        Path workDir = Paths.get(strVar(response.getVariables(), "workDir"));
        if (!noveltyApiClient.isHealthy()) {
            log.warn("[novelty_batch] pipeline not reachable at {} — screen may fail",
                    noveltyProperties.getApiBaseUrl());
        }
        try {
            ScreenOutcome outcome = chunkScreeningService.run(
                    batchId,
                    workDir.resolve("submissions.jsonl"),
                    workDir.resolve("results.jsonl"),
                    () -> extendLock(taskId));
            Map<String, Object> out = new HashMap<>();
            out.put("screened", outcome.screened());
            out.put("flagged", outcome.flagged());
            cibSevenRestClient.completeExternalTask(taskId, out);
            log.info("[novelty_batch] screen done for batch {}: {} screened, {} flagged",
                    batchId, outcome.screened(), outcome.flagged());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[novelty_batch] screen interrupted for batch {} — lock will expire and resume", batchId);
        } catch (Exception e) {
            // leave the task locked to expire -> it is re-fetched and resumes from checkpoint
            log.error("[novelty_batch] screen failed for batch {}: {}", batchId, e.getMessage(), e);
        }
    }

    /** Reset the external-task lock window so a multi-hour run keeps its lock. Best-effort. */
    private void extendLock(String taskId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("workerId", cibSevenProperties.getWorkerId());
            body.put("newDuration", noveltyProperties.getScreenLockMs());
            restTemplate.postForEntity(
                    cibSevenProperties.getBaseUrl() + "/external-task/" + taskId + "/extendLock", body, Void.class);
        } catch (Exception e) {
            log.warn("[novelty_batch] extendLock failed for task {}: {}", taskId, e.getMessage());
        }
    }

    // ── Handlers (SKELETON) ─────────────────────────────────────────────────

    /** Load the export at {@code exportPath}, normalise to {id,title,abstract}, write submissions.jsonl. */
    private Map<String, Object> handleIngest(Map<String, Map<String, Object>> variables) {
        String exportPath = strVar(variables, "exportPath");
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        Path workDir = Paths.get(noveltyProperties.getWorkDir(), batchId);
        log.info("[novelty_batch] ingest batch {} from {}", batchId, exportPath);
        try {
            List<Submission> submissions = ingestService.read(Paths.get(exportPath));
            ingestService.writeJsonl(submissions, workDir.resolve("submissions.jsonl"));
            Map<String, Object> out = new HashMap<>();
            out.put("batchId", batchId);
            out.put("total", submissions.size());
            out.put("workDir", workDir.toString());
            return out;
        } catch (IOException e) {
            throw new RuntimeException("ingest failed for " + exportPath + ": " + e.getMessage(), e);
        }
    }

    /** Aggregate all results into results.json + a committee-facing flagged.csv. */
    private Map<String, Object> handleReport(Map<String, Map<String, Object>> variables) {
        String batchId = strVar(variables, "batchId");
        Path workDir = Paths.get(strVar(variables, "workDir"));
        log.info("[novelty_batch] build report for batch {}", batchId);
        try {
            BatchSummary summary = reportService.build(
                    workDir.resolve("results.jsonl"), workDir.resolve("submissions.jsonl"), workDir);
            Map<String, Object> out = new HashMap<>();
            out.put("reportPath", workDir.resolve("flagged.csv").toString());
            out.put("flaggedCount", summary.getFlagged());
            out.put("total", summary.getTotal());
            return out;
        } catch (IOException e) {
            throw new RuntimeException("report failed for batch " + batchId + ": " + e.getMessage(), e);
        }
    }

    /** Notify the committee: how many flagged of how many, and where the report is. */
    private Map<String, Object> handleNotify(Map<String, Map<String, Object>> variables) {
        Integer flagged = 0;  // TODO read from vars
        log.info("[novelty_batch] notify committee: {} flagged", flagged);
        // TODO: send a summary (mail/SSE) with the report link.
        return new HashMap<>();
    }
}
