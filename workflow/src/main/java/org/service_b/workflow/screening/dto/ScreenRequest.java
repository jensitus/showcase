package org.service_b.workflow.screening.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Body for POST /screen. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenRequest {
    // NB: @AllArgsConstructor follows FIELD order — (submissions, corpus, beforeYear).
    // NoveltyApiClient uses the named setters instead, so adding a field later cannot
    // silently swap two arguments of the same type.
    private List<Submission> submissions;

    /**
     * Which corpus to screen against, by the name the pipeline's GET /health lists.
     *
     * <p>Must be explicit for congress runs. The pipeline falls back to the FIRST entry
     * of its NOVELTY_INDEX_DIRS, which on the congress server is {@code excl-check}
     * (76,024 records, with the not-new-labelled abstracts removed) rather than
     * {@code congress-A2} (87,844, all congresses 2011-2026) — a quietly different
     * corpus, and only the latter is the one a year cutoff is meant for. An unknown name
     * is rejected with 400 before the job starts, so a mistake here fails loudly.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String corpus;

    /**
     * Year cutoff, sent as the pipeline's snake_case {@code before_year}.
     *
     * <p>Omitted entirely when null, so a request from this client is byte-identical to
     * what it sent before the field existed. That matters because the pipeline ignores
     * unknown JSON fields: sending {@code before_year} to a server too old to understand
     * it is a SILENT no-op, not an error. The only safe confirmation is that the value
     * comes back — the pipeline echoes it in the 202, in GET /jobs and on every report.
     */
    @JsonProperty("before_year")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer beforeYear;
}
