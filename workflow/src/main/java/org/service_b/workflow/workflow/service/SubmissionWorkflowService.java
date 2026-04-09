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
    private static final String[] REVIEW_OPTIONS = {"APPROVED", "REJECTED"};

    private final SubmissionService submissionService;
    private final RestClientService restClientService;
    private final ProcessService processService;
    private final HashMapConverter hashMapConverter;

    public Submission startSubmissionWorkflow(CreateSubmissionRequest request, String createdBy) throws Exception {
        // 1. Save submission — commits immediately (no outer transaction)
        Submission submission = submissionService.saveSubmission(
                request.getTitle(),
                request.getAuthors(),
                request.getAbstractText(),
                request.getTopic(),
                request.getSubmitterEmail(),
                createdBy
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

    public TaskDto assignReviewers(UUID submissionId, String initiator, TaskDto taskDto) {
        Submission submission = submissionService.getSubmission(submissionId);

        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(buildSubmissionAdditionalInfo(submission)));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(buildReviewerConfig()));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(buildReviewerConfigData()));
        return taskDto;
    }

    private Map<String, Object> buildReviewerConfig() {
        Map<String, Object> config = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            config.put("reviewer_" + i, "");
        }
        return config;
    }

    private Map<String, Object> buildReviewerConfigData() {
        Map<String, Object> configData = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            Map<String, Object> field = new HashMap<>();
            field.put("position", i);
            field.put("type", "text");
            field.put("required", i <= 3);
            configData.put("reviewer_" + i, field);
        }
        return configData;
    }

    public TaskDto scoreAbstract(UUID submissionId, String initiator, TaskDto taskDto) {
        Submission submission = submissionService.getSubmission(submissionId);

        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(buildBlindAdditionalInfo(submission)));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(buildScoringConfig()));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(buildScoringConfigData()));
        return taskDto;
    }

    /** Blind review — title and abstract only, no author or submitter info. */
    private Map<String, Object> buildBlindAdditionalInfo(Submission submission) {
        Map<String, Object> info = new HashMap<>();

        Map<String, String> paperDetails = new HashMap<>();
        paperDetails.put("Title", submission.getTitle());
        paperDetails.put("Topic", submission.getTopic() != null ? submission.getTopic().name() : "—");
        info.put("Submission", paperDetails);

        Map<String, String> abstractContent = new HashMap<>();
        abstractContent.put("Abstract", submission.getAbstractText());
        info.put("Content", abstractContent);

        return info;
    }

    private Map<String, Object> buildScoringConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("score_relevance",       0);
        config.put("score_methodology",     0);
        config.put("score_originality",     0);
        config.put("score_clarity",         0);
        config.put("score_conclusions",     0);
        config.put("overall_recommendation", "??");
        config.put("reviewer_comments",     "");
        return config;
    }

    private static final String[] RECOMMENDATION_OPTIONS = {"ACCEPT_ORAL", "ACCEPT_POSTER", "REJECT"};

    private Map<String, Object> buildScoringConfigData() {
        Map<String, Object> configData = new HashMap<>();

        String[][] criteria = {
            {"score_relevance",   "Importance / Relevance (1–5)"},
            {"score_methodology", "Methodology (1–5)"},
            {"score_originality", "Originality / Novelty (1–5)"},
            {"score_clarity",     "Clarity / Presentation (1–5)"},
            {"score_conclusions", "Conclusions / Impact (1–5)"}
        };
        for (int i = 0; i < criteria.length; i++) {
            Map<String, Object> field = new HashMap<>();
            field.put("position", i + 1);
            field.put("type", "number");
            field.put("min", 1);
            field.put("max", 5);
            configData.put(criteria[i][0], field);
        }

        Map<String, Object> recField = new HashMap<>();
        recField.put("position", 6);
        recField.put("type", "select");
        recField.put("values", RECOMMENDATION_OPTIONS);
        configData.put("overall_recommendation", recField);

        Map<String, Object> commentsField = new HashMap<>();
        commentsField.put("position", 7);
        commentsField.put("type", "textarea");
        configData.put("reviewer_comments", commentsField);

        return configData;
    }

    private static final String[] FORMAT_OPTIONS = {
        "ORAL", "POSTER", "E_POSTER", "LIGHTNING_TALK", "LATE_BREAKING"
    };

    public TaskDto assignFormat(UUID submissionId, String initiator, TaskDto taskDto) {
        Submission submission = submissionService.getSubmission(submissionId);

        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(buildSubmissionAdditionalInfo(submission)));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(buildFormatConfig()));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(buildFormatConfigData()));
        return taskDto;
    }

    private Map<String, Object> buildFormatConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("presentation_format",    "??");
        config.put("session_name",           "");
        config.put("session_datetime",       "");
        config.put("session_room",           "");
        config.put("confirmation_deadline",  "");
        config.put("registration_deadline",  "");
        config.put("upload_deadline",        "");
        config.put("committee_notes",        "");
        return config;
    }

    private Map<String, Object> buildFormatConfigData() {
        Map<String, Object> configData = new HashMap<>();

        Map<String, Object> formatField = new HashMap<>();
        formatField.put("position", 1);
        formatField.put("type", "select");
        formatField.put("values", FORMAT_OPTIONS);
        configData.put("presentation_format", formatField);

        String[][] textFields = {
            {"session_name",          "Session Name"},
            {"session_datetime",      "Date & Time (e.g. 2026-09-15 10:30)"},
            {"session_room",          "Room / Hall"},
            {"confirmation_deadline", "Confirmation Deadline (e.g. 2026-05-01)"},
            {"registration_deadline", "Registration Deadline (e.g. 2026-06-01)"},
            {"upload_deadline",       "Slide / Poster Upload Deadline (e.g. 2026-09-01)"},
        };
        for (int i = 0; i < textFields.length; i++) {
            Map<String, Object> field = new HashMap<>();
            field.put("position", i + 2);
            field.put("type", "text");
            configData.put(textFields[i][0], field);
        }

        Map<String, Object> notesField = new HashMap<>();
        notesField.put("position", 9);
        notesField.put("type", "textarea");
        configData.put("committee_notes", notesField);

        return configData;
    }

    // -------------------------------------------------------------------------
    // Mock user tasks — replace script tasks in BPMN for interactive demo paths
    // -------------------------------------------------------------------------

    public TaskDto mockAuthorConfirmation(TaskDto taskDto) {
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(
                Map.of("author_confirmed", "??")));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(
                Map.of("author_confirmed", Map.of("position", 1, "type", "select",
                        "values", new String[]{"yes", "declined", "pending"}))));
        return taskDto;
    }

    public TaskDto mockAuthorRegistration(TaskDto taskDto) {
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(
                Map.of("author_registered", false)));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(
                Map.of("author_registered", Map.of("position", 1, "type", "select",
                        "values", new String[]{"true", "false"}))));
        return taskDto;
    }

    public TaskDto mockPresenterShowsUp(TaskDto taskDto) {
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(
                Map.of("presenter_shows_up", false)));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(
                Map.of("presenter_shows_up", Map.of("position", 1, "type", "select",
                        "values", new String[]{"true", "false"}))));
        return taskDto;
    }

    // -------------------------------------------------------------------------
    // Stage 6 — Presentation tasks
    // -------------------------------------------------------------------------

    public TaskDto uploadMaterials(UUID submissionId, String initiator, TaskDto taskDto,
                                   String format, String sessionName, String sessionDatetime, String sessionRoom) {
        Submission submission = submissionService.getSubmission(submissionId);
        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(
                buildSessionAdditionalInfo(submission, format, sessionName, sessionDatetime, sessionRoom)));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(buildUploadConfig(format)));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(buildUploadConfigData()));
        return taskDto;
    }

    public TaskDto speakerReadyRoom(UUID submissionId, String initiator, TaskDto taskDto,
                                    String format, String sessionName, String sessionDatetime, String sessionRoom) {
        Submission submission = submissionService.getSubmission(submissionId);
        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(
                buildSessionAdditionalInfo(submission, format, sessionName, sessionDatetime, sessionRoom)));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(buildSpeakerReadyRoomConfig()));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(buildSpeakerReadyRoomConfigData()));
        return taskDto;
    }

    public TaskDto deliverPresentation(UUID submissionId, String initiator, TaskDto taskDto,
                                       String format, String sessionName, String sessionDatetime, String sessionRoom) {
        Submission submission = submissionService.getSubmission(submissionId);
        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(
                buildSessionAdditionalInfo(submission, format, sessionName, sessionDatetime, sessionRoom)));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(buildDeliveryConfig()));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(buildDeliveryConfigData()));
        return taskDto;
    }

    public TaskDto recordNoShow(UUID submissionId, String initiator, TaskDto taskDto,
                                String format, String sessionName, String sessionDatetime, String sessionRoom) {
        Submission submission = submissionService.getSubmission(submissionId);
        taskDto.setAssignee(initiator);
        taskDto.setAdditionalInfo(hashMapConverter.convertToDatabaseColumn(
                buildSessionAdditionalInfo(submission, format, sessionName, sessionDatetime, sessionRoom)));
        taskDto.setConfig(hashMapConverter.convertToDatabaseColumn(buildNoShowConfig()));
        taskDto.setConfigData(hashMapConverter.convertToDatabaseColumn(buildNoShowConfigData()));
        return taskDto;
    }

    private static final String[] NO_SHOW_REASON_OPTIONS = {
        "ILLNESS", "TRAVEL_ISSUE", "PERSONAL_EMERGENCY", "UNRESPONSIVE", "OTHER"
    };
    private static final String[] SANCTION_OPTIONS = {"NONE", "WARNING", "FLAGGED_FOR_FUTURE"};

    private Map<String, Object> buildNoShowConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("no_show_reason",       "??");
        config.put("advance_notice_given", "NO");
        config.put("no_show_notes",        "");
        config.put("sanction_applied",     "NONE");
        return config;
    }

    private Map<String, Object> buildNoShowConfigData() {
        Map<String, Object> configData = new HashMap<>();

        Map<String, Object> reasonField = new HashMap<>();
        reasonField.put("position", 1);
        reasonField.put("type", "select");
        reasonField.put("values", NO_SHOW_REASON_OPTIONS);
        configData.put("no_show_reason", reasonField);

        Map<String, Object> noticeField = new HashMap<>();
        noticeField.put("position", 2);
        noticeField.put("type", "select");
        noticeField.put("values", new String[]{"YES", "NO"});
        configData.put("advance_notice_given", noticeField);

        Map<String, Object> notesField = new HashMap<>();
        notesField.put("position", 3);
        notesField.put("type", "textarea");
        configData.put("no_show_notes", notesField);

        Map<String, Object> sanctionField = new HashMap<>();
        sanctionField.put("position", 4);
        sanctionField.put("type", "select");
        sanctionField.put("values", SANCTION_OPTIONS);
        configData.put("sanction_applied", sanctionField);

        return configData;
    }

    private Map<String, Object> buildSessionAdditionalInfo(Submission submission, String format,
                                                            String sessionName, String sessionDatetime, String sessionRoom) {
        Map<String, Object> info = new HashMap<>();

        Map<String, String> paper = new HashMap<>();
        paper.put("Title", submission.getTitle());
        paper.put("Authors", submission.getAuthors());
        paper.put("Topic", submission.getTopic() != null ? submission.getTopic().name() : "—");
        info.put("Submission", paper);

        Map<String, String> session = new HashMap<>();
        session.put("Format",    format != null ? format : "—");
        session.put("Session",   sessionName != null ? sessionName : "—");
        session.put("Date/Time", sessionDatetime != null ? sessionDatetime : "—");
        session.put("Room",      sessionRoom != null ? sessionRoom : "—");
        info.put("Session", session);

        return info;
    }

    private static final String[] UPLOAD_FORMAT_OPTIONS = {"POWERPOINT", "PDF", "OTHER"};

    private Map<String, Object> buildUploadConfig(String presentationFormat) {
        Map<String, Object> config = new HashMap<>();
        config.put("file_format",    "??");
        config.put("upload_notes",   "");
        return config;
    }

    private Map<String, Object> buildUploadConfigData() {
        Map<String, Object> configData = new HashMap<>();

        Map<String, Object> formatField = new HashMap<>();
        formatField.put("position", 1);
        formatField.put("type", "select");
        formatField.put("values", UPLOAD_FORMAT_OPTIONS);
        configData.put("file_format", formatField);

        Map<String, Object> notesField = new HashMap<>();
        notesField.put("position", 2);
        notesField.put("type", "textarea");
        configData.put("upload_notes", notesField);

        return configData;
    }

    private static final String[] TECH_CHECK_OPTIONS = {"PASSED", "PASSED_WITH_ISSUES", "FAILED"};

    private Map<String, Object> buildSpeakerReadyRoomConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("technical_check_result", "??");
        config.put("issues_found",           "");
        config.put("resolution",             "");
        return config;
    }

    private Map<String, Object> buildSpeakerReadyRoomConfigData() {
        Map<String, Object> configData = new HashMap<>();

        Map<String, Object> checkField = new HashMap<>();
        checkField.put("position", 1);
        checkField.put("type", "select");
        checkField.put("values", TECH_CHECK_OPTIONS);
        configData.put("technical_check_result", checkField);

        Map<String, Object> issuesField = new HashMap<>();
        issuesField.put("position", 2);
        issuesField.put("type", "textarea");
        configData.put("issues_found", issuesField);

        Map<String, Object> resolutionField = new HashMap<>();
        resolutionField.put("position", 3);
        resolutionField.put("type", "textarea");
        configData.put("resolution", resolutionField);

        return configData;
    }

    private static final String[] DELIVERY_OUTCOME_OPTIONS = {
        "DELIVERED", "CANCELLED_ILLNESS", "CANCELLED_NOSHOW", "CANCELLED_TECHNICAL"
    };

    private Map<String, Object> buildDeliveryConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("delivery_outcome",     "??");
        config.put("actual_duration_min",  0);
        config.put("session_chair_notes",  "");
        return config;
    }

    private Map<String, Object> buildDeliveryConfigData() {
        Map<String, Object> configData = new HashMap<>();

        Map<String, Object> outcomeField = new HashMap<>();
        outcomeField.put("position", 1);
        outcomeField.put("type", "select");
        outcomeField.put("values", DELIVERY_OUTCOME_OPTIONS);
        configData.put("delivery_outcome", outcomeField);

        Map<String, Object> durationField = new HashMap<>();
        durationField.put("position", 2);
        durationField.put("type", "number");
        durationField.put("min", 0);
        durationField.put("max", 60);
        configData.put("actual_duration_min", durationField);

        Map<String, Object> notesField = new HashMap<>();
        notesField.put("position", 3);
        notesField.put("type", "textarea");
        configData.put("session_chair_notes", notesField);

        return configData;
    }

    public Map<String, Object> mapScoreAbstractVars(Map<String, Object> vars) {
        Map<String, Object> mapped = new HashMap<>(vars);
        Object rec = vars.get("overall_recommendation");
        mapped.put("abstract_accepted",
                   "ACCEPT_ORAL".equals(rec) || "ACCEPT_POSTER".equals(rec));
        return mapped;
    }

    public Map<String, Object> mapReceiveSubmissionVars(Map<String, Object> vars) {
        Map<String, Object> mapped = new HashMap<>(vars);
        mapped.put("submission_valid", "APPROVED".equals(vars.get(REVIEW_DECISION)));
        return mapped;
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
