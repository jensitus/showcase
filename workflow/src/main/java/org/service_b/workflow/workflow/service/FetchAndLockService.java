package org.service_b.workflow.workflow.service;

import org.service_b.workflow.workflow.dto.CompleteExternalTask;
import org.service_b.workflow.workflow.dto.FetchAndLock;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FetchAndLockService {

    private final RestClientService restClientService;
    private static final String FETCH_AND_LOCK_URL = "/external-task/fetchAndLock";

    public FetchAndLockResponse fetchAndLockExternalTask(FetchAndLock fetchAndLock) {
        FetchAndLockResponse fetchAndLockResponse = restClientService.fetchAndLock(fetchAndLock);
        log.info(fetchAndLockResponse.toString());
        return fetchAndLockResponse;
    }

    public void completeExternalTask(String taskId, CompleteExternalTask completeExternalTask) {
        // restClientService.completeExternalTask(taskId, completeExternalTask);
    }

}
