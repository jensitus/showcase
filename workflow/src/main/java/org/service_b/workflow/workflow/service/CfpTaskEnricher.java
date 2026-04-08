package org.service_b.workflow.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.workflow.dto.CreateTaskRequest;
import org.service_b.workflow.workflow.dto.TaskDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CfpTaskEnricher implements TaskEnricher {

    private final SubmissionWorkflowService submissionWorkflowService;

    @Override
    public boolean supports(Map<String, Object> variables) {
        return variables.containsKey("submissionId");
    }

    @Override
    public TaskDto enrich(String taskKey, CreateTaskRequest request, TaskDto taskDto) {
        UUID submissionId = extractSubmissionId(request.getVariables());
        if (submissionId == null) {
            log.warn("submissionId not parseable for task: {}", taskKey);
            return null;
        }

        String initiator = getStringValue(request.getVariables(), "initiator");
        Map<String, Object> vars = request.getVariables();

        return switch (taskKey) {
            case "ut_receive_submission"   -> submissionWorkflowService.receiveSubmission(submissionId, initiator, taskDto);
            case "ut_assign_reviewers"     -> submissionWorkflowService.assignReviewers(submissionId, initiator, taskDto);
            case "ut_score_abstract"       -> submissionWorkflowService.scoreAbstract(submissionId, initiator, taskDto);
            case "ut_assign_format"        -> submissionWorkflowService.assignFormat(submissionId, initiator, taskDto);
            case "ut_upload-materials"     -> submissionWorkflowService.uploadMaterials(submissionId, initiator, taskDto,
                    getStringValue(vars, "presentation_format"), getStringValue(vars, "session_name"),
                    getStringValue(vars, "session_datetime"),    getStringValue(vars, "session_room"));
            case "ut_speaker_ready_room"   -> submissionWorkflowService.speakerReadyRoom(submissionId, initiator, taskDto,
                    getStringValue(vars, "presentation_format"), getStringValue(vars, "session_name"),
                    getStringValue(vars, "session_datetime"),    getStringValue(vars, "session_room"));
            case "ut_deliver_presentation" -> submissionWorkflowService.deliverPresentation(submissionId, initiator, taskDto,
                    getStringValue(vars, "presentation_format"), getStringValue(vars, "session_name"),
                    getStringValue(vars, "session_datetime"),    getStringValue(vars, "session_room"));
            case "ut_record_no_show"       -> submissionWorkflowService.recordNoShow(submissionId, initiator, taskDto,
                    getStringValue(vars, "presentation_format"), getStringValue(vars, "session_name"),
                    getStringValue(vars, "session_datetime"),    getStringValue(vars, "session_room"));
            case "ut_mock_confirmation"    -> submissionWorkflowService.mockAuthorConfirmation(taskDto);
            case "ut_mock_registration"    -> submissionWorkflowService.mockAuthorRegistration(taskDto);
            case "ut_mock_presenter"       -> submissionWorkflowService.mockPresenterShowsUp(taskDto);
            default -> {
                log.debug("No enrichment for CFP task: {}", taskKey);
                yield null;
            }
        };
    }

    @Override
    public Map<String, Object> coerceCompleteVars(String taskDefinitionKey, Map<String, Object> vars) {
        return switch (taskDefinitionKey) {
            case "ut_receive_submission" -> submissionWorkflowService.mapReceiveSubmissionVars(vars);
            case "ut_score_abstract"     -> submissionWorkflowService.mapScoreAbstractVars(vars);
            case "ut_mock_registration"  -> coerceBoolean(vars, "author_registered");
            case "ut_mock_presenter"     -> coerceBoolean(vars, "presenter_shows_up");
            default                      -> vars;
        };
    }

    private Map<String, Object> coerceBoolean(Map<String, Object> vars, String key) {
        Map<String, Object> mapped = new HashMap<>(vars);
        mapped.put(key, Boolean.parseBoolean(String.valueOf(vars.get(key))));
        return mapped;
    }

    private UUID extractSubmissionId(Map<String, Object> variables) {
        try {
            Object value = variables.get("submissionId");
            return value != null ? UUID.fromString(value.toString()) : null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid submissionId format", e);
            return null;
        }
    }

    private String getStringValue(Map<String, Object> variables, String key) {
        Object value = variables.get(key);
        return value != null ? value.toString() : null;
    }
}
