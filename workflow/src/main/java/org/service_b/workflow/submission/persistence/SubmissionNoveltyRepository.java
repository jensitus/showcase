package org.service_b.workflow.submission.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubmissionNoveltyRepository extends JpaRepository<SubmissionNovelty, UUID> {

    Optional<SubmissionNovelty> findBySubmissionId(UUID submissionId);
}
