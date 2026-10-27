package org.service_b.workflow.submission.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The novelty check of one submission: screening against PubMed and, when screening let it
 * through, the larger model's "adds anything new?". The columns are what a list or task
 * shows; {@code resultJson} keeps the pipeline's full answer, the same JSON the Spark writes.
 */
@Entity
@Table(name = "SUBMISSION_NOVELTY")
@Getter
@Setter
public class SubmissionNovelty {

    public enum Status {
        /** Screened and not flagged: waiting for the larger model. */
        SCREENED,
        /** Checked; verdict (and adds_new, if the 35B was asked) are set. */
        DONE,
        /** The pipeline could not be reached, even after retries; the process went on without it.
         *  A screening done before that is kept (verdict). */
        UNAVAILABLE
    }

    @Id
    @Column(name = "ID", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "submission_id", nullable = false, unique = true)
    UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    Status status;

    /** Screening: likely_novel | possible_overlap | likely_duplicate. */
    @Column(name = "verdict")
    String verdict;

    /** The 35B: yes | partly | no; null when screening already flagged the abstract. */
    @Column(name = "adds_new")
    String addsNew;

    /** Flagged by screening, or by the 35B ("nothing" or "only partly" new). */
    @Column(name = "flagged")
    Boolean flagged;

    @Column(name = "checked_at")
    LocalDateTime checkedAt;

    @Column(name = "error", length = 1024)
    String error;

    /** {"screening": NoveltyReport, "novelty_q": answer or null}. */
    @Column(name = "result_json", columnDefinition = "text")
    String resultJson;
}
