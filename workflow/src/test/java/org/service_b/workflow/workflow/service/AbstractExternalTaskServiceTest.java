package org.service_b.workflow.workflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.config.ExternalTaskConfig;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractExternalTaskServiceTest {

    private FetchAndLockService fetchAndLockService;
    private CibSevenRestClient cibSevenRestClient;
    private Worker worker;

    /** One topic whose handler fails or succeeds as the test says. */
    private static class Worker extends AbstractExternalTaskService {
        RuntimeException failure;

        Worker(FetchAndLockService f, CibSevenProperties p, CibSevenRestClient c) {
            super(f, p, c);
        }

        void poll() {
            processExternalTask(Map.of("t", new ExternalTaskConfig.TaskDefinition("topic", "tenant", vars -> {
                if (failure != null) {
                    throw failure;
                }
                return Map.of();
            })), "t");
        }
    }

    @BeforeEach
    void setUp() {
        fetchAndLockService = mock(FetchAndLockService.class);
        cibSevenRestClient = mock(CibSevenRestClient.class);
        CibSevenProperties props = new CibSevenProperties();
        props.setWorkerId("w");
        props.setDefaultRetries(3);
        props.setDefaultRetryTimeout(60000);
        worker = new Worker(fetchAndLockService, props, cibSevenRestClient);
    }

    private void waiting(String retries) {
        FetchAndLockResponse task = new FetchAndLockResponse();
        task.setId("task");
        task.setTopicName("topic");
        task.setRetries(retries);
        task.setVariables(Map.of());
        when(fetchAndLockService.fetchAndLockExternalTask(any())).thenReturn(task);
    }

    @Test
    void aFailureIsReportedWithRetriesInsteadOfThrown() {
        waiting(null);
        worker.failure = new RuntimeException("ingest failed for /ABS/PATH/submissions.jsonl");

        worker.poll();

        verify(cibSevenRestClient).handleTaskFailure(eq("task"), eq("w"),
                contains("/ABS/PATH"), anyString(), eq(3), eq(60000L));
        verify(cibSevenRestClient, never()).completeExternalTask(anyString(), anyMap());
    }

    @Test
    void theLastRetryRaisesTheIncident() {
        waiting("1");
        worker.failure = new RuntimeException("still missing");

        worker.poll();

        verify(cibSevenRestClient).handleTaskFailure(eq("task"), eq("w"), anyString(), anyString(), eq(0), anyLong());
    }

    @Test
    void successCompletesTheTask() {
        waiting(null);

        worker.poll();

        verify(cibSevenRestClient).completeExternalTask(eq("task"), anyMap());
        verify(cibSevenRestClient, never()).handleTaskFailure(anyString(), anyString(), anyString(), anyString(), any(Integer.class), anyLong());
    }

    @Test
    void nothingWaitingDoesNothing() {
        worker.poll();

        verify(cibSevenRestClient, never()).completeExternalTask(anyString(), anyMap());
    }
}
