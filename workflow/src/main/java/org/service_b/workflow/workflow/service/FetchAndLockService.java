package org.service_b.workflow.workflow.service;

import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.dto.CompleteExternalTask;
import org.service_b.workflow.workflow.dto.FetchAndLock;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import org.service_b.workflow.workflow.dto.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Fetches external tasks for the pollers. When a topic came back empty
 * {@code emptyFetchesBeforeLongPoll} times in a row, the next fetch long-polls: the engine
 * holds it up to {@code longPollMs} and answers as soon as a task appears. A fetched task
 * resets the count, so a poller drains waiting tasks with immediate fetches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FetchAndLockService {

    private final RestClientService restClientService;
    private final CibSevenProperties cibSevenProperties;
    /** Empty fetches in a row, per topic. */
    private final Map<String, Integer> emptyFetches = new ConcurrentHashMap<>();
    private final AtomicBoolean engineReachable = new AtomicBoolean(true);
    private static final String FETCH_AND_LOCK_URL = "/external-task/fetchAndLock";

    /** The locked task, or null when none is waiting or the engine cannot be reached. */
    public FetchAndLockResponse fetchAndLockExternalTask(FetchAndLock fetchAndLock) {
        String topics = topics(fetchAndLock);
        boolean longPoll = emptyFetches.getOrDefault(topics, 0) >= cibSevenProperties.getEmptyFetchesBeforeLongPoll();
        fetchAndLock.setAsyncResponseTimeout(longPoll ? cibSevenProperties.getLongPollMs() : null);

        FetchAndLockResponse task;
        try {
            task = restClientService.fetchAndLock(fetchAndLock);
        } catch (ResourceAccessException e) {
            // once per outage, not once per poller and tick
            if (engineReachable.compareAndSet(true, false)) {
                log.warn("Engine not reachable at {} — external tasks wait until it is back: {}",
                        cibSevenProperties.getBaseUrl(), e.getMessage());
            }
            return null;
        } catch (RestClientException e) {
            log.warn("Fetching {} failed: {}", topics, e.getMessage());
            return null;
        }
        if (engineReachable.compareAndSet(false, true)) {
            log.info("Engine reachable again at {}", cibSevenProperties.getBaseUrl());
        }

        if (task == null) {
            emptyFetches.merge(topics, 1, Integer::sum);
            return null;
        }
        emptyFetches.remove(topics);
        log.info("Fetched external task {} ({})", task.getId(), task.getTopicName());
        return task;
    }

    private static String topics(FetchAndLock fetchAndLock) {
        return Arrays.stream(fetchAndLock.getTopics()).map(Topic::getTopicName).collect(Collectors.joining(","));
    }

    public void completeExternalTask(String taskId, CompleteExternalTask completeExternalTask) {
        // restClientService.completeExternalTask(taskId, completeExternalTask);
    }

}
