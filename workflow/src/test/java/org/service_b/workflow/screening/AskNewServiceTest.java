package org.service_b.workflow.screening;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.AskOutcome;
import org.service_b.workflow.screening.dto.JobStatus;
import org.service_b.workflow.screening.dto.ScreenResponse;
import org.service_b.workflow.screening.persistence.ScreeningChunk;
import org.service_b.workflow.screening.persistence.ScreeningChunkRepository;
import org.service_b.workflow.screening.service.AskNewService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AskNewServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private NoveltyProperties props() {
        NoveltyProperties p = new NoveltyProperties();
        p.setAskChunkSize(2);
        p.setPollIntervalMs(1);
        p.setAskChunkMaxWaitMs(5000);
        return p;
    }

    private JobStatus job(String status, List<Map<String, Object>> results) {
        JobStatus s = new JobStatus();
        s.setStatus(status);
        s.setResults(results);
        return s;
    }

    /** 3 submissions, each with a screening result; chunk size 2 -> chunks [2, 1]. */
    private void writeBatch(Path dir) throws Exception {
        StringBuilder subs = new StringBuilder();
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            subs.append("{\"id\":\"s").append(i).append("\",\"title\":\"t\",\"abstract\":\"a\"}\n");
            results.append("{\"submission_id\":\"s").append(i)
                    .append("\",\"verdict\":\"likely_novel\",\"top_matches\":[]}\n");
        }
        Files.writeString(dir.resolve("submissions.jsonl"), subs.toString());
        Files.writeString(dir.resolve("results.jsonl"), results.toString());
    }

    private ScreeningChunkRepository repo() {
        ScreeningChunkRepository repo = mock(ScreeningChunkRepository.class);
        when(repo.findByBatchIdAndChunkNo(anyString(), anyInt())).thenReturn(Optional.empty());
        return repo;
    }

    @Test
    @SuppressWarnings("unchecked")
    void asksEveryChunkWithItsScreeningResultsAndCounts(@TempDir Path dir) throws Exception {
        writeBatch(dir);
        NoveltyApiClient client = mock(NoveltyApiClient.class);
        ScreenResponse resp = new ScreenResponse();
        resp.setJobId("j");
        when(client.askNew(any())).thenReturn(resp);
        when(client.getAskJob(anyString())).thenReturn(
                job("running", List.of()),
                job("done", List.of(Map.of("key", "s0", "adds_new", "no"),
                                    Map.of("key", "s1", "adds_new", "partly"))),
                job("done", List.of(Map.of("key", "s2", "error", "TimeoutError"))));
        ScreeningChunkRepository repo = repo();

        AskOutcome out = new AskNewService(client, repo, props(), mapper).run("b1",
                dir.resolve("submissions.jsonl"), dir.resolve("results.jsonl"),
                dir.resolve("ask_new.jsonl"), () -> { });

        assertEquals(new AskOutcome(2, 1, 1, 1), out);
        assertEquals(3, Files.readAllLines(dir.resolve("ask_new.jsonl")).size());
        ArgumentCaptor<List<Map<String, Object>>> sent = ArgumentCaptor.forClass(List.class);
        verify(client, times(2)).askNew(sent.capture());
        Map<String, Object> first = sent.getAllValues().get(0).get(0);
        assertEquals("s0", ((Map<String, Object>) first.get("submission")).get("id"));
        assertEquals("s0", ((Map<String, Object>) ((List<?>) first.get("results")).get(0)).get("submission_id"));
        // checkpoints under their own key, never the screen step's
        verify(repo, times(2)).save(any(ScreeningChunk.class));
        verify(repo).findByBatchIdAndChunkNo("b1:ask", 0);
    }

    @Test
    void resumesSkippingDoneChunks(@TempDir Path dir) throws Exception {
        writeBatch(dir);
        NoveltyApiClient client = mock(NoveltyApiClient.class);
        ScreenResponse resp = new ScreenResponse();
        resp.setJobId("j");
        when(client.askNew(any())).thenReturn(resp);
        when(client.getAskJob(anyString())).thenReturn(job("done", List.of(Map.of("key", "s2", "adds_new", "yes"))));
        ScreeningChunkRepository repo = repo();
        when(repo.existsByBatchIdAndChunkNoAndStatus(eq("b1:ask"), eq(0), anyString())).thenReturn(true);

        new AskNewService(client, repo, props(), mapper).run("b1",
                dir.resolve("submissions.jsonl"), dir.resolve("results.jsonl"),
                dir.resolve("ask_new.jsonl"), () -> { });

        verify(client, times(1)).askNew(any());     // only chunk 1
    }

    @Test
    void refusesWhenScreeningDidNotCoverEverySubmission(@TempDir Path dir) throws Exception {
        writeBatch(dir);
        Files.writeString(dir.resolve("results.jsonl"),
                "{\"submission_id\":\"s0\",\"verdict\":\"likely_novel\",\"top_matches\":[]}\n");
        AskNewService svc = new AskNewService(mock(NoveltyApiClient.class), repo(), props(), mapper);

        assertThrows(IllegalStateException.class, () -> svc.run("b1",
                dir.resolve("submissions.jsonl"), dir.resolve("results.jsonl"),
                dir.resolve("ask_new.jsonl"), () -> { }));
    }
}
