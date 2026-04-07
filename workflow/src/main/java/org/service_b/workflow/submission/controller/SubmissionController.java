package org.service_b.workflow.submission.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.submission.dto.CreateSubmissionRequest;
import org.service_b.workflow.submission.dto.SubmissionDto;
import org.service_b.workflow.submission.persistence.Submission;
import org.service_b.workflow.submission.service.SubmissionService;
import org.service_b.workflow.workflow.service.SubmissionWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionWorkflowService submissionWorkflowService;

    @PostMapping
    public ResponseEntity<SubmissionDto> createSubmission(@RequestBody CreateSubmissionRequest request) throws Exception {
        Submission submission = submissionWorkflowService.startSubmissionWorkflow(request);
        SubmissionDto dto = toDto(submission);
        return ResponseEntity.ok(dto);
    }

    private SubmissionDto toDto(Submission submission) {
        SubmissionDto dto = new SubmissionDto();
        dto.setId(submission.getId());
        dto.setTitle(submission.getTitle());
        dto.setAuthors(submission.getAuthors());
        dto.setAbstractText(submission.getAbstractText());
        dto.setTopic(submission.getTopic() != null ? submission.getTopic().name() : null);
        dto.setSubmitterEmail(submission.getSubmitterEmail());
        dto.setSubmissionNumber(submission.getSubmissionNumber());
        dto.setState(submission.getSubmissionState() != null ? submission.getSubmissionState().name() : null);
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setUpdatedAt(submission.getUpdatedAt());
        return dto;
    }
}
