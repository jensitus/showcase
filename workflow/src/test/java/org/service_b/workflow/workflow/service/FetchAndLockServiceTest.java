package org.service_b.workflow.workflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.service_b.workflow.workflow.config.CibSevenProperties;
import org.service_b.workflow.workflow.dto.FetchAndLock;
import org.service_b.workflow.workflow.dto.FetchAndLockResponse;
import org.service_b.workflow.workflow.dto.Topic;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FetchAndLockServiceTest {

    private RestClientService restClientService;
    private FetchAndLockService service;
    /** asyncResponseTimeout of each fetch sent, in order (the request object is captured as sent). */
    private final List<Integer> sent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        restClientService = mock(RestClientService.class);
        CibSevenProperties props = new CibSevenProperties();
        props.setEmptyFetchesBeforeLongPoll(2);
        props.setLongPollMs(30000);
        service = new FetchAndLockService(restClientService, props);
    }

    private FetchAndLockResponse fetch(String topicName) {
        Topic topic = new Topic();
        topic.setTopicName(topicName);
        FetchAndLock request = new FetchAndLock();
        request.setTopics(new Topic[]{topic});
        FetchAndLockResponse task = service.fetchAndLockExternalTask(request);
        sent.add(request.getAsyncResponseTimeout());
        return task;
    }

    private static FetchAndLockResponse task(String id) {
        FetchAndLockResponse task = new FetchAndLockResponse();
        task.setId(id);
        return task;
    }

    @Test
    void afterTwoEmptyFetchesTheNextOneLongPolls() {
        when(restClientService.fetchAndLock(any())).thenReturn(null);

        assertThat(fetch("t")).isNull();
        fetch("t");
        fetch("t");
        fetch("t");

        assertThat(sent).containsExactly(null, null, 30000, 30000);
    }

    @Test
    void aFetchedTaskResetsToImmediateFetches() {
        when(restClientService.fetchAndLock(any())).thenReturn(null, null, task("a"), null);

        fetch("t");
        fetch("t");
        assertThat(fetch("t").getId()).isEqualTo("a");   // found while long-polling
        fetch("t");                                       // more may be waiting: ask at once

        assertThat(sent).containsExactly(null, null, 30000, null);
    }

    @Test
    void topicsAreCountedSeparately() {
        when(restClientService.fetchAndLock(any())).thenReturn(null);

        fetch("quiet");
        fetch("quiet");
        fetch("other");

        assertThat(sent).containsExactly(null, null, null);
    }

    @Test
    void anUnreachableEngineIsNoTaskAndNoException() {
        when(restClientService.fetchAndLock(any())).thenThrow(new ResourceAccessException("Connection refused"));

        assertThat(fetch("t")).isNull();
        assertThat(fetch("t")).isNull();
        assertThat(fetch("t")).isNull();

        // a failed fetch is not an empty one: it does not switch to long polling
        assertThat(sent).containsOnlyNulls();
        verify(restClientService, times(3)).fetchAndLock(any());
    }
}
