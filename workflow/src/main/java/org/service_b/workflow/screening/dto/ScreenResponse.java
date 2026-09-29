package org.service_b.workflow.screening.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Response from POST /screen: a queued batch job. */
@Data
public class ScreenResponse {
    @JsonProperty("job_id")
    private String jobId;
    private int total;
    /** Which corpus the pipeline will screen against. */
    private String corpus;
    /**
     * The year cutoff the pipeline accepted, echoed straight back.
     *
     * <p>This is the whole point of the echo: the pipeline ignores JSON fields it does
     * not know, so a client sending {@code before_year} to an older server gets a
     * perfectly normal 202 and a run with no cutoff at all. Null here after sending a
     * cutoff means exactly that, and {@link
     * org.service_b.workflow.screening.client.NoveltyApiClient} refuses to continue.
     */
    @JsonProperty("before_year")
    private Integer beforeYear;
}
