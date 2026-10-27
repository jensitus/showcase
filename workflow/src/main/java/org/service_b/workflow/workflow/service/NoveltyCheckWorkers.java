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
 * External-task workers for the {@code novelty_check} process (called by the call-for-papers
 * process for a valid submission), one poller per step:
 *
 *   novelty_check_screen      screen against PubMed            -> novelty_screening_flagged
 *   novelty_check_ask         the larger model, if not flagged -> novelty_adds_new
 *   novelty_check_unavailable record that the pipeline stayed unreachable
 *
 * <p>Not the shared 100 s pattern: a step can take minutes (the first /check after a pipeline
 * restart ~75 s; the larger model up to ~90 s), so each takes a longer lock, extended while
 * waiting. The Spark sits behind a tunnel that may be down: a failure is retried
 * {@value #RETRIES} times, {@value #RETRY_MS} ms apart; then the step throws the BPMN error
 * {@value #UNAVAILABLE_ERROR}, which the process's event subprocess turns into "check
 * unavailable" — and the submission goes on to Assign Reviewers.
 */
@Service
@Slf4j
public class NoveltyCheckWorkers {

    static final String TENANT = "cfp";
    static final String UNAVAILABLE_ERROR = "PIPELINE_UNAVAILABLE";
    static final int RETRIES = 3;
    static final long RETRY_MS = 300_000;   // 5 min: ~15 min in all before giving up

    private final FetchAndLockService fetchAndLockService;
    private final CibSevenProperties cibSevenProperties;
    private final CibSevenRestClient cibSevenRestClient;
    private final NoveltyProperties noveltyProperties;
    private final SubmissionService submissionService;
    private final NoveltyCheckService noveltyCheckService;
    private final RestTemplate restTemplate;

    public NoveltyCheckWorkers(FetchAndLockService fetchAndLockService,
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

    /** One step's work: the submission and a lock keep-alive in, process variables out. */
    @FunctionalInterface
    private interface Step {
        Map<String, Object> run(Submission submission, Runnable keepAlive) throws Exception;
    }

    @Scheduled(fixedRate = 30000)
    public void screen() {
        process("novelty_check_screen", (submission, keepAlive) -> {
            SubmissionNovelty n = noveltyCheckService.screen(submission);
            Map<String, Object> vars = variables(n);
            vars.put("novelty_screening_flagged", Boolean.TRUE.equals(n.getFlagged()));
            return vars;
        });
    }

    @Scheduled(fixedRate = 30000)
    public void ask() {
        process("novelty_check_ask", (submission, keepAlive) -> variables(noveltyCheckService.ask(submission, keepAlive)));
    }

    @Scheduled(fixedRate = 30000)
    public void recordUnavailable() {
        FetchAndLockResponse task = fetch("novelty_check_unavailable");
        if (task == null) {
            return;
        }
        Submission submission = submission(task);
        Map<String, Object> error = task.getVariables().get("novelty_error");
        SubmissionNovelty n = noveltyCheckService.markUnavailable(submission,
                error == null ? null : String.valueOf(error.get("value")));
        cibSevenRestClient.completeExternalTask(task.getId(), variables(n));
    }

    private void process(String topicName, Step step) {
        FetchAndLockResponse task = fetch(topicName);
        if (task == null) {
            return;
        }
        String taskId = task.getId();
        Submission submission = submission(task);
        try {
            Map<String, Object> vars = step.run(submission,
                    () -> extendLock(taskId, noveltyProperties.getAskLockMs()));
            cibSevenRestClient.completeExternalTask(taskId, vars);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[novelty] {} interrupted for submission {} — the lock expires and it runs again",
                    topicName, submission.getId());
        } catch (Exception e) {
            int left = retriesLeft(task.getRetries());
            if (left > 0) {
                log.warn("[novelty] {} failed for submission {} ({} retries left): {}",
                        topicName, submission.getId(), left, e.getMessage());
                cibSevenRestClient.handleTaskFailure(taskId, cibSevenProperties.getWorkerId(),
                        "novelty pipeline: " + e.getMessage(), String.valueOf(e), left, RETRY_MS);
            } else {
                log.error("[novelty] {} gave up for submission {} after {} retries: {}",
                        topicName, submission.getId(), RETRIES, e.getMessage());
                cibSevenRestClient.handleBpmnError(taskId, UNAVAILABLE_ERROR, e.getMessage());
            }
        }
    }

    /** The engine reports no retries on the first attempt; after a failure, what we set. */
    static int retriesLeft(String retries) {
        return retries == null || retries.isBlank() ? RETRIES : Integer.parseInt(retries) - 1;
    }

    private FetchAndLockResponse fetch(String topicName) {
        Topic topic = new Topic();
        topic.setTopicName(topicName);
        topic.setTenantIdIn(new String[]{TENANT});
        topic.setLockDuration(noveltyProperties.getAskLockMs());
        FetchAndLock fetchAndLock = new FetchAndLock();
        fetchAndLock.setWorkerId(cibSevenProperties.getWorkerId());
        fetchAndLock.setMaxTasks(1);
        fetchAndLock.setTopics(new Topic[]{topic});
        FetchAndLockResponse response = fetchAndLockService.fetchAndLockExternalTask(fetchAndLock);
        return response == null || response.getId() == null ? null : response;
    }

    private Submission submission(FetchAndLockResponse task) {
        return submissionService.getSubmission(
                UUID.fromString((String) task.getVariables().get("submissionId").get("value")));
    }

    /** What the calling process gets back (the call activity maps these out). */
    private static Map<String, Object> variables(SubmissionNovelty n) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("novelty_status", n.getStatus().name());
        vars.put("novelty_verdict", n.getVerdict() == null ? "" : n.getVerdict());
        vars.put("novelty_adds_new", n.getAddsNew() == null ? "" : n.getAddsNew());
        vars.put("novelty_flagged", Boolean.TRUE.equals(n.getFlagged()));
        return vars;
    }

    /** Reset the external-task lock window while a step waits. Best-effort. */
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
