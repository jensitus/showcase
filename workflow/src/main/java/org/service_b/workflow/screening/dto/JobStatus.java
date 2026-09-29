package org.service_b.workflow.screening.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** Response from GET /jobs/{id}: poll until {@link #isTerminal()}. */
@Data
public class JobStatus {
    private String status;   // "running" | "done" | "error"
    private int done;
    private int total;
    /** Each entry is a NoveltyReport (verdict / novelty_score / rationale / top_matches). */
    private List<Map<String, Object>> results;
    /** Set by the pipeline when status is "error". */
    private String error;
    /**
     * The year cutoff the pipeline actually applied, echoed back. Null here when a batch
     * was sent with a cutoff but the pipeline is too old to honour it — the one signal
     * that distinguishes "filtered" from "silently unfiltered".
     */
    @JsonProperty("before_year")
    private Integer beforeYear;

    public boolean isDone() {
        return "done".equals(status);
    }

    /** The pipeline failed the job; the results are incomplete and will not grow. */
    public boolean isError() {
        return "error".equals(status);
    }

    /**
     * Whether polling can stop. Without this a failed job looks identical to a slow one,
     * so the poller would wait out the whole chunk deadline (30 min by default) before
     * reporting a failure the pipeline already knew about.
     */
    public boolean isTerminal() {
        return isDone() || isError();
    }
}
