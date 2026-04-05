package org.service_b.workflow.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.submission.dto.CreateSubmissionRequest;
import org.service_b.workflow.submission.persistence.Submission;
import org.service_b.workflow.submission.service.SubmissionService;
import org.service_b.workflow.workflow.dto.ProcessInstanceWithVariableDto;
import org.service_b.workflow.workflow.dto.TaskDto;
import org.service_b.workflow.shared.utils.HashMapConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionWorkflowService {

    private static final String SUBMISSION_PROCESS_DEFINITION_KEY = "abstract_submission_lifecycle";
    private static final String SUBMISSION_TENANT_ID = "cfp";
    private static final String REVIEW_DECISION = "reviewDecision";
    private static final String REVIEWER_NOTES = "reviewerNotes";
    private static final String[] REVIEW_OPTIONS = {"APPROVED", "REJECTED", "REVISION_REQUESTED"};

    private final SubmissionService submissionService;
    private final RestClientService restClientService;
    private final ProcessService processService;
    private final HashMapConverter hashMapConverter;

    public Submission startSubmissionWorkflow(CreateSubmissionRequest request) throws Exception {
        // 1. Save submission — commits immediately (no outer transaction)
        Submission submission = submissionService.saveSubmission(
                request.getTitle(),
                request.getAuthors(),
                request.getAbstractText(),
                request.getTopic(),
                request.getSubmitterEmail()
        );

        // 2. Pre-create the ProcessEntity with submission linkage and NO processInstanceId yet,
        //    so the CIB Seven task callback can find it in the DB even before we get the response.
        processService.preCreateProcess(submission, SUBMISSION_TENANT_ID);

        // 3. Start the process in CIB Seven. CIB Seven may immediately call back POST /api/tasks
        //    for ut_receive_submission before this call returns — the pre-created entity handles that.
        StartProcessBody startProcessBody = buildStartProcessBody(request, submission);
        ProcessInstanceWithVariableDto processInstance =
                restClientService.startCib7Process(SUBMISSION_PROCESS_DEFINITION_KEY,
                                                   startProcessBody,
                                                   SUBMISSION_TENANT_ID);

        // 4. Now update the pre-created entity with the real processInstanceId + definitionId.
        log.info("Started submission process instance: {}", processInstance);
        processService.activateProcess(submission.getId(),
                                       processInstance.getId(),
                                       processInstance.getDefinitionId());

        return submission;
    }

    public TaskDto receiveSubmission(UUID submissionId, String initiator, TaskDto taskDto) {
        Submission submission = submissionService.getSubmission(submissionId);

        Map<String, Object> additionalInfo = buildSubmissionAdditionalInfo(submission);
        Map<String, Object> config = buildReviewConfig();
        Map<String, Object> configData = buildReviewConfigData();

        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(additionalInfo));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(config));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(configData));
        return taskDto;
    }

    private Map<String, Object> buildSubmissionAdditionalInfo(Submission submission) {
        Map<String, Object> info = new HashMap<>();

        Map<String, String> paperDetails = new HashMap<>();
        paperDetails.put("Title", submission.getTitle());
        paperDetails.put("Authors", submission.getAuthors());
        paperDetails.put("Topic", submission.getTopic() != null ? submission.getTopic().name() : "—");
        paperDetails.put("Submitted by", submission.getSubmitterEmail());
        info.put("Submission", paperDetails);

        Map<String, String> abstractContent = new HashMap<>();
        abstractContent.put("Abstract", submission.getAbstractText());
        info.put("Content", abstractContent);

        return info;
    }

    private Map<String, Object> buildReviewConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(REVIEW_DECISION, "??");
        config.put(REVIEWER_NOTES, "");
        return config;
    }

    private Map<String, Object> buildReviewConfigData() {
        Map<String, Object> configData = new HashMap<>();

        Map<String, Object> decisionField = new HashMap<>();
        decisionField.put("position", 1);
        decisionField.put("type", "select");
        decisionField.put("values", REVIEW_OPTIONS);
        configData.put(REVIEW_DECISION, decisionField);

        Map<String, Object> notesField = new HashMap<>();
        notesField.put("position", 2);
        notesField.put("type", "textarea");
        configData.put(REVIEWER_NOTES, notesField);

        return configData;
    }

    private StartProcessBody buildStartProcessBody(CreateSubmissionRequest request, Submission submission) {
        String initiator = SecurityContextHolder.getContext().getAuthentication().getName();

        Map<String, Object> variables = new HashMap<>();
        variables.put("submissionId", Map.of("value", submission.getId().toString(), "type", "String"));
        variables.put("title", Map.of("value", request.getTitle(), "type", "String"));
        variables.put("authors", Map.of("value", request.getAuthors(), "type", "String"));
        variables.put("topic", Map.of("value", request.getTopic(), "type", "String"));
        variables.put("submitterEmail", Map.of("value", request.getSubmitterEmail(), "type", "String"));
        variables.put("initiator", Map.of("value", initiator, "type", "String"));
        variables.put("call_for_papers",Map.of("value", true, "type", "Boolean"));

        StartProcessBody body = new StartProcessBody();
        body.setVariables(variables);
        body.setBusinessKey("cfp-" + submission.getId());
        return body;
    }
}
