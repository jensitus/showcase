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
 * The steps of the novelty_check process, one per external task — the process as validated
 * on the Spark: {@link #screen} against PubMed, and only when that finds nothing
 * {@link #ask} the larger model "adds anything new?". "Nothing new" and "only partly new"
 * count as flagged. Each step stores its part in the submission's submission_novelty row.
 *
 * <p>Engine concerns (lock, retries, the BPMN error) stay with the workers: a step throws
 * when the pipeline fails, and {@link #markUnavailable} records that it stayed unreachable.
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
     * Screen against PubMed. Flagged here, the check is complete (DONE); otherwise it waits
     * for the larger model (SCREENED). A submission screened before is not sent again.
     */
    public SubmissionNovelty screen(Submission submission) {
        SubmissionNovelty n = row(submission);
        if (n.getVerdict() != null && n.getStatus() != SubmissionNovelty.Status.UNAVAILABLE) {
            return n;                                   // re-fetched task after a crash
        }
        // abstract_text carries the section labels; the pipeline splits it by them
        Map<String, Object> screening = client.check(submission.getId().toString(),
                submission.getTitle(), submission.getAbstractText());
        String verdict = String.valueOf(screening.get("verdict"));
        boolean flagged = !"likely_novel".equals(verdict);
        n.setVerdict(verdict);
        n.setFlagged(flagged);
        n.setStatus(flagged ? SubmissionNovelty.Status.DONE : SubmissionNovelty.Status.SCREENED);
        n.setCheckedAt(LocalDateTime.now());
        n.setError(null);
        n.setResultJson(toJson(result(screening, null)));
        log.info("[novelty] submission {}: screening {}", submission.getId(), verdict);
        return repository.save(n);
    }

    /**
     * Ask the larger model about a submission screening let through.
     *
     * @param keepAlive called while it thinks, to keep the task's lock
     */
    @SuppressWarnings("unchecked")
    public SubmissionNovelty ask(Submission submission, Runnable keepAlive) throws InterruptedException {
        SubmissionNovelty n = row(submission);
        if (n.getStatus() == SubmissionNovelty.Status.DONE) {
            return n;
        }
        Map<String, Object> screening = (Map<String, Object>) fromJson(n.getResultJson()).get("screening");
        if (screening == null) {
            throw new IllegalStateException("submission " + submission.getId() + " has not been screened");
        }
        Map<String, Object> sub = Map.of("id", submission.getId().toString(), "title", submission.getTitle(),
                "abstract", submission.getAbstractText());
        ScreenResponse job = client.askNew(List.of(Map.of("submission", sub, "results", List.of(screening))));
        Map<String, Object> answer = awaitAnswer(job.getJobId(), keepAlive);
        String addsNew = String.valueOf(answer.get("adds_new"));

        n.setAddsNew(addsNew);
        n.setFlagged(LLM_FLAG.contains(addsNew));
        n.setStatus(SubmissionNovelty.Status.DONE);
        n.setCheckedAt(LocalDateTime.now());
        n.setResultJson(toJson(result(screening, answer)));
        log.info("[novelty] submission {}: larger model {}", submission.getId(), addsNew);
        return repository.save(n);
    }

    /** The pipeline stayed unreachable: record that, so the process can go on and people see why. */
    public SubmissionNovelty markUnavailable(Submission submission, String error) {
        SubmissionNovelty n = row(submission);
        n.setStatus(SubmissionNovelty.Status.UNAVAILABLE);
        n.setCheckedAt(LocalDateTime.now());
        n.setError(error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
        return repository.save(n);
    }

    private SubmissionNovelty row(Submission submission) {
        return repository.findBySubmissionId(submission.getId()).orElseGet(() -> {
            SubmissionNovelty n = new SubmissionNovelty();
            n.setSubmissionId(submission.getId());
            return n;
        });
    }

    private static Map<String, Object> result(Map<String, Object> screening, Map<String, Object> answer) {
        Map<String, Object> result = new HashMap<>();
        result.put("screening", screening);
        result.put("novelty_q", answer);
        return result;
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unreadable novelty result", e);
        }
    }
}
