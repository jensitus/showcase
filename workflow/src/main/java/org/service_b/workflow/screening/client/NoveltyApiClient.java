package org.service_b.workflow.screening.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.JobStatus;
import org.service_b.workflow.screening.dto.ScreenRequest;
import org.service_b.workflow.screening.dto.ScreenResponse;
import org.service_b.workflow.screening.dto.Submission;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Thin client for the novelty pipeline API on the Spark. The pipeline already does
 * all the screening; this just submits a batch and polls the job.
 *
 *   POST /screen        -> { job_id, total }
 *   GET  /jobs/{job_id} -> { status, done, total, results[] }
 *   GET  /health        -> liveness
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NoveltyApiClient {

    private final RestTemplate restTemplate;
    private final NoveltyProperties props;

    /**
     * Queue one chunk of submissions for screening; returns the job handle.
     *
     * @param corpus     which corpus to screen against, or null for the pipeline's
     *                   default (wrong for congress runs — see ScreenRequest#getCorpus)
     * @param beforeYear compare only against strictly earlier years, or null for no
     *                   cutoff. Logged explicitly because a wrong or dropped cutoff is
     *                   invisible in the results otherwise — the run simply comes back
     *                   with every abstract matching itself.
     */
    public ScreenResponse screenBatch(List<Submission> submissions, String corpus,
                                     Integer beforeYear) {
        String url = props.getApiBaseUrl() + "/screen";
        log.info("POST {} ({} submissions, corpus={}, beforeYear={})", url, submissions.size(),
                corpus == null ? "pipeline default" : corpus,
                beforeYear == null ? "none" : beforeYear);
        ScreenRequest body = new ScreenRequest();
        body.setSubmissions(submissions);
        body.setCorpus(corpus);
        body.setBeforeYear(beforeYear);
        ScreenResponse resp = restTemplate.postForObject(url, body, ScreenResponse.class);
        if (beforeYear != null && resp != null && resp.getBeforeYear() == null) {
            throw new IllegalStateException(
                    "pipeline at " + props.getApiBaseUrl() + " did not echo before_year="
                    + beforeYear + " — it is too old to apply the cutoff and would screen "
                    + "every abstract against a corpus containing itself");
        }
        return resp;
    }

    /** Poll a screening job. Caller loops until {@link JobStatus#isDone()}. */
    public JobStatus getJob(String jobId) {
        return restTemplate.getForObject(props.getApiBaseUrl() + "/jobs/" + jobId, JobStatus.class);
    }

    /** Liveness check before starting a long run. */
    public boolean isHealthy() {
        try {
            restTemplate.getForObject(props.getApiBaseUrl() + "/health", String.class);
            return true;
        } catch (Exception e) {
            log.warn("novelty pipeline health check failed at {}: {}", props.getApiBaseUrl(), e.getMessage());
            return false;
        }
    }
}
