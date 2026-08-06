package org.service_b.workflow.screening.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** Response from GET /jobs/{id}: poll until {@code status == "done"}. */
@Data
public class JobStatus {
    private String status;   // "running" | "done"
    private int done;
    private int total;
    /** Each entry is a NoveltyReport (verdict / novelty_score / rationale / top_matches). */
    private List<Map<String, Object>> results;

    public boolean isDone() {
        return "done".equals(status);
    }
}
