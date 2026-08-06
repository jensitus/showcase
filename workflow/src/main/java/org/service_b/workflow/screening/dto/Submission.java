package org.service_b.workflow.screening.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One abstract to screen. The pipeline expects the JSON key {@code abstract}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // corpus/export rows may carry extra fields (year, …)
public class Submission {
    private String id;
    private String title;
    @JsonProperty("abstract")
    private String abstractText;
}
