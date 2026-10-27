package org.service_b.workflow.submission.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** One section of a submission's abstract (see {@link AbstractSection}); shaped like a congress export's sections. */
@Entity
@Table(name = "SUBMISSION_SECTION")
@Getter
@Setter
public class SubmissionSection {

    @Id
    @Column(name = "ID", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    // Not serialised: a submission is sent over SSE with its sections, and the back
    // reference would recurse.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    Submission submission;

    @Column(name = "order_nr", nullable = false)
    int orderNr;

    /** The AbstractSection constant, e.g. PURPOSE. */
    @Column(name = "shortname", nullable = false)
    String shortname;

    /** The label at submission time, e.g. "Purpose or Learning Objective". */
    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "content", length = 4000, nullable = false)
    String content;
}
