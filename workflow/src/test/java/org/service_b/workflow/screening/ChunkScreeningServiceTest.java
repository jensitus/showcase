package org.service_b.workflow.screening;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.JobStatus;
import org.service_b.workflow.screening.dto.ScreenOutcome;
import org.service_b.workflow.screening.dto.ScreenResponse;
import org.service_b.workflow.screening.persistence.ScreeningChunk;
import org.service_b.workflow.screening.persistence.ScreeningChunkRepository;
import org.service_b.workflow.screening.service.ChunkScreeningService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChunkScreeningServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private NoveltyProperties props() {
        NoveltyProperties p = new NoveltyProperties();
        p.setChunkSize(2);
        p.setPollIntervalMs(1);
        p.setChunkMaxWaitMs(5000);
        return p;
    }

    private JobStatus doneJob(String verdict) {
        JobStatus s = new JobStatus();
        s.setStatus("done");
        s.setResults(List.of(Map.of("verdict", verdict)));
        return s;
    }

    private void writeSubs(Path p, int n) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("{\"id\":\"s").append(i).append("\",\"title\":\"t\",\"abstract\":\"a\"}\n");
        }
        Files.writeString(p, sb.toString());
    }

    @Test
    void screensAllChunksAndCheckpoints(@TempDir Path dir) throws Exception {
        Path subs = dir.resolve("submissions.jsonl");
        Path results = dir.resolve("results.jsonl");
        writeSubs(subs, 3);  // chunkSize 2 -> chunks [2, 1]

        NoveltyApiClient client = mock(NoveltyApiClient.class);
        ScreenResponse resp = new ScreenResponse();
        resp.setJobId("j");
        when(client.screenBatch(any(), any(), any())).thenReturn(resp);
        when(client.getJob(anyString())).thenReturn(doneJob("likely_duplicate"));

        ScreeningChunkRepository repo = mock(ScreeningChunkRepository.class);
        when(repo.existsByBatchIdAndChunkNoAndStatus(anyString(), anyInt(), anyString())).thenReturn(false);
        when(repo.findByBatchIdAndChunkNo(anyString(), anyInt())).thenReturn(java.util.Optional.empty());

        ChunkScreeningService svc = new ChunkScreeningService(client, repo, props(), mapper);
        ScreenOutcome outcome = svc.run("b1", subs, results, () -> { }, null, null);

        assertEquals(3, outcome.screened());
        assertEquals(2, outcome.flagged());                 // 2 chunks x 1 flagged result
        assertEquals(2, Files.readAllLines(results).size());
        verify(client, times(2)).screenBatch(any(), any(), any());        // one call per chunk
        verify(repo, times(2)).save(any(ScreeningChunk.class));
    }

    @Test
    void passesCorpusAndCutoffToTheClient(@TempDir Path dir) throws Exception {
        // The whole point of the feature: if corpus or beforeYear is dropped anywhere
        // between the trigger and the HTTP call, a congress is screened against a corpus
        // containing itself and every abstract comes back a duplicate of itself. The
        // pipeline cannot tell us that happened, so assert it here.
        Path subs = dir.resolve("submissions.jsonl");
        Path results = dir.resolve("results.jsonl");
        writeSubs(subs, 2);  // chunkSize 2 -> exactly one chunk

        NoveltyApiClient client = mock(NoveltyApiClient.class);
        ScreenResponse resp = new ScreenResponse();
        resp.setJobId("j");
        resp.setBeforeYear(2026);
        when(client.screenBatch(any(), any(), any())).thenReturn(resp);
        when(client.getJob(anyString())).thenReturn(doneJob("likely_novel"));

        ScreeningChunkRepository repo = mock(ScreeningChunkRepository.class);
        when(repo.existsByBatchIdAndChunkNoAndStatus(anyString(), anyInt(), anyString())).thenReturn(false);
        when(repo.findByBatchIdAndChunkNo(anyString(), anyInt())).thenReturn(java.util.Optional.empty());

        ChunkScreeningService svc = new ChunkScreeningService(client, repo, props(), mapper);
        svc.run("b1", subs, results, () -> { }, "congress-A2", 2026);

        verify(client).screenBatch(any(), eq("congress-A2"), eq(2026));
    }

    @Test
    void resumesSkippingDoneChunks(@TempDir Path dir) throws Exception {
        Path subs = dir.resolve("submissions.jsonl");
        Path results = dir.resolve("results.jsonl");
        writeSubs(subs, 3);  // chunks [2, 1]

        NoveltyApiClient client = mock(NoveltyApiClient.class);
        ScreenResponse resp = new ScreenResponse();
        resp.setJobId("j");
        when(client.screenBatch(any(), any(), any())).thenReturn(resp);
        when(client.getJob(anyString())).thenReturn(doneJob("possible_overlap"));

        ScreeningChunkRepository repo = mock(ScreeningChunkRepository.class);
        when(repo.existsByBatchIdAndChunkNoAndStatus(eq("b1"), eq(0), anyString())).thenReturn(true);  // chunk 0 done
        when(repo.existsByBatchIdAndChunkNoAndStatus(eq("b1"), eq(1), anyString())).thenReturn(false);
        when(repo.findByBatchIdAndChunkNo(anyString(), anyInt())).thenReturn(java.util.Optional.empty());

        ChunkScreeningService svc = new ChunkScreeningService(client, repo, props(), mapper);
        svc.run("b1", subs, results, () -> { }, null, null);

        verify(client, times(1)).screenBatch(any(), any(), any());   // only chunk 1 screened
        verify(repo, times(1)).save(any(ScreeningChunk.class));
    }
}
