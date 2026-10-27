package org.service_b.workflow.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.JobStatus;
import org.service_b.workflow.screening.dto.ScreenResponse;
import org.service_b.workflow.submission.persistence.Submission;
import org.service_b.workflow.submission.persistence.SubmissionNovelty;
import org.service_b.workflow.submission.persistence.SubmissionNoveltyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The novelty check of one valid submission, the process as validated on the Spark:
 * screening against PubMed, and — only when screening finds nothing — the larger model's
 * "adds anything new?". "Nothing new" and "only partly new" count as flagged.
 *
 * <p>Engine concerns (lock, retries) stay with the caller: {@link #check} throws when the
 * pipeline fails, and the caller decides whether to retry or {@link #markUnavailable}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoveltyCheckService {

    private static final Set<String> LLM_FLAG = Set.of("no", "partly");

    private final NoveltyApiClient client;
    private final SubmissionNoveltyRepository repository;
    private final NoveltyProperties props;
    private final ObjectMapper mapper;

    /**
     * Check a submission and store the result. A submission already checked is not sent
     * again (a re-fetched task after a crash between storing and completing).
     *
     * @param keepAlive called while waiting for the larger model, to keep the task's lock
     */
    public SubmissionNovelty check(Submission submission, Runnable keepAlive) throws InterruptedException {
        var existing = repository.findBySubmissionId(submission.getId())
                .filter(n -> n.getStatus() == SubmissionNovelty.Status.DONE);
        if (existing.isPresent()) {
            return existing.get();
        }
        String id = submission.getId().toString();
        // abstract_text carries the section labels; the pipeline splits it by them
        Map<String, Object> screening = client.check(id, submission.getTitle(), submission.getAbstractText());
        String verdict = String.valueOf(screening.get("verdict"));
        boolean screeningFlagged = !"likely_novel".equals(verdict);

        Map<String, Object> answer = null;
        if (!screeningFlagged) {
            Map<String, Object> sub = Map.of("id", id, "title", submission.getTitle(),
                    "abstract", submission.getAbstractText());
            ScreenResponse job = client.askNew(List.of(Map.of("submission", sub, "results", List.of(screening))));
            answer = awaitAnswer(job.getJobId(), keepAlive);
        }
        String addsNew = answer == null ? null : String.valueOf(answer.get("adds_new"));

        SubmissionNovelty n = repository.findBySubmissionId(submission.getId()).orElseGet(SubmissionNovelty::new);
        n.setSubmissionId(submission.getId());
        n.setStatus(SubmissionNovelty.Status.DONE);
        n.setVerdict(verdict);
        n.setAddsNew(addsNew);
        n.setFlagged(screeningFlagged || LLM_FLAG.contains(addsNew));
        n.setCheckedAt(LocalDateTime.now());
        n.setError(null);
        Map<String, Object> result = new HashMap<>();
        result.put("screening", screening);
        result.put("novelty_q", answer);
        n.setResultJson(toJson(result));
        log.info("[novelty] submission {}: {}{} -> {}", id, verdict,
                addsNew == null ? "" : ", 35B: " + addsNew, n.getFlagged() ? "flagged" : "not flagged");
        return repository.save(n);
    }

    /** The pipeline stayed unreachable: record that, so the process can go on and people see why. */
    public SubmissionNovelty markUnavailable(Submission submission, String error) {
        SubmissionNovelty n = repository.findBySubmissionId(submission.getId()).orElseGet(SubmissionNovelty::new);
        n.setSubmissionId(submission.getId());
        n.setStatus(SubmissionNovelty.Status.UNAVAILABLE);
        n.setCheckedAt(LocalDateTime.now());
        n.setError(error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
        return repository.save(n);
    }

    private Map<String, Object> awaitAnswer(String jobId, Runnable keepAlive) throws InterruptedException {
        long deadline = System.currentTimeMillis() + props.getAskChunkMaxWaitMs();
        while (true) {
            JobStatus status = client.getAskJob(jobId);
            if (status != null && status.isCancelled()) {
                throw new IllegalStateException("ask job " + jobId + " was cancelled in the pipeline");
            }
            if (status != null && status.isDone()) {
                Map<String, Object> answer = status.getResults().get(0);
                if (answer.get("error") != null) {     // the pipeline already retried once
                    throw new IllegalStateException("the larger model failed: " + answer.get("error"));
                }
                return answer;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("ask job " + jobId + " did not finish within "
                        + props.getAskChunkMaxWaitMs() + "ms");
            }
            keepAlive.run();
            Thread.sleep(props.getPollIntervalMs());
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialise the novelty result", e);
        }
    }
}
