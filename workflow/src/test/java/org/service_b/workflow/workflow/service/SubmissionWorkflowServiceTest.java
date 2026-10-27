package org.service_b.workflow.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.service_b.workflow.shared.utils.HashMapConverter;
import org.service_b.workflow.submission.dto.CreateSubmissionRequest;
import org.service_b.workflow.submission.dto.SectionDto;
import org.service_b.workflow.submission.persistence.Submission;
import org.service_b.workflow.submission.persistence.SubmissionNovelty;
import org.service_b.workflow.submission.persistence.SubmissionNoveltyRepository;
import org.service_b.workflow.submission.persistence.SubmissionTopic;
import org.service_b.workflow.submission.service.SubmissionService;
import org.service_b.workflow.workflow.domain.ResponsibleForType;
import org.service_b.workflow.workflow.dto.ProcessDto;
import org.service_b.workflow.workflow.dto.ProcessInstanceWithVariableDto;
import org.service_b.workflow.workflow.dto.TaskDto;
import org.service_b.workflow.workflow.exception.ProcessStartException;
import org.service_b.workflow.workflow.persistence.entity.ProcessEntity;
import org.service_b.workflow.workflow.persistence.entity.Processable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmissionWorkflowServiceTest {

    private SubmissionWorkflowService service;
    private StubSubmissionService submissionService;
    private StubProcessService processService;
    private StubRestClientService restClientService;
    private SubmissionNoveltyRepository noveltyRepository;

    @BeforeEach
    void setUp() {
        submissionService = new StubSubmissionService();
        processService = new StubProcessService();
        restClientService = new StubRestClientService();
        noveltyRepository = mock(SubmissionNoveltyRepository.class);
        when(noveltyRepository.findBySubmissionId(any())).thenReturn(Optional.empty());
        service = new SubmissionWorkflowService(submissionService, restClientService, processService,
                new HashMapConverter(), noveltyRepository);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static CreateSubmissionRequest buildRequest() {
        CreateSubmissionRequest req = new CreateSubmissionRequest();
        req.setTitle("On the Origins of Bugs");
        req.setAuthors("A. Author");
        req.setAbstractText("This paper investigates the root causes of software defects.");
        req.setTopic(SubmissionTopic.IMAGING_INFORMATICS.name());
        req.setSubmitterEmail("author@example.com");
        return req;
    }

    @Nested
    @DisplayName("startSubmissionWorkflow")
    class StartSubmissionWorkflowTests {

        @Test
        @DisplayName("returns the saved submission when the engine accepts the process start")
        void success() {
            ProcessInstanceWithVariableDto engineResponse = new ProcessInstanceWithVariableDto();
            engineResponse.setId("proc-123");
            engineResponse.setDefinitionId("abstract_submission_lifecycle:1:abc");
            restClientService.configureSuccess(engineResponse);

            Submission result = service.startSubmissionWorkflow(buildRequest(), "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(submissionService.getSavedSubmissionId());
            assertThat(processService.isActivateCalled()).isTrue();
            assertThat(submissionService.getDeletedSubmissionId()).isNull();
            assertThat(processService.getDeletedResponsibleForId()).isNull();
        }

        @Test
        @DisplayName("deletes submission and process entity when the engine rejects the process start")
        void engineFailure_rollsBackAndThrows() {
            restClientService.configureFailure("tenant 'cfp' not found");

            assertThatThrownBy(() -> service.startSubmissionWorkflow(buildRequest(), "testuser"))
                    .isInstanceOf(ProcessStartException.class)
                    .hasMessageContaining("tenant 'cfp' not found");

            UUID savedId = submissionService.getSavedSubmissionId();
            assertThat(submissionService.getDeletedSubmissionId())
                    .as("submission must be deleted on engine failure")
                    .isEqualTo(savedId);
            assertThat(processService.getDeletedResponsibleForId())
                    .as("process entity must be deleted on engine failure")
                    .isEqualTo(savedId);
            assertThat(processService.isActivateCalled())
                    .as("activateProcess must not be called when engine failed")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("mapReceiveSubmissionVars")
    class ReceiveSubmissionTests {

        @Test
        @DisplayName("the validation result decides submission_valid")
        void validationResult() {
            assertThat(service.mapReceiveSubmissionVars(Map.of("validationResult", "VALID")))
                    .containsEntry("submission_valid", true);
            assertThat(service.mapReceiveSubmissionVars(Map.of("validationResult", "INVALID")))
                    .containsEntry("submission_valid", false);
        }

        @Test
        @DisplayName("a task created before the rename still completes with reviewDecision")
        void legacyReviewDecision() {
            assertThat(service.mapReceiveSubmissionVars(Map.of("reviewDecision", "APPROVED")))
                    .containsEntry("submission_valid", true);
        }
    }

    @Nested
    @DisplayName("assignReviewers")
    class AssignReviewersTests {

        /** TaskDto has only its all-args constructor. */
        private TaskDto emptyTask() {
            return new TaskDto(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        @Test
        @DisplayName("shows the novelty check between the submission and the abstract")
        @SuppressWarnings("unchecked")
        void showsNoveltyCheck() throws Exception {
            SubmissionNovelty n = new SubmissionNovelty();
            n.setStatus(SubmissionNovelty.Status.DONE);
            n.setVerdict("likely_novel");
            n.setAddsNew("partly");
            n.setFlagged(true);
            n.setResultJson("""
                    {"screening": {"verdict": "likely_novel", "top_matches": [{"title": "Earlier DWI study", "year": 2021, "similarity": 0.81}]},
                     "novelty_q": {"adds_new": "partly", "adds_what": "Only a slightly larger cohort."}}""");
            when(noveltyRepository.findBySubmissionId(any())).thenReturn(Optional.of(n));

            TaskDto task = service.assignReviewers(UUID.randomUUID(), "chair", emptyTask());
            Map<String, Object> info = new ObjectMapper().readValue(task.getAdditionalInfo(), LinkedHashMap.class);

            assertThat(info.keySet()).containsExactly("Submission", "Novelty check", "Content");
            Map<String, String> novelty = (Map<String, String>) info.get("Novelty check");
            assertThat(novelty.get("Result")).startsWith("Flagged");
            assertThat(novelty.get("Closest earlier work")).isEqualTo("Earlier DWI study (2021, similarity 0.81)");
            assertThat(novelty.get("Larger model")).isEqualTo("Only partly new: Only a slightly larger cohort.");
        }

        @Test
        @DisplayName("says so when the check did not run")
        @SuppressWarnings("unchecked")
        void notChecked() throws Exception {
            TaskDto task = service.assignReviewers(UUID.randomUUID(), "chair", emptyTask());
            Map<String, Object> info = new ObjectMapper().readValue(task.getAdditionalInfo(), LinkedHashMap.class);
            assertThat((Map<String, String>) info.get("Novelty check")).containsEntry("Status", "Not checked");
        }
    }

    // -------------------------------------------------------------------------
    // Stubs
    // -------------------------------------------------------------------------

    private static class StubSubmissionService extends SubmissionService {

        private UUID savedSubmissionId;
        private UUID deletedSubmissionId;

        StubSubmissionService() {
            super(null, null);
        }

        @Override
        public Submission saveSubmission(String title, String authors, String abstractText,
                                         List<SectionDto> sections,
                                         String topic, String submitterEmail, String createdBy) {
            Submission s = new Submission();
            s.setId(UUID.randomUUID());
            s.setTitle(title);
            s.setTopic(SubmissionTopic.valueOf(topic));
            savedSubmissionId = s.getId();
            return s;
        }

        @Override
        public Submission getSubmission(UUID submissionId) {
            Submission s = new Submission();
            s.setId(submissionId);
            s.setTitle("A title");
            s.setAuthors("A. Author");
            s.setTopic(SubmissionTopic.NEURO);
            s.setAbstractText("Purpose: something.");
            return s;
        }

        @Override
        public void deleteSubmission(UUID submissionId) {
            deletedSubmissionId = submissionId;
        }

        UUID getSavedSubmissionId() { return savedSubmissionId; }
        UUID getDeletedSubmissionId() { return deletedSubmissionId; }
    }

    private static class StubProcessService extends ProcessService {

        private UUID deletedResponsibleForId;
        private boolean activateCalled = false;

        StubProcessService() {
            super(null, null, null, null, null);
        }

        @Override
        public ProcessEntity preCreateProcess(Processable processable, String tenantId) {
            return new ProcessEntity();
        }

        @Override
        public void deleteByResponsibleForId(UUID responsibleForId) {
            deletedResponsibleForId = responsibleForId;
        }

        @Override
        public ProcessDto activateProcess(UUID responsibleForId, String processInstanceId, String processDefinitionId) {
            activateCalled = true;
            return null;
        }

        UUID getDeletedResponsibleForId() { return deletedResponsibleForId; }
        boolean isActivateCalled() { return activateCalled; }
    }

    private static class StubRestClientService extends RestClientService {

        private boolean shouldFail = false;
        private String failureMessage;
        private ProcessInstanceWithVariableDto successResponse;

        StubRestClientService() {
            super(RestClient.builder());
        }

        void configureSuccess(ProcessInstanceWithVariableDto response) {
            this.successResponse = response;
        }

        void configureFailure(String message) {
            this.shouldFail = true;
            this.failureMessage = message;
        }

        @Override
        public ProcessInstanceWithVariableDto startCib7Process(String processDefinitionKey,
                                                                StartProcessBody body,
                                                                String tenantId) {
            if (shouldFail) {
                throw new RuntimeException(failureMessage);
            }
            return successResponse;
        }
    }
}
