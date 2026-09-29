package org.service_b.workflow.screening.dto;

import lombok.Data;

/** Body for POST /api/screening/batch — where the submissions export lives. */
@Data
public class StartBatchRequest {
    /** Path to the submissions export (.jsonl / .json / .csv) the runner ingests. */
    private String exportPath;

    /**
     * Compare only against abstracts from strictly earlier years (year &lt; beforeYear).
     *
     * <p>Screening a congress against a corpus that contains it is meaningless: every
     * submission matches itself at cosine ~1.0 and comes back a duplicate of itself. A
     * caller screening the 2026 congress therefore passes 2026.
     *
     * <p>Null means no cutoff, which is the right default for corpora that cannot
     * contain the submissions (e.g. screening fresh abstracts against PubMed).
     */
    private Integer beforeYear;

    /**
     * Which pipeline corpus to screen against (a name from the pipeline's /health).
     *
     * <p>Null lets the pipeline pick its default, which is fine for the synthetic demo
     * but wrong for a congress run — see {@link ScreenRequest#getCorpus()}.
     */
    private String corpus;
}
