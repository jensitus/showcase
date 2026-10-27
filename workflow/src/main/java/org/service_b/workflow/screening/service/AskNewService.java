package org.service_b.workflow.screening.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.client.NoveltyApiClient;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.AskOutcome;
import org.service_b.workflow.screening.dto.JobStatus;
import org.service_b.workflow.screening.dto.ScreenResponse;
import org.service_b.workflow.screening.persistence.ScreeningChunk;
import org.service_b.workflow.screening.persistence.ScreeningChunkRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The second step of the process: asks the pipeline's larger model (qwen3.6 35B) whether
 * each abstract that screening let through adds anything new. Validated on the 2026
 * congress: with it, 92.4% of the abstracts three or more reviewers called "not new" are
 * flagged, against 64.8% from screening alone.
 *
 * <p>Same shape as {@link ChunkScreeningService}: chunks, a checkpoint per chunk, an
 * injected lock keep-alive. Each submission goes with its screening result, which is the
 * evidence the model sees (its closest earlier abstracts); the pipeline skips the ones
 * screening already flagged, so a chunk of mostly flagged abstracts is quick.
 *
 * <p>Checkpoints share the screening_chunk table under the key {@code <batchId>:ask}, so
 * they never collide with the screen step's chunks of the same batch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AskNewService {

    private final NoveltyApiClient client;
    private final ScreeningChunkRepository chunkRepo;
    private final NoveltyProperties props;
    private final ObjectMapper mapper;

    /**
     * @param resultsPath   the screen step's results.jsonl (one NoveltyReport per submission)
     * @param answersPath   where the answers are appended (ask_new.jsonl)
     * @param lockKeepAlive called while polling and after each chunk
     */
    public AskOutcome run(String batchId, Path submissionsPath, Path resultsPath, Path answersPath,
                          Runnable lockKeepAlive) throws IOException, InterruptedException {
        String key = batchId + ":ask";
        List<Map<String, Object>> items = items(submissionsPath, resultsPath);
        List<List<Map<String, Object>>> chunks = partition(items, props.getAskChunkSize());
        log.info("[ask] batch {}: {} submissions in {} chunks of {}",
                batchId, items.size(), chunks.size(), props.getAskChunkSize());

        dropUnaccountedAnswers(key, answersPath);

        for (int i = 0; i < chunks.size(); i++) {
            if (chunkRepo.existsByBatchIdAndChunkNoAndStatus(key, i, ScreeningChunk.DONE)) {
                log.info("[ask] batch {} chunk {} already done — skipping", batchId, i);
                continue;
            }
            List<Map<String, Object>> chunk = chunks.get(i);
            ScreenResponse job = client.askNew(chunk);
            JobStatus status = pollUntilDone(job.getJobId(), lockKeepAlive);
            appendAnswers(answersPath, status.getResults());
            markDone(key, i, chunk.size());
            lockKeepAlive.run();
            log.info("[ask] batch {} chunk {}/{} done", batchId, i + 1, chunks.size());
        }
        return outcome(answersPath);
    }

    /** {submission, results: [its screening result]} per submission, in submission order. */
    private List<Map<String, Object>> items(Path submissionsPath, Path resultsPath) throws IOException {
        Map<String, Map<String, Object>> resultById = new HashMap<>();
        for (Map<String, Object> r : readJsonl(resultsPath)) {
            resultById.put(String.valueOf(r.get("submission_id")), r);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> sub : readJsonl(submissionsPath)) {
            Map<String, Object> result = resultById.get(String.valueOf(sub.get("id")));
            if (result == null) {
                throw new IllegalStateException("submission " + sub.get("id") + " has no screening "
                        + "result — the screen step must cover every submission before the ask step");
            }
            items.add(Map.of("submission", sub, "results", List.of(result)));
        }
        return items;
    }

    private JobStatus pollUntilDone(String jobId, Runnable lockKeepAlive) throws InterruptedException {
        long deadline = System.currentTimeMillis() + props.getAskChunkMaxWaitMs();
        while (true) {
            JobStatus status = client.getAskJob(jobId);
            if (status != null && status.isCancelled()) {
                throw new IllegalStateException("ask job " + jobId + " was cancelled in the pipeline");
            }
            if (status != null && status.isDone()) {
                return status;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("ask job " + jobId + " did not finish within "
                        + props.getAskChunkMaxWaitMs() + "ms");
            }
            lockKeepAlive.run();
            Thread.sleep(props.getPollIntervalMs());
        }
    }

    private void appendAnswers(Path answersPath, List<Map<String, Object>> answers) throws IOException {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        Files.createDirectories(answersPath.getParent());
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> a : answers) {
            sb.append(mapper.writeValueAsString(a)).append('\n');
        }
        Files.writeString(answersPath, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** As in ChunkScreeningService: answers appended by a chunk that never checkpointed go. */
    private void dropUnaccountedAnswers(String key, Path answersPath) throws IOException {
        if (!Files.exists(answersPath)) {
            return;
        }
        int accounted = chunkRepo.findByBatchIdAndStatus(key, ScreeningChunk.DONE).stream()
                .mapToInt(ScreeningChunk::getScreenedCount)
                .sum();
        List<String> lines = Files.readAllLines(answersPath).stream()
                .filter(l -> !l.isBlank())
                .toList();
        if (lines.size() <= accounted) {
            return;
        }
        log.warn("[ask] {}: {} answer line(s) belong to no completed chunk — discarding them before resuming",
                key, lines.size() - accounted);
        Files.write(answersPath, lines.subList(0, accounted));
    }

    private void markDone(String key, int chunkNo, int count) {
        ScreeningChunk c = chunkRepo.findByBatchIdAndChunkNo(key, chunkNo)
                .orElseGet(ScreeningChunk::new);
        c.setBatchId(key);
        c.setChunkNo(chunkNo);
        c.setStatus(ScreeningChunk.DONE);
        c.setScreenedCount(count);
        c.setUpdatedAt(Instant.now());
        chunkRepo.save(c);
    }

    private AskOutcome outcome(Path answersPath) throws IOException {
        int asked = 0, nothing = 0, partly = 0, errors = 0;
        for (Map<String, Object> a : readJsonl(answersPath)) {
            if (a.containsKey("error")) {
                errors++;
            } else if (!a.containsKey("skipped")) {
                asked++;
                String answer = String.valueOf(a.get("adds_new"));
                nothing += "no".equals(answer) ? 1 : 0;
                partly += "partly".equals(answer) ? 1 : 0;
            }
        }
        return new AskOutcome(asked, nothing, partly, errors);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readJsonl(Path path) throws IOException {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!Files.exists(path)) {
            return out;
        }
        for (String line : Files.readAllLines(path)) {
            if (!line.isBlank()) {
                out.add(mapper.readValue(line, Map.class));
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
