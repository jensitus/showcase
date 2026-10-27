package org.service_b.workflow.submission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.JobStatus;
import org.service_b.workflow.screening.dto.ScreenResponse;
import org.service_b.workflow.submission.persistence.Submission;
import org.service_b.workflow.submission.persistence.SubmissionNovelty;
import org.service_b.workflow.submission.persistence.SubmissionNoveltyRepository;
import org.service_b.workflow.submission.service.NoveltyCheckService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoveltyCheckServiceTest {

    private NoveltyApiClient client;
    private SubmissionNoveltyRepository repository;
    private NoveltyCheckService service;
    private Submission submission;

    @BeforeEach
    void setUp() {
        client = mock(NoveltyApiClient.class);
        repository = mock(SubmissionNoveltyRepository.class);
        when(repository.findBySubmissionId(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        NoveltyProperties props = new NoveltyProperties();
        props.setPollIntervalMs(1);
        props.setAskChunkMaxWaitMs(5000);
        service = new NoveltyCheckService(client, repository, props, new ObjectMapper());

        submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setTitle("DWI for prostate cancer");
        submission.setAbstractText("Purpose or Learning Objective: To assess DWI.");
        ScreenResponse job = new ScreenResponse();
        job.setJobId("j");
        when(client.askNew(any())).thenReturn(job);
    }

    private void screening(String verdict) {
        when(client.check(anyString(), anyString(), anyString()))
                .thenReturn(Map.of("verdict", verdict, "top_matches", List.of()));
    }

    private void answer(Map<String, Object> row) {
        JobStatus s = new JobStatus();
        s.setStatus("done");
        s.setResults(List.of(row));
        when(client.getAskJob("j")).thenReturn(s);
    }

    /** The row as screen() saved it, served back to the next step. */
    private void stored() {
        when(repository.findBySubmissionId(submission.getId())).thenAnswer(inv -> Optional.ofNullable(saved));
        when(repository.save(any())).thenAnswer(inv -> saved = inv.getArgument(0));
    }

    private SubmissionNovelty saved;

    @Test
    void screeningFlagsIt_theCheckIsComplete() {
        screening("possible_overlap");

        SubmissionNovelty n = service.screen(submission);

        assertThat(n.getStatus()).isEqualTo(SubmissionNovelty.Status.DONE);
        assertThat(n.getFlagged()).isTrue();
        assertThat(n.getResultJson()).contains("\"novelty_q\":null");
        verify(client, never()).askNew(any());
    }

    @Test
    void screeningLetsItThrough_theLargerModelDecides() throws Exception {
        stored();
        screening("likely_novel");
        SubmissionNovelty screened = service.screen(submission);
        assertThat(screened.getStatus()).isEqualTo(SubmissionNovelty.Status.SCREENED);
        assertThat(screened.getFlagged()).isFalse();

        answer(Map.of("key", "x", "adds_new", "partly", "adds_what", "A slightly larger cohort."));
        SubmissionNovelty n = service.ask(submission, () -> { });

        assertThat(n.getStatus()).isEqualTo(SubmissionNovelty.Status.DONE);
        assertThat(n.getAddsNew()).isEqualTo("partly");
        assertThat(n.getFlagged()).isTrue();                      // "partly" counts as flagged
        assertThat(n.getResultJson()).contains("likely_novel").contains("A slightly larger cohort.");
    }

    @Test
    void theLargerModelSayingYesLeavesItUnflagged() throws Exception {
        stored();
        screening("likely_novel");
        service.screen(submission);
        answer(Map.of("key", "x", "adds_new", "yes", "adds_what", "A new method."));

        assertThat(service.ask(submission, () -> { }).getFlagged()).isFalse();
    }

    @Test
    void aScreenedSubmissionIsNotSentAgain() {
        stored();
        screening("possible_overlap");
        service.screen(submission);
        service.screen(submission);                               // a re-fetched task

        verify(client, times(1)).check(anyString(), anyString(), anyString());
    }

    @Test
    void aFailedAnswerIsThrownSoTheWorkerRetries() {
        stored();
        screening("likely_novel");
        service.screen(submission);
        answer(Map.of("key", "x", "error", "TimeoutError"));

        assertThatThrownBy(() -> service.ask(submission, () -> { })).hasMessageContaining("TimeoutError");
        assertThat(saved.getStatus()).isEqualTo(SubmissionNovelty.Status.SCREENED);   // nothing half-written
    }

    @Test
    void unavailableAfterScreeningKeepsTheVerdict() {
        stored();
        screening("likely_novel");
        service.screen(submission);

        SubmissionNovelty n = service.markUnavailable(submission, "Connection refused");

        assertThat(n.getStatus()).isEqualTo(SubmissionNovelty.Status.UNAVAILABLE);
        assertThat(n.getVerdict()).isEqualTo("likely_novel");
        assertThat(n.getError()).isEqualTo("Connection refused");
    }
}
