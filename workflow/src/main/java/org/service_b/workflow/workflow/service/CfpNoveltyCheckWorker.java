package org.service_b.workflow.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.submission.persistence.Submission;
import org.service_b.workflow.submission.persistence.SubmissionNovelty;
import org.service_b.workflow.submission.service.NoveltyCheckService;
import org.service_b.workflow.submission.service.SubmissionService;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.dto.FetchAndLock;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import org.service_b.workflow.workflow.dto.Topic;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * External-task worker for {@code cfp_novelty_check}: a valid submission goes to the novelty
 * pipeline on the Spark before reviewers are assigned (see {@link NoveltyCheckService}).
 *
 * <p>Not the shared 100 s pattern: screening plus the larger model can take minutes, so it
 * takes a longer lock and extends it while waiting. The Spark is reached over a tunnel that
 * may be down: a failure is retried {@value #RETRIES} times, {@value #RETRY_MS} ms apart, and
 * after that the check is recorded as unavailable and the task completed, so the submission
 * still reaches Assign Reviewers — where the chair sees that the check did not run.
 */
@Service
@Slf4j
public class CfpNoveltyCheckWorker {

    static final String TOPIC = "cfp_novelty_check";
    static final String TENANT = "cfp";
    static final int RETRIES = 3;
    static final long RETRY_MS = 300_000;   // 5 min: ~15 min in all before giving up

    private final FetchAndLockService fetchAndLockService;
    private final CibSevenProperties cibSevenProperties;
    private final CibSevenRestClient cibSevenRestClient;
    private final NoveltyProperties noveltyProperties;
    private final SubmissionService submissionService;
    private final NoveltyCheckService noveltyCheckService;
    private final RestTemplate restTemplate;

    public CfpNoveltyCheckWorker(FetchAndLockService fetchAndLockService,
                                 CibSevenProperties cibSevenProperties,
                                 CibSevenRestClient cibSevenRestClient,
                                 NoveltyProperties noveltyProperties,
                                 SubmissionService submissionService,
                                 NoveltyCheckService noveltyCheckService,
                                 RestTemplate restTemplate) {
        this.fetchAndLockService = fetchAndLockService;
        this.cibSevenProperties = cibSevenProperties;
        this.cibSevenRestClient = cibSevenRestClient;
        this.noveltyProperties = noveltyProperties;
        this.submissionService = submissionService;
        this.noveltyCheckService = noveltyCheckService;
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRate = 30000)
    public void checkNovelty() {
        Topic topic = new Topic();
        topic.setTopicName(TOPIC);
        topic.setTenantIdIn(new String[]{TENANT});
        topic.setLockDuration(noveltyProperties.getAskLockMs());
        FetchAndLock fetchAndLock = new FetchAndLock();
        fetchAndLock.setWorkerId(cibSevenProperties.getWorkerId());
        fetchAndLock.setMaxTasks(1);
        fetchAndLock.setTopics(new Topic[]{topic});

        FetchAndLockResponse response = fetchAndLockService.fetchAndLockExternalTask(fetchAndLock);
        if (response == null || response.getId() == null) {
            return;  // nothing waiting
        }
        String taskId = response.getId();
        Submission submission = submissionService.getSubmission(
                UUID.fromString((String) response.getVariables().get("submissionId").get("value")));
        try {
            SubmissionNovelty result = noveltyCheckService.check(submission,
                    () -> extendLock(taskId, noveltyProperties.getAskLockMs()));
            cibSevenRestClient.completeExternalTask(taskId, variables(result));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[novelty] check interrupted for submission {} — the lock expires and it runs again", submission.getId());
        } catch (Exception e) {
            int left = retriesLeft(response.getRetries());
            if (left > 0) {
                log.warn("[novelty] check failed for submission {} ({} retries left): {}", submission.getId(), left, e.getMessage());
                cibSevenRestClient.handleTaskFailure(taskId, cibSevenProperties.getWorkerId(),
                        "novelty pipeline: " + e.getMessage(), String.valueOf(e), left, RETRY_MS);
            } else {
                log.error("[novelty] pipeline unavailable for submission {} after {} retries — continuing without the check: {}",
                        submission.getId(), RETRIES, e.getMessage());
                SubmissionNovelty unavailable = noveltyCheckService.markUnavailable(submission, e.getMessage());
                cibSevenRestClient.completeExternalTask(taskId, variables(unavailable));
            }
        }
    }

    /** The engine reports no retries on the first attempt; after a failure, what we set. */
    static int retriesLeft(String retries) {
        return retries == null || retries.isBlank() ? RETRIES : Integer.parseInt(retries) - 1;
    }

    private static Map<String, Object> variables(SubmissionNovelty n) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("novelty_status", n.getStatus().name());
        vars.put("novelty_verdict", n.getVerdict() == null ? "" : n.getVerdict());
        vars.put("novelty_adds_new", n.getAddsNew() == null ? "" : n.getAddsNew());
        vars.put("novelty_flagged", Boolean.TRUE.equals(n.getFlagged()));
        return vars;
    }

    /** Reset the external-task lock window while the larger model thinks. Best-effort. */
    private void extendLock(String taskId, int lockMs) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("workerId", cibSevenProperties.getWorkerId());
            body.put("newDuration", lockMs);
            restTemplate.postForEntity(
                    cibSevenProperties.getBaseUrl() + "/external-task/" + taskId + "/extendLock", body, Void.class);
        } catch (Exception e) {
            log.warn("[novelty] extendLock failed for task {}: {}", taskId, e.getMessage());
        }
    }
}
