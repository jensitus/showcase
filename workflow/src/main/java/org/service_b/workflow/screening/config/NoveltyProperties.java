package org.service_b.workflow.screening.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Settings for the novelty batch runner. The pipeline API lives on the DGX Spark;
 * for a demo it is reached over the SSH tunnel (local 8090 -> Spark 8080), for a
 * real run via a direct network path — either way just this base URL changes.
 */
@Configuration
@ConfigurationProperties(prefix = "novelty")
@Data
public class NoveltyProperties {
    /** Base URL of the novelty pipeline API (POST /screen, GET /jobs/{id}). */
    private String apiBaseUrl = "http://localhost:8090";
    /** Submissions per screening chunk (checkpoint granularity for a ~10k run). */
    private int chunkSize = 500;
    /** CIB Seven tenant this batch process is deployed under. */
    private String tenant = "screening";
    /** Working directory for per-batch files (submissions, results, report). */
    private String workDir = "data/screening";
    /** External-task lock window for the screen step; extended after each chunk. */
    private int screenLockMs = 300_000;      // 5 min
    /** How often to poll a chunk's screening job. */
    private long pollIntervalMs = 3_000;
    /** Max time to wait for a single chunk before giving up (incident + resume later). */
    private long chunkMaxWaitMs = 1_800_000;  // 30 min
    /** Committee address that receives the completion summary. Empty = log only. */
    private String committeeEmail = "";
}
