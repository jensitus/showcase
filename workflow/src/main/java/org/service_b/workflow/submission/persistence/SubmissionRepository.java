package org.service_b.workflow.submission.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findBySubmitterEmailOrderBySubmittedAtDesc(String submitterEmail);
    List<Submission> findByCreatedByOrderBySubmittedAtDesc(String createdBy);
}
