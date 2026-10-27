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

    @Test
    void screeningFlagsIt_theLargerModelIsNotAsked() throws Exception {
        screening("possible_overlap");

        SubmissionNovelty n = service.check(submission, () -> { });

        assertThat(n.getFlagged()).isTrue();
        assertThat(n.getAddsNew()).isNull();
        assertThat(n.getResultJson()).contains("\"novelty_q\":null");
        verify(client, never()).askNew(any());
    }

    @Test
    void screeningLetsItThrough_theLargerModelDecides() throws Exception {
        screening("likely_novel");
        answer(Map.of("key", "x", "adds_new", "partly", "adds_what", "A slightly larger cohort."));

        SubmissionNovelty partly = service.check(submission, () -> { });
        assertThat(partly.getStatus()).isEqualTo(SubmissionNovelty.Status.DONE);
        assertThat(partly.getAddsNew()).isEqualTo("partly");
        assertThat(partly.getFlagged()).isTrue();                 // "partly" counts as flagged

        answer(Map.of("key", "x", "adds_new", "yes", "adds_what", "A new method."));
        assertThat(service.check(submission, () -> { }).getFlagged()).isFalse();
    }

    @Test
    void anAlreadyCheckedSubmissionIsNotSentAgain() throws Exception {
        SubmissionNovelty done = new SubmissionNovelty();
        done.setStatus(SubmissionNovelty.Status.DONE);
        when(repository.findBySubmissionId(submission.getId())).thenReturn(Optional.of(done));

        assertThat(service.check(submission, () -> { })).isSameAs(done);
        verify(client, never()).check(anyString(), anyString(), anyString());
    }

    @Test
    void aFailedAnswerIsThrownSoTheWorkerRetries() {
        screening("likely_novel");
        answer(Map.of("key", "x", "error", "TimeoutError"));

        assertThatThrownBy(() -> service.check(submission, () -> { }))
                .hasMessageContaining("TimeoutError");
        verify(repository, never()).save(any());
    }
}
