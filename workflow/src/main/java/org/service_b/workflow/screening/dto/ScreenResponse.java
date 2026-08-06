package org.service_b.workflow.screening.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Response from POST /screen: a queued batch job. */
@Data
public class ScreenResponse {
    @JsonProperty("job_id")
    private String jobId;
    private int total;
}
