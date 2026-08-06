package org.service_b.workflow.screening.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One abstract to screen. The pipeline expects the JSON key {@code abstract}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    private String id;
    private String title;
    @JsonProperty("abstract")
    private String abstractText;
}
