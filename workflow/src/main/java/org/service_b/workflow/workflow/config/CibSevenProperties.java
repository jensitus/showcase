package org.service_b.workflow.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cibseven")
@Data
public class CibSevenProperties {
    private String baseUrl = "http://localhost:17000/engine-rest";
    private String workerId = "business-logic-worker";
    private int defaultRetries = 3;
    private long defaultRetryTimeout = 60000; // 1 minute
    /** Empty fetches in a row for a topic before the next one long-polls. */
    private int emptyFetchesBeforeLongPoll = 2;
    /** How long the engine holds a long-polling fetch open when no task is waiting. */
    private int longPollMs = 30000;
}
