package org.service_b.workflow.screening.service;

import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.config.ExternalTaskConfig;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.service_b.workflow.workflow.service.AbstractExternalTaskService;
import org.service_b.workflow.workflow.service.FetchAndLockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, ExternalTaskConfig.TaskDefinition> taskDefinitions;

    public ScreeningBatchService(FetchAndLockService fetchAndLockService,
                                 CibSevenProperties cibSevenProperties,
                                 CibSevenRestClient cibSevenRestClient,
                                 NoveltyApiClient noveltyApiClient,
                                 NoveltyProperties noveltyProperties) {
        super(fetchAndLockService, cibSevenProperties, cibSevenRestClient);
        this.noveltyApiClient = noveltyApiClient;
        this.noveltyProperties = noveltyProperties;
        this.taskDefinitions = initializeTaskDefinitions();
    }

    private Map<String, ExternalTaskConfig.TaskDefinition> initializeTaskDefinitions() {
        String tenant = noveltyProperties.getTenant();
        Map<String, ExternalTaskConfig.TaskDefinition> defs = new HashMap<>();
        defs.put("ingest", new ExternalTaskConfig.TaskDefinition("novelty_ingest", tenant, this::handleIngest));
        defs.put("screen", new ExternalTaskConfig.TaskDefinition("novelty_screen", tenant, this::handleScreen));
        defs.put("report", new ExternalTaskConfig.TaskDefinition("novelty_report", tenant, this::handleReport));
        defs.put("notify", new ExternalTaskConfig.TaskDefinition("novelty_notify", tenant, this::handleNotify));
        return defs;
    }

    @Scheduled(fixedRate = 30000)
    public void ingest() { processExternalTask(taskDefinitions, "ingest"); }

    @Scheduled(fixedRate = 30000)
    public void screen() { processExternalTask(taskDefinitions, "screen"); }

    @Scheduled(fixedRate = 30000)
    public void report() { processExternalTask(taskDefinitions, "report"); }

    @Scheduled(fixedRate = 30000)
    public void notifyCommittee() { processExternalTask(taskDefinitions, "notify"); }

    // ── Handlers (SKELETON) ─────────────────────────────────────────────────

    /** Load the export at {@code exportPath}, normalise to {id,title,abstract}, count. */
    private Map<String, Object> handleIngest(Map<String, Map<String, Object>> variables) {
        String exportPath = strVar(variables, "exportPath");
        log.info("[novelty_batch] ingest from {}", exportPath);
        // TODO: read the export (congress JSON via convert, or .jsonl / .csv),
        //       normalise to a submissions file, count total, init the checkpoint.
        Map<String, Object> out = new HashMap<>();
        out.put("total", 0);              // TODO real count
        out.put("batchId", "TODO");       // TODO a persisted batch id
        return out;
    }

    /**
     * Screen the batch in chunks, checkpointing after each so a ~10k / multi-hour run
     * is resumable. Intended shape:
     * <pre>
     *   for each not-yet-done chunk of {@code chunkSize}:
     *       ScreenResponse job = noveltyApiClient.screenBatch(chunk);
     *       poll noveltyApiClient.getJob(job.getJobId()) until isDone();
     *       append results, mark chunk done (checkpoint table).
     * </pre>
     * NOTE: a chunk can take many minutes — the external-task lock duration must be
     * extended accordingly (the base uses 100s), or each chunk submitted then polled
     * across separate activations. To be finalised in the next step.
     */
    private Map<String, Object> handleScreen(Map<String, Map<String, Object>> variables) {
        String batchId = strVar(variables, "batchId");
        log.info("[novelty_batch] screen batch {} (chunkSize={})", batchId, noveltyProperties.getChunkSize());
        if (!noveltyApiClient.isHealthy()) {
            log.warn("[novelty_batch] pipeline not reachable at {}", noveltyProperties.getApiBaseUrl());
        }
        // TODO: chunked + checkpointed screening loop via noveltyApiClient (see javadoc).
        Map<String, Object> out = new HashMap<>();
        out.put("screened", 0);   // TODO
        out.put("flagged", 0);    // TODO
        return out;
    }

    /** Aggregate all results into results.json + a committee-facing flagged.csv. */
    private Map<String, Object> handleReport(Map<String, Map<String, Object>> variables) {
        String batchId = strVar(variables, "batchId");
        log.info("[novelty_batch] build report for batch {}", batchId);
        // TODO: write results.json (all) + flagged.csv (the flagged ~20%, most-suspicious first).
        Map<String, Object> out = new HashMap<>();
        out.put("reportPath", "TODO");
        out.put("flaggedCount", 0);
        return out;
    }

    /** Notify the committee: how many flagged of how many, and where the report is. */
    private Map<String, Object> handleNotify(Map<String, Map<String, Object>> variables) {
        Integer flagged = 0;  // TODO read from vars
        log.info("[novelty_batch] notify committee: {} flagged", flagged);
        // TODO: send a summary (mail/SSE) with the report link.
        return new HashMap<>();
    }
}
