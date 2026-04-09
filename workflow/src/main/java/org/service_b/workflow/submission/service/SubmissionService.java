package org.service_b.workflow.submission.service;

import lombok.RequiredArgsConstructor;
import org.service_b.workflow.submission.persistence.Submission;
import org.service_b.workflow.submission.persistence.SubmissionRepository;
import org.service_b.workflow.submission.persistence.SubmissionState;
import org.service_b.workflow.submission.persistence.SubmissionTopic;
import org.service_b.workflow.sse.EventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final EventService eventService;

    public Submission saveSubmission(String title,
                                     String authors,
                                     String abstractText,
                                     String topic,
                                     String submitterEmail,
                                     String createdBy) {
        Submission submission = new Submission();
        submission.setTitle(title);
        submission.setAuthors(authors);
        submission.setAbstractText(abstractText);
        submission.setTopic(SubmissionTopic.valueOf(topic));
        submission.setSubmitterEmail(submitterEmail);
        submission.setCreatedBy(createdBy);
        submission.setSubmissionNumber(generateSubmissionNumber());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setState(SubmissionState.SUBMITTED);

        Submission saved = submissionRepository.save(submission);
        eventService.sendEvent(saved, "submission");
        return saved;
    }

    public void updateState(UUID submissionId, SubmissionState state) {
        Submission submission = submissionRepository.findById(submissionId).orElseThrow();
        submission.setState(state);
        submission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(submission);
        eventService.sendEvent(submission, "submission");
    }

    public void updateReviewerNotes(UUID submissionId, String reviewerNotes) {
        Submission submission = submissionRepository.findById(submissionId).orElseThrow();
        submission.setReviewerNotes(reviewerNotes);
        submission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(submission);
    }

    public Submission getSubmission(UUID submissionId) {
        return submissionRepository.findById(submissionId).orElseThrow();
    }

    public List<Submission> getSubmissionsByEmail(String email) {
        return submissionRepository.findBySubmitterEmailOrderBySubmittedAtDesc(email);
    }

    public List<Submission> getMySubmissions(String username) {
        return submissionRepository.findByCreatedByOrderBySubmittedAtDesc(username);
    }

    private int generateSubmissionNumber() {
        long count = submissionRepository.count();
        return 20000 + Long.valueOf(count + 1).intValue();
    }
}
