package org.service_b.workflow.screening.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.dto.JobStatus;
import org.service_b.workflow.screening.dto.ScreenOutcome;
import org.service_b.workflow.screening.dto.ScreenResponse;
import org.service_b.workflow.screening.dto.Submission;
import org.service_b.workflow.screening.persistence.ScreeningChunk;
import org.service_b.workflow.screening.persistence.ScreeningChunkRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Screens a batch in chunks, checkpointing each so a long / interrupted run resumes
 * from where it left off. Engine concerns (the external-task lock) are injected as a
 * {@code lockKeepAlive} callback, so this core is unit-testable with a mocked client
 * and repository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkScreeningService {

    private static final Set<String> FLAG = Set.of("possible_overlap", "likely_duplicate");

    private final NoveltyApiClient client;
    private final ScreeningChunkRepository chunkRepo;
    private final NoveltyProperties props;
    private final ObjectMapper mapper;

    /**
     * @param lockKeepAlive called after each chunk (and while polling) to extend the
     *                      external-task lock so a multi-hour run doesn't lose it.
     */
    public ScreenOutcome run(String batchId, Path submissionsPath, Path resultsPath, Runnable lockKeepAlive)
            throws IOException, InterruptedException {
        List<Submission> submissions = readSubmissions(submissionsPath);
        List<List<Submission>> chunks = partition(submissions, props.getChunkSize());
        log.info("[screen] batch {}: {} submissions in {} chunks of {}",
                batchId, submissions.size(), chunks.size(), props.getChunkSize());

        for (int i = 0; i < chunks.size(); i++) {
            if (chunkRepo.existsByBatchIdAndChunkNoAndStatus(batchId, i, ScreeningChunk.DONE)) {
                log.info("[screen] batch {} chunk {} already done — skipping", batchId, i);
                continue;
            }
            List<Submission> chunk = chunks.get(i);
            ScreenResponse job = client.screenBatch(chunk);
            JobStatus status = pollUntilDone(job.getJobId(), lockKeepAlive);
            appendResults(resultsPath, status.getResults());
            markDone(batchId, i, chunk.size());
            lockKeepAlive.run();
            log.info("[screen] batch {} chunk {}/{} done", batchId, i + 1, chunks.size());
        }

        int flagged = countFlagged(resultsPath);
        return new ScreenOutcome(submissions.size(), flagged);
    }

    private JobStatus pollUntilDone(String jobId, Runnable lockKeepAlive) throws InterruptedException {
        long deadline = System.currentTimeMillis() + props.getChunkMaxWaitMs();
        while (true) {
            JobStatus status = client.getJob(jobId);
            if (status != null && status.isDone()) {
                return status;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("screening job " + jobId + " did not finish within "
                        + props.getChunkMaxWaitMs() + "ms");
            }
            lockKeepAlive.run();
            Thread.sleep(props.getPollIntervalMs());
        }
    }

    private void appendResults(Path resultsPath, List<Map<String, Object>> results) throws IOException {
        if (results == null || results.isEmpty()) {
            return;
        }
        Files.createDirectories(resultsPath.getParent());
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> r : results) {
            sb.append(mapper.writeValueAsString(r)).append('\n');
        }
        Files.writeString(resultsPath, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void markDone(String batchId, int chunkNo, int count) {
        ScreeningChunk c = chunkRepo.findByBatchIdAndChunkNo(batchId, chunkNo)
                .orElseGet(ScreeningChunk::new);
        c.setBatchId(batchId);
        c.setChunkNo(chunkNo);
        c.setStatus(ScreeningChunk.DONE);
        c.setScreenedCount(count);
        c.setUpdatedAt(Instant.now());
        chunkRepo.save(c);
    }

    private int countFlagged(Path resultsPath) throws IOException {
        if (!Files.exists(resultsPath)) {
            return 0;
        }
        int flagged = 0;
        for (String line : Files.readAllLines(resultsPath)) {
            if (line.isBlank()) {
                continue;
            }
            Map<String, Object> r = mapper.readValue(line, Map.class);
            if (FLAG.contains(String.valueOf(r.get("verdict")))) {
                flagged++;
            }
        }
        return flagged;
    }

    private List<Submission> readSubmissions(Path path) throws IOException {
        List<Submission> out = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (!line.isBlank()) {
                out.add(mapper.readValue(line, Submission.class));
            }
        }
        return out;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }
}
