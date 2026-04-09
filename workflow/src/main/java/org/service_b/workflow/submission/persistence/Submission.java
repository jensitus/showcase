package org.service_b.workflow.submission.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.service_b.workflow.workflow.domain.ResponsibleForType;
import org.service_b.workflow.workflow.persistence.entity.Processable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "SUBMISSION")
public class Submission implements Processable {

    @Id
    @Column(name = "ID", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "title")
    String title;

    @Column(name = "authors")
    String authors;

    @Column(name = "abstract_text", length = 8192)
    String abstractText;

    @Setter
    @Column(name = "topic")
    @Enumerated(EnumType.STRING)
    SubmissionTopic topic;

    @Column(name = "submitter_email")
    String submitterEmail;

    @Column(name = "created_by")
    String createdBy;

    @Column(name = "submission_number")
    Integer submissionNumber;

    @Getter
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    SubmissionState submissionState;

    @Getter
    @Column(name = "reviewer_notes", length = 8192)
    String reviewerNotes;

    @Override
    public ResponsibleForType getType() {
        return ResponsibleForType.SUBMISSION;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public SubmissionTopic getTopic() { return topic; }

    public String getSubmitterEmail() { return submitterEmail; }
    public void setSubmitterEmail(String submitterEmail) { this.submitterEmail = submitterEmail; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Integer getSubmissionNumber() { return submissionNumber; }
    public void setSubmissionNumber(Integer submissionNumber) { this.submissionNumber = submissionNumber; }

    public void setState(SubmissionState submissionState) { this.submissionState = submissionState; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }
}
